package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeWindow;

public class ContactUsForm extends DocumentationForm {
	public ContactUsForm(GenotypeWindow main) {
		super(main, "contactUs-form");
		
		fillForm("contactUs-form", "contactUs-text");
	}
}
