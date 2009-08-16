package rega.genotype.ui.forms;

import java.io.File;
import java.io.IOException;

import net.sf.witty.wt.SignalListener;
import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WEmptyEvent;
import net.sf.witty.wt.WMouseEvent;
import net.sf.witty.wt.WPushButton;
import net.sf.witty.wt.WText;
import net.sf.witty.wt.WTextArea;

import org.apache.commons.io.FileUtils;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.ui.util.GenotypeLib;

public class StartForm extends IForm {
	private WText note;
	private WTextArea ta;
	private WPushButton run, clear;
	private FileUpload fileUpload;
	
	public StartForm(GenotypeWindow main) {
		super(main, "start-form");
		
		new WBreak(this);
		
		note = new WText(getMain().getResourceManager().getOrganismValue("start-form", "note"), this);
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
					String s  = ">lala\n actg";
					ta.setText(s);
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
					final File thisJobDir = GenotypeLib.createJobDir();
					Thread analysis = new Thread(new Runnable(){
						public void run() {
							try {
								File seqFile = new File(thisJobDir.getAbsolutePath()+File.separatorChar+"sequences.fasta");
								FileUtils.writeStringToFile(seqFile, ta.text());
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
		});
	}

	public void init() {
		ta.setText("");
	}
}
