package rega.genotype.ui.forms;

import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WText;

import org.jdom.Element;

import rega.genotype.ui.framework.GenotypeWindow;

public class ContactUsForm extends IForm {
	public ContactUsForm(GenotypeWindow main) {
		super(main, "contactUs-form");
		
		Element text = main.getResourceManager().getOrganismElement("contactUs-form", "contactUs-text");
		for(Object o : text.getChildren()) {
			final Element e = (Element)o;
			if(e.getName().equals("text")) {
				new WText(lt(getMain().getResourceManager().extractFormattedText(e)), this);
			}
			new WBreak(this);
		}
	}
}
