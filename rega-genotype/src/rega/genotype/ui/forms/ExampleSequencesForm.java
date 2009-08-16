package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeWindow;

public class ExampleSequencesForm extends DocumentationForm {
	public ExampleSequencesForm(GenotypeWindow main) {
		super(main, "exampleSequences-form");
		
		fillForm("exampleSequences-form", "exampleSequences-sequences");
	}
}
