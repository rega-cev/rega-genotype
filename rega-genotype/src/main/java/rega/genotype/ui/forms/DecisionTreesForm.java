/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeWindow;

/**
 * The decision trees documentation form widget.
 */
public class DecisionTreesForm extends DocumentationForm {
	public DecisionTreesForm(GenotypeWindow main) {
		super(main, "decisionTrees-form");
		
		fillForm("decisionTrees-form", "decisionTrees-text");
	}
}
