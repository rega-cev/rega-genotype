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

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.Sequence;
import rega.genotype.SequenceAlignment;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.utils.Settings;
import rega.genotype.utils.Utils;
import eu.webtoolkit.jwt.Icon;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.StandardButton;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WInteractWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WMessageBox;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTemplate;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTextArea;

/**
 * StartForm implementation implements a widget which allows the user to submit
 * sequences for a new job or get the results of an existing job using the job Id.
 */
public class StartForm extends AbstractForm {
	private WTextArea sequenceTA;
	private FileUpload fileUpload;
	
	private WLineEdit jobIdTF;
	
	private WText errorJobId, errorText;
	private String fileUploadFasta;
	private String msgUploadFile = "Successfully uploaded file! Click Start to process the file.";
	
	public StartForm(GenotypeWindow main) {
		super(main);
		
		WTemplate t = new WTemplate(tr("start-form"), this);
		t.addFunction("tr", WTemplate.Functions.tr);
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
		            messageBox.buttonClicked().addListener(StartForm.this,
		                    new Signal1.Listener<StandardButton>() {
		                        public void trigger(StandardButton sb) {
		                            if (messageBox.getButtonResult() == StandardButton.Yes) {
		                            	startJob(fasta);
		                            }
		                            if (messageBox != null)
		                                messageBox.remove();
		                        }
		                    });
		            messageBox.show();
				} else {
					if (error == null)
						startJob(fasta);
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
	
	private void startJob(final String fastaContent) {
		final File thisJobDir = GenotypeLib.createJobDir(getMain().getOrganismDefinition().getOrganismName());

		Thread analysis = new Thread(new Runnable(){
			public void run() {
				try {
					File seqFile = new File(thisJobDir.getAbsolutePath()+File.separatorChar+"sequences.fasta");
					Utils.writeStringToFile(seqFile, fastaContent);
					getMain().getOrganismDefinition().startAnalysis(thisJobDir);
					File done = new File(thisJobDir.getAbsolutePath()+File.separatorChar+"DONE");
					Utils.writeStringToFile(done, System.currentTimeMillis()+"");
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
		
		getMain().changeInternalPath(JobForm.JOB_URL + "/" + AbstractJobOverview.jobId(thisJobDir) + "/");
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
	
	private void setFastaTextArea(String fileUploadFasta){
		this.fileUploadFasta = fileUploadFasta;
	}
	
	private String getFastaTextArea(){
		return this.fileUploadFasta;
	}
	
	public void resize(WLength width, WLength height) {
		super.resize(width, height);
		this.sequenceTA.resize(new WLength(100, WLength.Unit.Percentage), WLength.Auto);
	}

	@Override
	public void handleInternalPath(String internalPath) {
		
	}
}
