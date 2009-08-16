package rega.genotype.ui.forms;

import net.sf.witty.wt.WText;
import rega.genotype.ui.framework.GenotypeWindow;

public class HowToCiteForm extends IForm {
	public HowToCiteForm(GenotypeWindow main) {
		super(main, "howToCite-form");
		WText toCiteText = new WText(main.getResourceManager().getOrganismValue("howToCite-form", "cite"), this);
	}
}
