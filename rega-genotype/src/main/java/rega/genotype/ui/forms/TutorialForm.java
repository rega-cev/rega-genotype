/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeWindow;

public class TutorialForm extends DocumentationForm {
	public TutorialForm(GenotypeWindow main) {
		super(main, "tutorial-form");
		
		fillForm("tutorial-form", "tutorial-text");
	}
}
