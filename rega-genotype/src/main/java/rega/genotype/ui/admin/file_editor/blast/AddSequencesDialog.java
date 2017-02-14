package rega.genotype.ui.admin.file_editor.blast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.BlastAnalysis;
import rega.genotype.BlastAnalysis.Result;
import rega.genotype.Constants;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.SequenceAlignment;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.ui.framework.widgets.ObjectListComboBox;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Utils;
import sun.nio.cs.StandardCharsets;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WCheckBox;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WContainerWidget.Overflow;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTextArea;

/**
 * Editor for blast.fasta file
 * 
 * @author michael
 */
public class AddSequencesDialog extends WDialog{
	private TaxusTable analysFastaFileWidget;
	public AddSequencesDialog(final Cluster cluster, final AlignmentAnalyses alignmentAnalyses,
			final ToolConfig toolConfig) {
		show();
		getTitleBar().addWidget(new WText("Add sequences"));
		setHeight(new WLength(420));
		setResizable(true);
		getContents().setOverflow(Overflow.OverflowAuto);

		final WPushButton nextB = new WPushButton("Next", getFooter());
		final WPushButton okB = new WPushButton("OK", getFooter());
		final WPushButton cancelB = new WPushButton("Cancel", getFooter());

		final FastaFileUpload fastaFileUpload = new FastaFileUpload();
		getContents().addWidget(fastaFileUpload);

		okB.hide();

		nextB.clicked().addListener(nextB, new Signal.Listener() {
			public void trigger() {
				analysFastaFileWidget = new TaxusTable(
						cluster, fastaFileUpload.getText(), alignmentAnalyses, toolConfig);
				WContainerWidget c = new WContainerWidget();
				c.addWidget(analysFastaFileWidget);
				getContents().removeWidget(fastaFileUpload);
				getContents().addWidget(analysFastaFileWidget);
				nextB.hide();
				okB.show();
			}
		});

		okB.clicked().addListener(okB, new Signal.Listener() {
			public void trigger() {
				accept();
			}
		});
		cancelB.clicked().addListener(cancelB, new Signal.Listener() {
			public void trigger() {
				reject();
			}
		});
	}

	public Map<AbstractSequence, Cluster> getSelectedSequences() {
		if (analysFastaFileWidget == null)
			return new HashMap<AbstractSequence, Cluster>();
		else
			return analysFastaFileWidget.getSelectedSequences();
	}

	// classes

	// step 1: choose fasta file 

	public static class FastaFileUpload extends Template {
		final WTextArea fastaTA = new WTextArea();
		final FileUpload upload = new FileUpload();
		public FastaFileUpload() {
			super(tr("admin.fasta-file-upload"));
			fastaTA.setInline(false);
			fastaTA.setWidth(new WLength(700));
			fastaTA.setHeight(new WLength(300));
			Utils.removeSpellCheck(fastaTA);

			upload.getWFileUpload().setFilters(".fasta");
			upload.setInline(true);
			
			upload.uploadedFile().addListener(upload, new Signal1.Listener<File>() {
				public void trigger(File file) {
					fastaTA.setText(FileUtil.readFile(file));
				}
			});

			bindWidget("upload", upload);
			bindWidget("text-area", fastaTA);
		}

		public String getText(){
			return fastaTA.getText();
		}
	}

	// step 2: choose clusters 

	private static class SequenceData {
		WCheckBox addSequenceChB;
		ObjectListComboBox<Cluster> clusterCB;
		public SequenceData(WCheckBox addSequenceChB, ObjectListComboBox<Cluster> clusterCB) {
			this.addSequenceChB = addSequenceChB;
			this.clusterCB = clusterCB;
		}
	}

	private static class TaxusTable extends WTable { 
		private Map<AbstractSequence, SequenceData> sequenceMap = new HashMap<AbstractSequence, SequenceData>();
		private AlignmentAnalyses alignmentAnalyses; // AlignmentAnalyses of the tool
		private List<String> taxaIds = new ArrayList<String>(); // pre-compute from alignmentAnalyses

		private enum Mode {SingalCluster, AllClusters};
		private Mode mode = Mode.SingalCluster;
		private Cluster cluster;
		
		public TaxusTable(Cluster cluster, String fasta,
				AlignmentAnalyses alignmentAnalyses, ToolConfig toolConfig) {
			this.cluster = cluster;
			this.alignmentAnalyses = alignmentAnalyses;

			// pre-compute taxaIds
			for (Cluster c: alignmentAnalyses.getAllClusters()){
				taxaIds.addAll(c.getTaxaIds());
			}

			// headers 
			setHeaderCount(1);
			getElementAt(0, 0).addWidget(new WText("Add"));
			getElementAt(0, 1).addWidget(new WText("Name"));
			getElementAt(0, 2).addWidget(new WText("Blast analysis"));
			getElementAt(0, 3).addWidget(new WText("Cluster"));
			getElementAt(0, 4).addWidget(new WText("Info"));

			addStyleClass("fasta-analysis-table");
			setMaximumSize(new WLength(300), WLength.Auto);

			alignmentAnalyses.analyses();

			mode = Mode.AllClusters;
			File blastXmlFile = new File(toolConfig.getConfiguration(), Constants.BLAST_XML_FILE_NAME);
			if (!blastXmlFile.exists()) {
				initSimpleTable(fasta, alignmentAnalyses);
			} else {
				// run the tool to identify the clusters.
		    	BlastAnalysis blastAnalysis = (BlastAnalysis) alignmentAnalyses.getAnalysis("blast");
		    	List<Result> analysisResults = blastAnalysis.analyze(alignmentAnalyses, fasta);
		    	if (analysisResults == null)
		    		initSimpleTable(fasta, alignmentAnalyses);
		    	else
		    		for (Result result: analysisResults)
		    			addRow(result.getSequence(), result.getConcludedCluster());
			}
		}

		/**
		 * Could not perform blast analysis -> blast column is empty.
		 */
		private void initSimpleTable(String fasta, AlignmentAnalyses alignmentAnalyses) {
			SequenceAlignment alignment = parseFasta(fasta, alignmentAnalyses.getAlignment().getSequenceType());
			if (alignment != null) {
				List<AbstractSequence> sequences = alignment.getSequences();
				for (AbstractSequence s: sequences) {
					addRow(s, null);
				}
			}
		}

		private SequenceAlignment parseFasta(String fasta, int sequenceType) {
			try {
				InputStream stream = new ByteArrayInputStream(fasta.getBytes());
				SequenceAlignment alignment = new SequenceAlignment(stream,
						SequenceAlignment.FILETYPE_FASTA, sequenceType);
				return alignment;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (ParameterProblemException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (FileFormatException e) {
				e.printStackTrace();
			}

			return null;
		}

		private void addRow(AbstractSequence s, Cluster blastResultCluster) {
			int row = getRowCount();
			final WCheckBox chb = new WCheckBox();
			final WText blastAnalysisT = new WText("");

			if (blastResultCluster != null)
				blastAnalysisT.setText(blastResultCluster.getName());

			// clusterCB
			List<Cluster> clusters = new ArrayList<AlignmentAnalyses.Cluster>(
					alignmentAnalyses.getAllClusters());
			clusters.add(0, null);
			if (cluster != null) { // new cluster
				clusters.remove(cluster); // place current cluster on top
				clusters.add(1, cluster);
			}

			final ObjectListComboBox<Cluster> clusterCB = new ObjectListComboBox<AlignmentAnalyses.Cluster>(
					clusters) {
				@Override
				protected WString render(Cluster c) {
					if (c == null)
						return new WString("(Empty)");
					else if (cluster != null && c.equals(cluster))
						return new WString("(Currently edited cluster)");
					else
						return new WString(c.getName());
				}
			};

			if (taxaIds != null && taxaIds.contains(s.getName())){
				chb.setUnChecked();
				chb.disable();
				getElementAt(row, 4).addWidget(new WText("Already exists"));
				clusterCB.disable();
			} else 
				chb.setChecked();
			if(mode == Mode.SingalCluster)
				clusterCB.disable();

			if (cluster != null) // Editing a cluster
				clusterCB.setCurrentObject(cluster);
			else // Add sequences file for all clusters.
				clusterCB.setCurrentObject(blastResultCluster);

			clusterCB.changed().addListener(clusterCB, new Signal.Listener() {
				public void trigger() {
					Cluster currentCluster = clusterCB.getCurrentObject();
					if (currentCluster == null) {
						chb.setUnChecked();
						chb.disable();
					} else {
						chb.enable();
					}
				}
			});
			if (clusterCB.getCurrentObject() == null) {
				chb.setUnChecked();
				chb.disable();
			} 

			// bind

			getElementAt(row, 0).addWidget(chb);
			getElementAt(row, 1).addWidget(new WText(Utils.nullToEmpty(s.getName())));
			getElementAt(row, 2).addWidget(blastAnalysisT);
			getElementAt(row, 3).addWidget(clusterCB);

			sequenceMap.put(s, new SequenceData(chb, clusterCB));
		}

		public Map<AbstractSequence, Cluster> getSelectedSequences() {
			Map<AbstractSequence, Cluster> ans = new HashMap<AbstractSequence, Cluster>();
			for (Map.Entry<AbstractSequence, SequenceData> e: sequenceMap.entrySet()){
				if (e.getValue().addSequenceChB.isChecked())
					ans.put(e.getKey(), e.getValue().clusterCB.getCurrentObject());
			}

			return ans;
		}
	}
}
