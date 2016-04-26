package rega.genotype.ui.admin.file_editor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.SequenceAlignment;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Utils;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WCheckBox;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WContainerWidget.Overflow;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTextArea;

/**
 * Editor for blast.fasta file
 * 
 * @author michael
 */
public class FastaFileEditorDialog extends WDialog{
	private AnalysFastaFileWidget analysFastaFileWidget;
	public FastaFileEditorDialog(final Cluster cluster, final AlignmentAnalyses alignmentAnalyses) {
		// TODO: if not blast tool run blust tool to identify the clusters.
		show();
		getTitleBar().addWidget(new WText("Add sequences"));
		setHeight(new WLength(400));
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
				analysFastaFileWidget = new AnalysFastaFileWidget(
						cluster, fastaFileUpload.getText(), alignmentAnalyses);
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

	public List<AbstractSequence> getSelectedSequences() {
		if (analysFastaFileWidget == null)
			return new ArrayList<AbstractSequence>();
		else
			return analysFastaFileWidget.getSelectedSequences();
	}

	// classes

	// step 1: choose fasta file 

	public static class FastaFileUpload extends WContainerWidget {
		final WTextArea fastaTA = new WTextArea(this);
		final FileUpload upload = new FileUpload();
		public FastaFileUpload() {
			fastaTA.setInline(false);
			fastaTA.setWidth(new WLength(700));
			fastaTA.setHeight(new WLength(300));

			upload.getWFileUpload().setFilters(".fasta");
			addWidget(upload);
			upload.uploadedFile().addListener(upload, new Signal1.Listener<File>() {
				public void trigger(File file) {
					fastaTA.setText(FileUtil.readFile(file));
				}
			});
		}

		public String getText(){
			return fastaTA.getText();
		}
	}

	// step 2: choose clusters 

	private static class AnalysFastaFileWidget extends WTable { 
		private Map<AbstractSequence, WCheckBox> sequenceMap = new HashMap<AbstractSequence, WCheckBox>();
		
		public AnalysFastaFileWidget(Cluster cluster, String fasta, AlignmentAnalyses alignmentAnalyses) {
			setHeaderCount(1);
			getElementAt(0, 0).addWidget(new WText("Add"));
			getElementAt(0, 1).addWidget(new WText("Name"));
			getElementAt(0, 2).addWidget(new WText("Tool"));
			getElementAt(0, 3).addWidget(new WText("Cluster"));
			getElementAt(0, 4).addWidget(new WText("Info"));

			addStyleClass("fasta-analysis-table");
			setHeight(new WLength(300));

			fillTaxuesTable(cluster, fasta, alignmentAnalyses.getAlignment().getSequenceType());
		}

		public void fillTaxuesTable(Cluster cluster, String fasta, int sequenceType) {
			SequenceAlignment alignment = parseFasta(fasta, sequenceType);
			if (alignment != null) {
				List<AbstractSequence> sequences = alignment.getSequences();
				List<String> taxaIds = cluster.getTaxaIds();
				for (AbstractSequence s: sequences) {
					int row = getRowCount();
					WCheckBox chb = new WCheckBox();
					if (taxaIds.contains(s.getName())){
						chb.setUnChecked();
						chb.disable();
						getElementAt(row, 4).addWidget(new WText("Already exists"));
					} else 
						chb.setChecked();
						
					getElementAt(row, 0).addWidget(chb);
					getElementAt(row, 1).addWidget(new WText(Utils.nullToEmpty(s.getName())));
					getElementAt(row, 2).addWidget(new WText(Utils.nullToEmpty(cluster.getToolId())));
					getElementAt(row, 3).addWidget(new WText(Utils.nullToEmpty(cluster.getName())));

					sequenceMap.put(s, chb);
				}
			}
		}

		public List<AbstractSequence> getSelectedSequences() {
			List<AbstractSequence> ans = new ArrayList<AbstractSequence>();
			for (Map.Entry<AbstractSequence, WCheckBox> e: sequenceMap.entrySet())
				if (e.getValue().isChecked())
					ans.add(e.getKey());

			return ans;
		}

		private SequenceAlignment parseFasta(String fasta, int sequenceType) {
			try {
				InputStream stream = new ByteArrayInputStream(fasta.getBytes(StandardCharsets.UTF_8));
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
	}
}
