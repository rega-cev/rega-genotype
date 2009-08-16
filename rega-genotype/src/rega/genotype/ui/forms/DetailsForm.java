package rega.genotype.ui.forms;

import java.io.File;

import net.sf.witty.wt.WTable;
import net.sf.witty.wt.WText;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.framework.GenotypeWindow;

public class DetailsForm extends IForm {
	private WTable mainTable;
	private IDetailsForm mainDetails;
	
	private SaxParser p;
	
	public DetailsForm(GenotypeWindow main) {
		super(main, "details-form");
		mainTable = new WTable(this);
	}
	
	public void init(File jobDir, OrganismDefinition od, final int selectedSequenceIndex) {
		p = new SaxParser(){
			@Override
			public void endSequence() {
				if(selectedSequenceIndex==getSequenceIndex()) {
					stopParsing();
				}
			}
		};
		p.parseFile(jobDir);
		
		mainTable.clear();
		
		mainDetails = od.getMainDetailsForm();
		mainDetails.fillForm(p, od, jobDir);
		int rowIndex = 0;
		mainTable.putElementAt(rowIndex++, 0, new WText(mainDetails.getTitle()));
		mainTable.putElementAt(rowIndex++, 0, mainDetails);
		
		for(IDetailsForm df : od.getSupportingDetailsforms(p)) {
			mainTable.putElementAt(rowIndex++, 0, new WText(df.getTitle()));
			mainTable.putElementAt(rowIndex++, 0, df);
			df.fillForm(p, od, jobDir);
		}
	}
	
	public String getSequenceName() {
		return p.getValue("genotype_result.sequence['name']");
	}
}
