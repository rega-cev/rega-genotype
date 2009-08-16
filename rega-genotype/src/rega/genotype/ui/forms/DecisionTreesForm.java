package rega.genotype.ui.forms;

import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WText;

import org.jdom.Element;

import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.GenotypeLib;

public class DecisionTreesForm extends IForm {
	public DecisionTreesForm(GenotypeWindow main) {
		super(main, "decisionTrees-form");
		
		String ruleNumber;
		String ruleName;
		int headerNr=0;
		
		Element text = main.getResourceManager().getOrganismElement("decisionTrees-form", "decisionTrees-text");
		for(Object o : text.getChildren()) {
			final Element e = (Element)o;
			if(e.getName().equals("header")) {
				WText header = new WText(lt((++headerNr) + ". " + getMain().getResourceManager().extractFormattedText(e) +":"), this);
				header.setStyleClass("decisionTreeHeader");
			} else if(e.getName().equals("rule")){
				ruleNumber = e.getAttributeValue("number");
				ruleName = e.getAttributeValue("name");
				new WText(lt(ruleNumber + ": " + ruleName + "<br></br>" + getMain().getResourceManager().extractFormattedText(e) + "<br></br>"), this);
			} else if(e.getName().equals("figure")) {
				WContainerWidget imgDiv = new WContainerWidget(this);
				imgDiv.setStyleClass("imgDiv");
				GenotypeLib.getWImageFromResource(getMain().getOrganismDefinition(),e.getTextTrim(), imgDiv);
			}
			new WBreak(this);
		}
	}
}
