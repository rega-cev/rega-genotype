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

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.SequenceAlignment;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.utils.Settings;
import rega.genotype.utils.Utils;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WTemplate;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTextArea;

/**
 * StartForm implementation implements a widget which allows the user to submit
 * sequences for a new job or get the results of an existing job using the job Id.
 */
public class StartForm extends AbstractForm {
	private WTextArea sequenceTA;
	private WPushButton run, clear;
	private FileUpload fileUpload;
	
	private WLineEdit jobIdTF;
	private WPushButton monitorButton;
	
	private WText errorText;
	
	public StartForm(GenotypeWindow main) {
		super(main);
		
		WTemplate t = new WTemplate(tr("start-form"), this);
		t.addFunction("tr", WTemplate.Functions.tr);
		
		t.bindInt("maxAllowedSeqs", Settings.getInstance().getMaxAllowedSeqs());
		t.bindString("app.base.url", GenotypeMain.getApp().getEnvironment().getDeploymentPath());
		t.bindString("app.context", GenotypeMain.getApp().getServletContext().getContextPath());
		
		errorText = new WText();
		t.bindWidget("error-text", errorText);
		
		sequenceTA = new WTextArea();
		t.bindWidget("fasta-field", sequenceTA);
		sequenceTA.setObjectName("seq-input-fasta");
		sequenceTA.setRows(15);
		sequenceTA.setStyleClass("fasta-ta");

		run = new WPushButton();
		t.bindWidget("analyze-button", run);
		run.setObjectName("button-run");
		run.setText(tr("sequenceInput.run"));
	
		clear = new WPushButton();
		t.bindWidget("clear-button", clear);
		clear.setObjectName("button-clear");
		clear.setText(tr("sequenceInput.clear"));
		clear.clicked().addListener(this, new Signal1.Listener<WMouseEvent>() {
			public void trigger(WMouseEvent a) {
				sequenceTA.setText("");
			}
		});
	
		run.clicked().addListener(this, new Signal1.Listener<WMouseEvent>() {
			public void trigger(WMouseEvent a) {
				String fasta = sequenceTA.getText();
				
				CharSequence error = verifyFasta(fasta);
				validateInput(error);
				if (error == null)
					startJob(fasta);
			}
		});

		fileUpload = new FileUpload();
		t.bindWidget("file-upload-button", fileUpload);
		
		fileUpload.uploadedFile().addListener(this, new Signal1.Listener<File>() {
            public void trigger(File f) {                
				try {
					if (f.exists()) {
						String fasta = GenotypeLib.readFileToString(f);
						sequenceTA.setText(fasta);
						CharSequence error = verifyFasta(fasta);
						validateInput(error);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        });

		jobIdTF = new WLineEdit();
		jobIdTF.setWidth(new WLength(150));
		t.bindWidget("job-id-field", jobIdTF);
		monitorButton = new WPushButton(tr("startForm.monitor"));
		t.bindWidget("search-button", monitorButton);
		monitorButton.clicked().addListener(this, new Signal1.Listener<WMouseEvent>() {
			public void trigger(WMouseEvent a) {
				getMain().changeInternalPath(JobForm.JOB_URL+"/"+jobIdTF.getText());
			}
		});
		
		init();
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
		final File thisJobDir = GenotypeLib.createJobDir(getMain().getOrganismDefinition());

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

	@SuppressWarnings("unchecked")
	private void init() {
		//TODO set example sequence
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
			} else if (sequenceCount <= Settings.getInstance().getMaxAllowedSeqs()) {
				return null;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return tr("startForm.ioError");
		} catch (FileFormatException e) {
			return e.getMessage();
		}
		
		return null;
	}

	@Override
	public void handleInternalPath(String internalPath) {
		
	}
}
