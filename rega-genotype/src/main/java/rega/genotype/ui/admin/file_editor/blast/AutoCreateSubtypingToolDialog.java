package rega.genotype.ui.admin.file_editor.blast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;

import rega.genotype.AlignmentException;
import rega.genotype.ApplicationException;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.tools.FastaToRega;
import rega.genotype.tools.FastaToRega.PhyloAlignment;
import rega.genotype.ui.framework.widgets.Template;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WButtonGroup;
import eu.webtoolkit.jwt.WContainerWidget.Overflow;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WFileUpload;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WRadioButton;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.servlet.UploadedFile;

public class AutoCreateSubtypingToolDialog extends WDialog {
	private Signal created = new Signal();
	private WButtonGroup alignmentTypeBG;
	private WRadioButton oneAlignment;
	private WRadioButton manyAlignment;
	private Template t = new Template(tr("admin.auto-subtyping-widget"), getContents());


	public AutoCreateSubtypingToolDialog(final File workDir, final String scientificName) {
		super("Auto create virus tool");
		show();
		getContents().setMaximumSize(new WLength(600), new WLength(480));
		getContents().setOverflow(Overflow.OverflowAuto);
		final WPushButton close = new WPushButton("Close", getFooter());
		close.clicked().addListener(close, new Signal.Listener() {
			public void trigger() {
				reject();
			}
		});
		
		final WLineEdit taxonomyLE = new WLineEdit();
		
		taxonomyLE.setText(scientificName == null ? tr("empty").toString() : scientificName);

		alignmentTypeBG = new WButtonGroup();
		oneAlignment = new WRadioButton("Upload 1 alignment fasta file of full genoms.");
		manyAlignment = new WRadioButton("Upload many alignment fasta files and 1 blast.fasta files");
		alignmentTypeBG.addButton(oneAlignment);
		alignmentTypeBG.addButton(manyAlignment);
		oneAlignment.setInline(false);
		manyAlignment.setInline(false);
		alignmentTypeBG.setCheckedButton(oneAlignment);
		alignmentTypeChanged();

		WAnchor advanced = new WAnchor();
		advanced.setText("Advanced");
		advanced.addStyleClass("link");

		final WLineEdit formatLE = new WLineEdit("(\\d++)([^_]++)_(.*)");
		formatLE.setInline(true);
		formatLE.setWidth(new WLength(300));

		final WFileUpload upload = new WFileUpload();
		upload.setInline(false);
		upload.setFilters(".fasta");
		upload.setMultiple(true);

		final WPushButton createB = new WPushButton("Create");
		final WText info = new WText();
		info.addStyleClass("auto-form-info");
		info.setInline(false);
		createB.clicked().addListener(createB, new Signal.Listener() {
			public void trigger() {
				upload.upload();
			}
		});
		createB.setMargin(10);

		t.bindWidget("scientific-name", taxonomyLE);
		t.bindWidget("upload", upload);
		t.bindWidget("create", createB);
		t.bindWidget("info", info);
		t.bindWidget("advance-format", advanced);
		t.bindWidget("one-alignment", oneAlignment);
		t.bindWidget("many-alignments", manyAlignment);

		alignmentTypeBG.checkedChanged().addListener(manyAlignment, new Signal1.Listener<WRadioButton>() {
			public void trigger(WRadioButton arg) {
				alignmentTypeChanged();
			}
		});
		
		advanced.clicked().addListener(advanced, new Signal.Listener() {
			public void trigger() {
				Template advancedT = new Template(tr("admin.auto-subtyping-widget.advanced"));
				advancedT.bindWidget("format", formatLE);
				t.bindWidget("advance-format", advancedT);
			}
		});

		upload.uploaded().addListener(upload, new Signal.Listener() {
			public void trigger() {
				if (upload.getUploadedFiles().size() == 0) 
					info.setText("Upload file first.");

				String regix = formatLE.getText().replace("\"", "\\");
				try {
					if (alignmentTypeBG.getCheckedButton().equals(oneAlignment)) {
						File fastaAlingmentFile = new File(workDir, upload.getClientFileName());
						fastaAlingmentFile.delete();
						FileUtils.copyFile(new File(upload.getSpoolFileName()),
								fastaAlingmentFile);

						FastaToRega.createTool(taxonomyLE.getText(),
								fastaAlingmentFile, workDir, regix);
					} else {
						// clean old TODO

						// upload
						File blast = null;
						List<File> phyloFiles = new ArrayList<File>();
						for (UploadedFile f: upload.getUploadedFiles()) {
							if (f.getClientFileName().equals("blast.fasta")) {
								blast = new File(workDir, "blast.fasta");
								FileUtils.copyFile(new File(f.getSpoolFileName()), blast);
							} else if (f.getClientFileName().startsWith("phylo-")
									&& f.getClientFileName().endsWith(".fasta")) {
								File phylo = new File(workDir, f.getClientFileName());
								FileUtils.copyFile(new File(f.getSpoolFileName()), phylo);
								phyloFiles.add(phylo);
							}
						}

						if (phyloFiles.isEmpty()) {
							info.setText("Error: phylo-'cluster name'.fasta files must be provided.");
							return;
						}

						List<PhyloAlignment> phyloAlignments = new ArrayList<FastaToRega.PhyloAlignment>();
						for(File f: phyloFiles)
							phyloAlignments.add(new PhyloAlignment(f, regix));

						if (blast == null)
							FastaToRega.createTool(phyloAlignments, workDir);
						else
							FastaToRega.createTool(blast, phyloAlignments, workDir);
					}

					info.setText("Tool created.");
					created.trigger();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					info.setText("Error: " + e.getMessage());
				} catch (ParameterProblemException e) {
					e.printStackTrace();
					info.setText("Error: " + e.getMessage());
				} catch (IOException e) {
					e.printStackTrace();
					info.setText("Error: " + e.getMessage());
				} catch (FileFormatException e) {
					e.printStackTrace();
					info.setText("Error: " + e.getMessage());
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
					info.setText("Error: " + e.getMessage());
				} catch (TransformerException e) {
					e.printStackTrace();
					info.setText("Error: " + e.getMessage());
				} catch (ApplicationException e) {
					e.printStackTrace();
					info.setText("Error: " + e.getMessage());
				} catch (InterruptedException e) {
					e.printStackTrace();
					info.setText("Error: " + e.getMessage());
				} catch (AlignmentException e) {
					e.printStackTrace();
					info.setText("Error: " + e.getMessage());
				}
			}
		});	
	}

	private void alignmentTypeChanged() {
		if (alignmentTypeBG.getCheckedButton().equals(oneAlignment)) {
			t.bindString("text", tr("admin.auto-subtyping-widget.one-alignemnt"));
		} else {
			t.bindString("text", tr("admin.auto-subtyping-widget.many-alignment"));
		}
	}

	public Signal created() {
		return created;
	}
}
