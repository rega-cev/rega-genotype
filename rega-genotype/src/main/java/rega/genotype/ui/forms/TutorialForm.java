package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeWindow;

public class TutorialForm extends DocumentationForm {
	public TutorialForm(GenotypeWindow main) {
		super(main, "tutorial-form");
		
		fillForm("tutorial-form", "tutorial-text");
	}
}
