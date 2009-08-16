package rega.genotype.ui.forms;

import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WText;

import org.jdom.Element;

import rega.genotype.ui.framework.GenotypeWindow;

public class ExampleSequencesForm extends IForm {
	public ExampleSequencesForm(GenotypeWindow main) {
		super(main, "exampleSequences-form");
		
		Element text = main.getResourceManager().getOrganismElement("exampleSequences-form", "exampleSequences-sequences");
		for(Object o : text.getChildren()) {
			final Element e = (Element)o;
			if(e.getName().equals("sequence")) {
				String sequence = ">" + e.getAttributeValue("name");
				sequence += "</br>";
				sequence += e.getTextTrim() + "</br>";
				new WText(lt(sequence), this);
			}
			new WBreak(this);
		}
	}
}
