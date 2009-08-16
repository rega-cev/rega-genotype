package rega.genotype.ui.forms;

import java.io.File;

import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WTable;
import net.sf.witty.wt.WText;
import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.framework.widgets.WListContainerWidget;

public class DetailsForm extends IForm {
	private WTable mainTable;
	private IDetailsForm mainDetails;
	
	private SaxParser p;
	
	public DetailsForm(GenotypeWindow main) {
		super(main, "details-form");
		mainTable = new WTable(this);
		mainTable.setStyleClass("detailsForm");
	}
	
	public void init(File jobDir, final int selectedSequenceIndex) {
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
		
		mainDetails = getMain().getOrganismDefinition().getMainDetailsForm();
		mainDetails.fillForm(p, getMain().getOrganismDefinition(), jobDir);
		int rowIndex = 0;
		mainTable.putElementAt(rowIndex, 0, new WText(mainDetails.getTitle()));
		mainTable.elementAt(rowIndex++, 0).setStyleClass("title");
		mainTable.putElementAt(rowIndex, 0, mainDetails);
		mainTable.elementAt(rowIndex++, 0).setStyleClass("details");
		
		mainTable.putElementAt(rowIndex, 0, new WText(tr("details.analysisDetails")));
		mainTable.elementAt(rowIndex++, 0).setStyleClass("title");
		
		WContainerWidget details = new WContainerWidget(mainTable.elementAt(rowIndex++, 0));
		WListContainerWidget ul = new WListContainerWidget(details);
		WContainerWidget li;
		for(IDetailsForm df : getMain().getOrganismDefinition().getSupportingDetailsforms(p)) {
			String detailTitle = df.getTitle().value();
			WText titleText = new WText(lt("<a href=\"#" + detailTitle.replace(" ", "").toLowerCase() + "\">"+detailTitle+"</a>"));
			titleText.setStyleClass("link");
			li = ul.addItem(titleText);
			li.addWidget(new WBreak());
			li.addWidget(new WText(df.getComment()));
			li.addWidget(new WBreak());
			
			if(df.getExtraComment()!=null) {
				WText extraComment = new WText(df.getExtraComment());
				details.addWidget(extraComment);
				extraComment.setStyleClass("details-extraComments");
			}
		}
		
		for(IDetailsForm df : getMain().getOrganismDefinition().getSupportingDetailsforms(p)) {
			String detailTitle = df.getTitle().value();
			WText titleText = new WText(lt("<a name=\"" + detailTitle.replace(" ", "").toLowerCase() + "\">"+detailTitle+"</a>"));
			mainTable.putElementAt(rowIndex, 0, titleText);
			mainTable.elementAt(rowIndex++, 0).setStyleClass("title");
			mainTable.putElementAt(rowIndex, 0, df);
			mainTable.elementAt(rowIndex++, 0).setStyleClass("details");
			df.fillForm(p, getMain().getOrganismDefinition(), jobDir);
		}
	}
	
	public String getSequenceName() {
		return p.getValue("genotype_result.sequence['name']");
	}
}
