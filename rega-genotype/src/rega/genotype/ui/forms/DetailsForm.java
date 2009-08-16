package rega.genotype.ui.forms;

import java.io.File;

import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WTable;
import net.sf.witty.wt.WText;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.SaxParser;

public class DetailsForm extends WContainerWidget {
	private WText title;
	private WTable mainTable;
	private IDetailsForm mainDetails;
	
	private SaxParser p;
	
	public DetailsForm() {
		title = new WText(lt("TITLE"), this);
		mainTable = new WTable(this);
	}
	
	public void init(File jobDir, final int selectedSequenceIndex, OrganismDefinition od) {
		p = new SaxParser(){
			@Override
			public void endSequence() {
				if(selectedSequenceIndex==getSequenceIndex()) {
					stopParsing();
				}
			}
		};
		p.parseFile(jobDir);
		
		mainDetails = od.getMainDetailsForm();
		mainDetails.fillForm(p, od, jobDir);
		mainTable.putElementAt(0, 0, new WText(mainDetails.getTitle()));
		mainTable.putElementAt(1, 0, mainDetails);
	}
}
