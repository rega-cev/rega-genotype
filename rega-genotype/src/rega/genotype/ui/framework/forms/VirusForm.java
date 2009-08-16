package rega.genotype.ui.framework.forms;

import java.io.File;

import net.sf.witty.wt.WContainerWidget;
import rega.genotype.ui.framework.widgets.SequenceInputWidget;
import rega.genotype.ui.util.GenotypeLib;

public abstract class VirusForm extends WContainerWidget{
	private SequenceInputWidget input;
	private File jobDir;
	
	public VirusForm(WContainerWidget parent){
		super(parent);
		init();
	}
	
	private void init(){
		input = new SequenceInputWidget(this){
			public void submit(String sequences){
				run(sequences);
			}
		};
	}
	
	private void run(String sequences){
		jobDir = GenotypeLib.createJobDir();
		run(getJobDir(), sequences);
	}
	public abstract void run(File jobDir, String sequences);


	public File getJobDir() {
		return jobDir;
	}
}
