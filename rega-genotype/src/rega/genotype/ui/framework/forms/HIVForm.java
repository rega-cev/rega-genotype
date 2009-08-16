package rega.genotype.ui.framework.forms;

import java.io.File;

import net.sf.witty.wt.WContainerWidget;
import rega.genotype.viruses.hiv.HIVTool;

public class HIVForm extends VirusForm{
	HIVTool hivTool;
	
	public HIVForm(WContainerWidget parent){
		super(parent);
		init();
	}
	
	private void init(){
		
	}
	
	public void run(File jobDir, String sequences){
		try{
			hivTool = new HIVTool(jobDir);
			//hivTool.analyze(sequences, traceFile);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}
