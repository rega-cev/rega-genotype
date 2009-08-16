package rega.genotype.ui.forms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jdom.Element;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.ui.util.Settings;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WBreak;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WInteractWidget;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTextArea;

public class StartForm extends IForm {
	private WText note;
	private WTextArea ta;
	private WPushButton run, clear;
	private FileUpload fileUpload;
	
	private WText monitorLabel;
	private WText jobIdLabel;
	private WLineEdit jobIdTF;
	private WPushButton monitorButton;

	private WText errorJobId, errorSeq;
	
	public StartForm(GenotypeWindow main) {
		super(main, "start-form");
		
		new WBreak(this);
		
		List<String> noteArgs = new ArrayList<String>();
		noteArgs.add(Settings.getInstance().getMaxAllowedSeqs()+"");
		note = new WText(getMain().getResourceManager().getOrganismValue("start-form", "note", noteArgs), this);
		note.setStyleClass("note");
		
		WContainerWidget seqinput = new WContainerWidget(this);
		seqinput.setStyleClass("seqInput");
		
		new WText(tr("sequenceInput.inputSequenceInFastaFormat"), seqinput);
		new WBreak(seqinput);
		
		ta = new WTextArea(seqinput);
		ta.setColumns(83);
		ta.setRows(15);
		new WBreak(seqinput);
	
		fileUpload = new FileUpload();
		seqinput.addWidget(fileUpload);
		fileUpload.getUploadFile().uploaded.addListener(this, new Signal.Listener() {
            public void trigger() {                
				try {
					String fasta = FileUtils.readFileToString(new File(fileUpload.getUploadFile().spoolFileName()));
					verifyFasta(fasta);
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        });
		
		run = new WPushButton(seqinput);
		run.setText(tr("sequenceInput.run"));
		
		clear = new WPushButton(seqinput);
		clear.setText(tr("sequenceInput.clear"));
		clear.clicked.addListener(this, new Signal1.Listener<WMouseEvent>() {
			public void trigger(WMouseEvent a) {
				ta.setText("");
			}
		});
	
		run.clicked.addListener(this, new Signal1.Listener<WMouseEvent>() {
			public void trigger(WMouseEvent a) {
				verifyFasta(ta.text());
			}
		});
		errorSeq = new WText(tr("startForm.errorSequence"), seqinput);		
		
		new WBreak(this);
		
		WContainerWidget monitorContainer = new WContainerWidget(this);
		monitorContainer.setStyleClass("monitor");
		monitorLabel = new WText(tr("startForm.provideJobId"), monitorContainer);
		new WBreak(monitorContainer);
		jobIdLabel = new WText(tr("startForm.jobId"), monitorContainer);
		jobIdTF = new WLineEdit(monitorContainer);
		monitorButton = new WPushButton(tr("startForm.monitor"), monitorContainer);
		monitorButton.clicked.addListener(this, new Signal1.Listener<WMouseEvent>() {
			public void trigger(WMouseEvent a) {
				File jobDir = getMain().getJobDir(jobIdTF.text());
				if (jobDir.exists()) {
					setValid(jobIdTF, errorJobId);
					getMain().monitorForm(jobDir, true);
				} else {
					setInvalid(jobIdTF, errorJobId);
				}
			}
		});
		errorJobId = new WText(tr("startForm.errorJobId"), monitorContainer);

		errorJobId.setStyleClass("error");
		errorJobId.hide();
		errorSeq.setStyleClass("error");
		errorSeq.hide();		
	}
	
	private void setValid(WInteractWidget w, WText errorMsg){
		w.setStyleClass("edit-valid");
		errorMsg.hide();
	}
	private void setInvalid(WInteractWidget w, WText errorMsg){
		w.setStyleClass("edit-invalid");
		errorMsg.show();
	}
	
	private void startJob(final String fastaContent) {
		final File thisJobDir = GenotypeLib.createJobDir(getMain().getOrganismDefinition());

		Thread analysis = new Thread(new Runnable(){
			public void run() {
				try {
					File seqFile = new File(thisJobDir.getAbsolutePath()+File.separatorChar+"sequences.fasta");
					FileUtils.writeStringToFile(seqFile, fastaContent);
					getMain().getOrganismDefinition().startAnalysis(thisJobDir);
					File done = new File(thisJobDir.getAbsolutePath()+File.separatorChar+"DONE");
					FileUtils.writeStringToFile(done, System.currentTimeMillis()+"");
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
		
		getMain().monitorForm(thisJobDir, true);
	}

	public void init() {
		List seqs = getMain().getResourceManager().getOrganismElement("exampleSequences-form", "exampleSequences-sequences").getChildren();
		
		if(seqs.size()>0) {
			Element seq = (Element)seqs.get(0);
			StringBuilder text = new StringBuilder(">" + seq.getAttributeValue("name") + "\n");
			final int nucleotidesPerLine = 80;
			int counter = 0;
			String nucleotides = seq.getTextTrim();
			for(int i = 0; i<nucleotides.length(); i++) {
				if(!Character.isWhitespace(nucleotides.charAt(i))) {
					text.append(nucleotides.charAt(i));
					counter++;
					if(counter%nucleotidesPerLine==0) {
						text.append("\n");
					}
				}
			}
			ta.setText(text.toString());
		}
	}

	private void verifyFasta(String fastaContent) {
		int amountOfSeqs = 0;
		int i = 0;
		
		while(true) {
			i = fastaContent.indexOf('>', i);
			if(i!=-1) {
				amountOfSeqs++;
				i++;
			} else { 
				break;
			}
		}
		
		if(amountOfSeqs<=Settings.getInstance().getMaxAllowedSeqs()) {
			setValid(ta, errorSeq);
			startJob(fastaContent);
		} else {
			setInvalid(ta, errorSeq);
		}
	}
}
