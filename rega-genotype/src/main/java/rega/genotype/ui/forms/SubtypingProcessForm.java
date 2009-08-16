package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeWindow;

public class SubtypingProcessForm extends DocumentationForm {

	public SubtypingProcessForm(GenotypeWindow main) {
		super(main, "subtypingProcess-form");
		
		fillForm("subtypingProcess-form", "subtypingProcess-text");
	}
}