/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import rega.genotype.AlignmentAnalyses;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.Sequence;
import rega.genotype.SequenceAlignment;
import rega.genotype.ngs.NgsAnalysis;
import rega.genotype.ngs.NgsFileSystem;
import rega.genotype.ngs.NgsProgress;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.framework.widgets.StandardDialog;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Utils;
import eu.webtoolkit.jwt.Icon;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.StandardButton;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WFileUpload;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WInteractWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WMessageBox;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WProgressBar;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTextArea;

/**
 * StartForm implementation implements a widget which allows the user to submit
 * sequences for a new job or get the results of an existing job using the job Id.
 */
public class StartForm extends AbstractForm {
	private WTextArea sequenceTA;
	private FileUpload fileUpload;
	private String fileUploadFasta;
	private WFileUpload fastqFileUpload1;
	private WFileUpload fastqFileUpload2;
	private WPushButton fastqStart;
	
	private WLineEdit jobIdTF;
	
	private WText errorJobId, errorText;
	private String msgUploadFile = "Successfully uploaded file! Click Start to process the file.";
	
	public StartForm(GenotypeWindow main) {
		super(main);

		Template t = new Template(tr("start-form"), this);
		t.setInternalPathEncoding(true);
		
		t.bindInt("maxAllowedSeqs", Settings.getInstance().getMaxAllowedSeqs());
		t.bindString("app.base.url", GenotypeMain.getApp().getEnvironment().getDeploymentPath());
		t.bindString("app.context", GenotypeMain.getApp().getServletContext().getContextPath());
		
		errorText = new WText();
		errorText.setStyleClass("error-text");
		t.bindWidget("error-text", errorText);
		
		sequenceTA = new WTextArea();
		t.bindWidget("fasta-field", sequenceTA);
		sequenceTA.setObjectName("seq-input-fasta");
		sequenceTA.setRows(15);
		sequenceTA.setColumns(83);
		sequenceTA.setStyleClass("fasta-ta");
		sequenceTA.setText(tr("sequenceInput.example").toString());

		Utils.removeSpellCheck(sequenceTA);

		WInteractWidget run = createButton("sequenceInput.run","sequenceInput.run.icon");
		t.bindWidget("analyze-button", run);
		run.setObjectName("button-run");
	
		WInteractWidget clear = createButton("sequenceInput.clear","sequenceInput.clear.icon");
		t.bindWidget("clear-button", clear);
		clear.setObjectName("button-clear");
		clear.clicked().addListener(this, new Signal1.Listener<WMouseEvent>() {
			public void trigger(WMouseEvent a) {
				sequenceTA.setText("");
			}
		});
	
		run.clicked().addListener(this, new Signal1.Listener<WMouseEvent>() {
			public void trigger(WMouseEvent a) {
				final String fasta;
				if (((sequenceTA.getText().equalsIgnoreCase("")) || (sequenceTA.getText().equalsIgnoreCase(msgUploadFile))) && (!(getFastaTextArea().equalsIgnoreCase("")))){
					fasta = getFastaTextArea();
				}else{
					fasta = sequenceTA.getText();
				}				
				
				CharSequence error = verifyFasta(fasta);
				validateInput(error);
				
				final Boolean submit = true;
				if (capSequences(fasta)) {
					final WMessageBox messageBox = new WMessageBox(
		                    tr("sequenceInput.capWarning.title").toString(),
		                    tr("sequenceInput.capWarning.msg"),
		                    Icon.Information, 
		                    EnumSet.of(StandardButton.Yes,StandardButton.No));
		            messageBox.setModal(false);
					messageBox.setWidth(new WLength(600));
		            messageBox.buttonClicked().addListener(StartForm.this,
		                    new Signal1.Listener<StandardButton>() {
		                        public void trigger(StandardButton sb) {
		                            if (messageBox.getButtonResult() == StandardButton.Yes) {
		                            	startLocalJob(fasta);
		                            }
		                            if (messageBox != null)
		                                messageBox.remove();
		                        }
		                    });
		            messageBox.show();
				} else {
					if (error == null)
						startLocalJob(fasta);
				}
			}

			
		});

		fileUpload = new FileUpload();
		t.bindWidget("file-upload-button", fileUpload);
		
		fileUpload.uploadedFile().addListener(this, new Signal1.Listener<File>() {
            public void trigger(File f) {                
				try {
					if (f.exists()) {
						String fasta = GenotypeLib.readFileToString(f);
						sequenceTA.setText(msgUploadFile);
						setFastaTextArea(fasta);
						
						CharSequence error = verifyFasta(fasta);
						validateInput(error);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        });

		jobIdTF = new WLineEdit();
		t.bindWidget("job-id-field", jobIdTF);
		WInteractWidget monitorButton = createButton("startForm.monitor","startForm.monitor.icon");
		t.bindWidget("search-button", monitorButton);
		monitorButton.clicked().addListener(this, new Signal1.Listener<WMouseEvent>() {
			public void trigger(WMouseEvent a) {
				getMain().changeInternalPath(JobForm.JOB_URL+"/"+jobIdTF.getText());
			}
		});
		
		errorJobId = new WText(tr("startForm.errorJobId"));
		errorJobId.setId("jobid-error-message");
		errorJobId.setStyleClass("error");
		errorJobId.hide();
		
		t.bindWidget("error-job", errorJobId);

		// blast.xml place holders
		AlignmentAnalyses alignmentAnalyses = readBlastXml();
		if(alignmentAnalyses == null) {
			t.bindEmpty("count_virus_from_blast.xml");
			t.bindEmpty("count_typing_tools");
		} else {
			List<Cluster> allClusters = alignmentAnalyses.getAllClusters();
			Set<String> tools = new HashSet<String>();
			for (Cluster c:allClusters) {
				if(c.getTaxonomyId() != null && !c.getTaxonomyId().isEmpty())
					tools.add(c.getTaxonomyId());
			}

			t.bindString("count_virus_from_blast.xml", allClusters.size() + "");
			t.bindString("count_typing_tools", tools.size() + "");
		}

		// NGS

		initNgs(t);
	}

	private String setFastqExtention(String fileName) {
		return FilenameUtils.getBaseName(fileName) + ".fastq";
	}

	private void initNgs(final Template t) {
		final Template ngsTemplate = new Template(tr("startForm.ngs-widget"));
		t.bindWidget("start-form.ngs-widget", ngsTemplate);

		fastqFileUpload1 = new WFileUpload();
		fastqFileUpload1.setProgressBar(new WProgressBar());
		fastqFileUpload1.setFilters(".fastq,.zip,.gz");

		fastqFileUpload2 = new WFileUpload();
		fastqFileUpload2.setProgressBar(new WProgressBar());
		fastqFileUpload2.setFilters(".fastq,.zip,.gz");

		fastqStart = new WPushButton("Start NGS analysis");

		fastqFileUpload1.fileTooLarge().addListener(fastqFileUpload1, new Signal1.Listener<Long>() {
			public void trigger(Long arg) {
				long maxRequestSize = WApplication.getInstance().getEnvironment().getServer().getConfiguration().getMaxRequestSize() / (1000*1000*1000);
				ngsTemplate.bindString("fastq-upload1-info", "File too large. Max file size = " + maxRequestSize + " GB");
			}
		});

		fastqFileUpload2.fileTooLarge().addListener(fastqFileUpload2, new Signal1.Listener<Long>() {
			public void trigger(Long arg) {
				long maxRequestSize = WApplication.getInstance().getEnvironment().getServer().getConfiguration().getMaxRequestSize() / (1000*1000*1000);
				ngsTemplate.bindString("fastq-upload2-info", "File too large. Max file size = " + maxRequestSize + " GB");
			}
		});

		fastqStart.clicked().addListener(fastqStart, new Signal.Listener() {
			public void trigger() {
				fastqFileUpload1.upload();
			}
		});

		fastqFileUpload1.uploaded().addListener(this, new Signal.Listener() {
			public void trigger() {
				fastqFileUpload2.upload();
			}
		});

		fastqFileUpload2.uploaded().addListener(this, new Signal.Listener() {
			public void trigger() {
				//Start NGS analysis and send assembled sequences to normal analysis.
				File fastqFile1 = new File(fastqFileUpload1.getSpoolFileName());
				File fastqFile2 = new File(fastqFileUpload2.getSpoolFileName());

				final File workDir = GenotypeLib.createJobDir(getMain().getOrganismDefinition().getJobDir());

				try {
					File fastqDir = new File(workDir, NgsFileSystem.FASTQ_FILES_DIR);
					fastqDir.mkdirs();

					new File(fastqDir, fastqFileUpload1.getClientFileName());

					File extructedFastqPE1 = new File(fastqDir, fastqFileUpload1.getClientFileName());
					File extructedFastqPE2 = new File(fastqDir, fastqFileUpload2.getClientFileName());

					if (!FilenameUtils.getExtension(fastqFile1.getName()).equals(
							FilenameUtils.getExtension(fastqFile2.getName()))) {
						StandardDialog d = new StandardDialog("Error");
						d.getContents().addWidget(new WText("Error: the files are not of the same typr?"));
						return;
					} else if (FilenameUtils.getExtension(fastqFileUpload1.getClientFileName()).equals("gz")) {
						extructedFastqPE1 = new File(fastqDir, setFastqExtention(fastqFileUpload1.getClientFileName()));
						extructedFastqPE2 = new File(fastqDir, setFastqExtention(fastqFileUpload2.getClientFileName()));
						FileUtil.unGzip1File(fastqFile1, extructedFastqPE1);
						FileUtil.unGzip1File(fastqFile2, extructedFastqPE2);
					} else if (FilenameUtils.getExtension(fastqFileUpload1.getClientFileName()).equals("zip")){ // compressed .zip
						extructedFastqPE1 = new File(fastqDir, setFastqExtention(fastqFileUpload1.getClientFileName()));
						extructedFastqPE2 = new File(fastqDir, setFastqExtention(fastqFileUpload2.getClientFileName()));
						FileUtil.unzip1File(fastqFile1, extructedFastqPE1);
						FileUtil.unzip1File(fastqFile2, extructedFastqPE2);
					} else { // not compressed
						FileUtils.copyFile(fastqFile1, extructedFastqPE1);
						FileUtils.copyFile(fastqFile2, extructedFastqPE2);
					}

					NgsProgress ngsProgress = new NgsProgress();

					//PE
					ngsProgress.setFastqPE1FileName(extructedFastqPE1.getName());
					ngsProgress.setFastqPE2FileName(extructedFastqPE2.getName());

					// SE TODO
					ngsProgress.save(workDir);

				} catch (IOException e) {
					e.printStackTrace();
				}

				Thread ngsAnalysis = new Thread(new Runnable() {
					public void run() {
						try {
							NgsAnalysis ngsAnalysis = new NgsAnalysis(workDir);
							if (ngsAnalysis.analyze()) {
								// analyze assembled fasta files 
								getMain().getOrganismDefinition().startAnalysis(workDir);
								File done = new File(workDir.getAbsolutePath()+File.separatorChar+"DONE");
								FileUtil.writeStringToFile(done, System.currentTimeMillis()+"");
							}
						} catch (IOException e) {
							e.printStackTrace();
							assert(false);
						} catch (ParameterProblemException e) {
							e.printStackTrace();
						} catch (FileFormatException e) {
							e.printStackTrace();
						}
					}
				});
				ngsAnalysis.start();
				//initNgs(t);
				getMain().changeInternalPath(JobForm.JOB_URL + "/" + AbstractJobOverview.jobId(workDir) + "/");
			}
		});

		ngsTemplate.bindWidget("fastq-start", fastqStart);
		ngsTemplate.bindWidget("fastq-upload1", fastqFileUpload1);
		ngsTemplate.bindWidget("fastq-upload2", fastqFileUpload2);
		ngsTemplate.bindEmpty("fastq-upload1-info");
		ngsTemplate.bindEmpty("fastq-upload2-info");
	}
	
	private WInteractWidget createButton(String textKey, String iconKey) {
		WString text = tr(textKey);
		if (hasKey(iconKey)) {
			if (text.isEmpty()) {
				return new WImage(tr(iconKey).toString());
			}
			else {
				WPushButton b = new WPushButton();
				b.setText(text);
				b.setIcon(new WLink(tr(iconKey).toString()));
				return b;
			}
		} else {
			WPushButton b = new WPushButton();
			b.setText(text);
			return b;
		}
	}
	
	private boolean hasKey(String key) {
		String value = WApplication.getInstance().getLocalizedStrings().resolveKey(key);
		return value != null;
	}
	
	private void validateInput(CharSequence error) {
		if (error == null) {
			sequenceTA.setStyleClass("edit-valid");
			errorText.setHidden(true);
		} else {
			sequenceTA.setStyleClass("edit-invalid");
			errorText.setHidden(false);
			errorText.setText(error);
		}
	}

	private void startLocalJob(final String fastaContent) {
		File jobDir = startJob(fastaContent, getMain().getOrganismDefinition());
		getMain().changeInternalPath(JobForm.JOB_URL + "/" + AbstractJobOverview.jobId(jobDir) + "/");
	}

	/**
	 * Perform analysis on given fasta sequences, in a thread and write the results to a new job dir.
	 * @param fastaContent
	 * @param organismDefinition
	 * @return The job dir
	 */
	public static File startJob(final String fastaContent, final OrganismDefinition organismDefinition) {
		final File thisJobDir = GenotypeLib.createJobDir(organismDefinition.getJobDir());

		Thread analysis = new Thread(new Runnable(){
			public void run() {
				try {
					File seqFile = new File(thisJobDir.getAbsolutePath()+File.separatorChar+"sequences.fasta");
					FileUtil.writeStringToFile(seqFile, fastaContent);
					organismDefinition.startAnalysis(thisJobDir);
					File done = new File(thisJobDir.getAbsolutePath()+File.separatorChar+"DONE");
					FileUtil.writeStringToFile(done, System.currentTimeMillis()+"");
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParameterProblemException e) {
					e.printStackTrace();
				} catch (FileFormatException e) {
					e.printStackTrace();
				}
			}
		});
		analysis.start();
		
		return thisJobDir;
	}

	private boolean capSequences(String fastaContent) {
		LineNumberReader r = new LineNumberReader(new StringReader(fastaContent));

		try {
			while (true) {
				Sequence s = SequenceAlignment.readFastaFileSequence(r, SequenceAlignment.SEQUENCE_DNA);
				if (s == null)
					break;
				
				if (s.isNameCapped()) 
					return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (FileFormatException e) {
			return false;
		}
		
		return false;
	}
	
	private CharSequence verifyFasta(String fastaContent) {
		int sequenceCount = 0;

		LineNumberReader r = new LineNumberReader(new StringReader(fastaContent));

		try {
			while (true) {
				if (SequenceAlignment.readFastaFileSequence(r, SequenceAlignment.SEQUENCE_DNA)
						== null)
					break;
				++sequenceCount;
			}

			if(sequenceCount == 0) {
				return tr("startForm.noSequence");
			} else if (sequenceCount > Settings.getInstance().getMaxAllowedSeqs()) {
				return tr("startForm.tooManySequences");
			}
		} catch (IOException e) {
			e.printStackTrace();
			return tr("startForm.ioError");
		} catch (FileFormatException e) {
			return e.getMessage();
		}
		
		return null;
	}
	
	public void resize(WLength width, WLength height) {
		super.resize(width, height);
		this.sequenceTA.resize(new WLength(100, WLength.Unit.Percentage), WLength.Auto);
	}

	@Override
	public void handleInternalPath(String internalPath) {
		
	}

	private void setFastaTextArea(String fileUploadFasta){
		this.fileUploadFasta = fileUploadFasta;
	}

	private String getFastaTextArea(){
		return this.fileUploadFasta;
	}

	private AlignmentAnalyses readBlastXml(){
		File xmlDir = new File(getMain().getOrganismDefinition().getXmlPath());
		if (AlignmentAnalyses.blastFile(xmlDir).exists()) {
			try {
				return new AlignmentAnalyses(AlignmentAnalyses.blastFile(xmlDir), null, null);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParameterProblemException e) {
				e.printStackTrace();
			} catch (FileFormatException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
