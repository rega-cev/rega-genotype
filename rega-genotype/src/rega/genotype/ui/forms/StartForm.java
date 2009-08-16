package rega.genotype.ui.forms;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import net.sf.witty.wt.SignalListener;
import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WMouseEvent;
import net.sf.witty.wt.WPushButton;
import net.sf.witty.wt.WTextArea;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.GenotypeLib;

public class StartForm extends WContainerWidget {
	private WTextArea ta;
	private WPushButton run, clear;
	private GenotypeWindow main;
	
	public StartForm(OrganismDefinition vd, WContainerWidget parent, GenotypeWindow main) {
		super(parent);
		this.main = main;
		init(vd);
	}
	
	public void init(final OrganismDefinition od) {
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
					
					main.removeWidget(StartForm.this);
					AbstractJobOverview ajo = od.getJobOverview(thisJobDir, main.getResourceManager());
					main.addWidget(ajo);
					ajo.fillTable();
			}
		});
	}
}
