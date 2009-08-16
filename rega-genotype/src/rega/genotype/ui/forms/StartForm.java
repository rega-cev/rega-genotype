package rega.genotype.ui.forms;

import java.io.File;
import java.io.IOException;

import net.sf.witty.wt.SignalListener;
import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WMouseEvent;
import net.sf.witty.wt.WPushButton;
import net.sf.witty.wt.WText;
import net.sf.witty.wt.WTextArea;

import org.apache.commons.io.FileUtils;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.GenotypeLib;

public class StartForm extends IForm {
	private WText title;
	private WText note;
	private WTextArea ta;
	private WPushButton run, clear;
	
	public StartForm(final OrganismDefinition od, WContainerWidget parent, GenotypeWindow main) {
		super(main, "start-form");
		
		title = new WText(getMain().getResourceManager().getOrganismValue("start-form", "title"), this);
		new WBreak(this);
		new WBreak(this);
		
		note = new WText(getMain().getResourceManager().getOrganismValue("start-form", "note"), this);
		new WBreak(this);
		new WBreak(this);
		
		new WText(tr("sequenceInput.inputSequenceInFastaFormat"), this);
		new WBreak(this);
		
		ta = new WTextArea(this);
		ta.setColumns(100);
		ta.setRows(15);
		new WBreak(this);
	
		run = new WPushButton(this);
		run.setText(tr("sequenceInput.run"));
		
		clear = new WPushButton(this);
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
								od.startAnalysis(thisJobDir);
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

	public void init(File jobDir, OrganismDefinition od, final int selectedSequenceIndex) {
		ta.setText("");
	}
}
