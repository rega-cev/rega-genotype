/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeWindow;

/**
 * The example sequences documentation form widget.
 */
public class ExampleSequencesForm extends DocumentationForm {
	public ExampleSequencesForm(GenotypeWindow main) {
		super(main, "exampleSequences-form");
		
		fillForm("exampleSequences-form", "exampleSequences-sequences");
	}
}
