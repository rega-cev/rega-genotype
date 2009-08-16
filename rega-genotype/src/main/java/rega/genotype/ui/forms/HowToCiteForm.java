package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeWindow;

public class HowToCiteForm extends DocumentationForm {
	public HowToCiteForm(GenotypeWindow main) {
		super(main, "howToCite-form");
		
		fillForm("howToCite-form", "howToCite-text");
	}
}
