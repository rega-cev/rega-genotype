package rega.genotype.ui.admin.file_editor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.SequenceAlignment;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.ui.admin.file_editor.xml.BlastXmlWriter;
import rega.genotype.ui.framework.exeptions.RegaGenotypeExeption;
import rega.genotype.ui.framework.widgets.MsgDialog;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.SelectionBehavior;
import eu.webtoolkit.jwt.SelectionMode;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WDialog.DialogCode;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WPanel;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WStackedWidget;
import eu.webtoolkit.jwt.WTableView;

/**
 * Editor for blast.xml and blast.fasta.
 * 
 * @author michael
 */
public class BlastFileEditor extends WContainerWidget{
	private File toolDir;
	private BlastAnalysisForm analysis;
	private WStackedWidget stack = new WStackedWidget(this);
	private Template layout = new Template(tr("admin.config.blast-file-editor"));
	private AlignmentAnalyses alignmentAnalyses;
	private ClusterTableModel clusterTableModel;

	public BlastFileEditor(final File toolDir) {
		this.toolDir = toolDir;

		WPushButton saveB = new WPushButton("Save");
		WPushButton cancelB = new WPushButton("Cancel");
		WPushButton addSequencesB = new WPushButton("Add sequences");

		alignmentAnalyses = readBlastXml();

		if (alignmentAnalyses == null){
			alignmentAnalyses = new AlignmentAnalyses();
			alignmentAnalyses.setAlignment(new SequenceAlignment());
			alignmentAnalyses.putAnalysis("blast",
					new BlastAnalysis(alignmentAnalyses,
							"", new ArrayList<AlignmentAnalyses.Cluster>(),
							0.0, 0.0, 0.0, 0.0, "", "", null));
		}

		WPanel analysisPanel = new WPanel();
		analysisPanel.addStyleClass("admin-panel");
		analysisPanel.setTitle("Analysis");
		analysis = new BlastAnalysisForm(
				(BlastAnalysis) alignmentAnalyses.getAnalysis("blast"));
		analysisPanel.setCentralWidget(analysis);

		createClustersTable(alignmentAnalyses);


		//bind

		stack.addWidget(layout);
		layout.bindWidget("analysis", analysisPanel);
		layout.bindWidget("save", saveB);
		layout.bindWidget("cancel", cancelB);
		layout.bindWidget("add-sequences", addSequencesB);


		saveB.clicked().addListener(saveB, new Signal.Listener() {
			public void trigger() {
				save();
			}
		});

		addSequencesB.clicked().addListener(addSequencesB, new Signal.Listener() {
			public void trigger() {
				final FastaFileEditorDialog d = new FastaFileEditorDialog(
						null, alignmentAnalyses, toolConfig());
				d.finished().addListener(d, new Signal1.Listener<WDialog.DialogCode>() {
					public void trigger(DialogCode arg) {
						if(arg == DialogCode.Accepted){
							Map<AbstractSequence, Cluster> selectedSequences = d.getSelectedSequences();
							for (Map.Entry<AbstractSequence, Cluster> e: selectedSequences.entrySet()){
								AbstractSequence sequence = e.getKey();
								Cluster cluster = e.getValue();
								cluster.addTaxus(sequence.getName());
								alignmentAnalyses.getAlignment().addSequence(sequence);
							}
						}
					}
				});
			}
		});
	}

	public void save() {
		try {
			if(!analysis.save()){
				new MsgDialog("Error", "Analysis is not valid.");
				return;
			}

			new BlastXmlWriter(blastFile(), alignmentAnalyses);
			writeFastaFile();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			new MsgDialog("Error", "Could not save blast.xml");
		} catch (IOException e) {
			e.printStackTrace();
			new MsgDialog("Error", "Could not save blast.fasta");
		} catch (ParameterProblemException e) {
			e.printStackTrace();
			new MsgDialog("Error", "Could not save blast.fasta");
		} catch (RegaGenotypeExeption e) {
			new MsgDialog("Error", e.getMessage());
		}
	}

	private File blastFile() {
		return new File(toolDir.getAbsolutePath(), "blast.xml");
	}

	private File fastaFile() {
		return new File(toolDir.getAbsolutePath(), "blast.fasta");
	}

	private ToolConfig toolConfig() {
		ToolManifest manifest = ToolManifest.parseJson(
				FileUtil.readFile(new File(toolDir, ToolManifest.MANIFEST_FILE_NAME)));
		if (manifest == null)
			return null;

		return Settings.getInstance().getConfig().getToolConfigById(manifest.getId(), manifest.getVersion());
	}
	/**
	 * Read blast.xml file
	 */
	private AlignmentAnalyses readBlastXml(){
		try {
			return new AlignmentAnalyses(blastFile(), null, null);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (ParameterProblemException e) {
			e.printStackTrace();
			return null;
		} catch (FileFormatException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void writeFastaFile() throws ParameterProblemException, FileNotFoundException, IOException {
		alignmentAnalyses.getAlignment().writeOutput(new FileOutputStream(fastaFile()),
				SequenceAlignment.FILETYPE_FASTA);
	}

	private void createClustersTable(AlignmentAnalyses alignmentAnalyses) {
		List<Cluster> clusters = alignmentAnalyses.getAllClusters();
		clusterTableModel = new ClusterTableModel(clusters);
		final WTableView table = new WTableView();
		table.setModel(clusterTableModel);
		table.setSelectionMode(SelectionMode.SingleSelection);
		table.setSelectionBehavior(SelectionBehavior.SelectRows);
		table.setHeight(new WLength(200));

		WPushButton addB = new WPushButton("Add");
		WPushButton editB = new WPushButton("Edit");
		WPushButton removeB = new WPushButton("Remove");

		Template clusterTableTemplate = new Template(tr("admin.config.cluster-table"));
		clusterTableTemplate.bindWidget("add", addB);
		clusterTableTemplate.bindWidget("edit", editB);
		clusterTableTemplate.bindWidget("remove", removeB);
		clusterTableTemplate.bindWidget("cluster-table", table);

		WPanel clusterPanel = new WPanel();
		clusterPanel.addStyleClass("admin-panel");
		clusterPanel.setTitle("Reference clusters");
		clusterPanel.setCentralWidget(clusterTableTemplate);

		layout.bindWidget("cluster-table", clusterPanel);

		addB.clicked().addListener(addB, new Signal.Listener() {
			public void trigger() {
				editClaster(null);
			}
		});

		editB.clicked().addListener(addB, new Signal.Listener() {
			public void trigger() {
				if (table.getSelectedIndexes().size() == 1){
					WModelIndex index = table.getSelectedIndexes().first();
					Cluster cluster = clusterTableModel.getCluster(index.getRow());
					editClaster(cluster);
				}
			}
		});

		table.doubleClicked().addListener(table, new Signal2.Listener<WModelIndex, WMouseEvent>() {
			public void trigger(WModelIndex index, WMouseEvent arg2) {
				if (index != null && stack.getCount() == 1) {
					Cluster cluster = clusterTableModel.getCluster(index.getRow());
					editClaster(cluster);
				}
			}
		});
	}

	private void editClaster(final Cluster cluster){
		final boolean isNew = cluster == null;

		final ClusterForm c = new ClusterForm(cluster, alignmentAnalyses, toolConfig());
		stack.addWidget(c);
		stack.setCurrentWidget(c);

		c.done().addListener(c, new Signal1.Listener<WDialog.DialogCode>() {
			public void trigger(DialogCode arg) {
				if (arg == DialogCode.Accepted) {
					if (isNew)
						alignmentAnalyses.getAllClusters().add(c.getCluster());
					clusterTableModel.refresh();
				}
				stack.removeWidget(c);
				stack.setCurrentWidget(layout);
			}
		});
	}
}
