package rega.genotype.ui.forms;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.sf.witty.wt.SignalListener;
import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WEmptyEvent;
import net.sf.witty.wt.WInteractWidget;
import net.sf.witty.wt.WLineEdit;
import net.sf.witty.wt.WMouseEvent;
import net.sf.witty.wt.WPushButton;
import net.sf.witty.wt.WText;
import net.sf.witty.wt.WTextArea;
import net.sf.witty.wt.i8n.WMessage;

import org.apache.commons.io.FileUtils;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.ui.util.Settings;

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
		
		Map<String, String> noteArgs = new HashMap<String, String>();
		noteArgs.put("${maxAllowedSeqs}", Settings.getInstance().getMaxAllowedSeqs()+"");
		note = new WText(getMain().getResourceManager().getOrganismValue("start-form", "note", noteArgs), this);
		note.setStyleClass("note");
		
		WContainerWidget seqinput = new WContainerWidget(this);
		seqinput.setStyleClass("seqInput");
		
		new WText(tr("sequenceInput.inputSequenceInFastaFormat"), seqinput);
		new WBreak(seqinput);
		
		ta = new WTextArea(seqinput);
		ta.setColumns(100);
		ta.setRows(15);
		new WBreak(seqinput);
	
		fileUpload = new FileUpload();
		seqinput.addWidget(fileUpload);
		fileUpload.getUploadFile().uploaded.addListener(new SignalListener<WEmptyEvent>() {
            public void notify(WEmptyEvent a) {                
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
		clear.clicked.addListener(new SignalListener<WMouseEvent>() {
			public void notify(WMouseEvent a) {
				ta.setText("");
			}
		});
	
		run.clicked.addListener(new SignalListener<WMouseEvent>() {
			public void notify(WMouseEvent a) {
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
		monitorButton.clicked.addListener(new SignalListener<WMouseEvent>() {
			public void notify(WMouseEvent a) {
				File jobDir = new File(Settings.getInstance().getJobDir().getAbsolutePath()+File.separatorChar+jobIdTF.text());
				if(jobDir.exists()) {
					setValid(jobIdTF, errorJobId);
					getMain().monitorForm(jobDir);
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
		final File thisJobDir = GenotypeLib.createJobDir();
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
		
		getMain().monitorForm(thisJobDir);
	}

	public void init() {
		ta.setText("> seqname\nACGTACGGAAACGATACAAGATACAAGATAACA");
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
