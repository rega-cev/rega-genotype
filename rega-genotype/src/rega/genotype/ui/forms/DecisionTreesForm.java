package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeWindow;

public class DecisionTreesForm extends DocumentationForm {
	public DecisionTreesForm(GenotypeWindow main) {
		super(main, "decisionTrees-form");
		
		fillForm("decisionTrees-form", "decisionTrees-text");
	}
}
