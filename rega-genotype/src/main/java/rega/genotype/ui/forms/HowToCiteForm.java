/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeWindow;

/**
 * The how to cite documentation form widget.
 */
public class HowToCiteForm extends DocumentationForm {
	public HowToCiteForm(GenotypeWindow main) {
		super(main, "howToCite-form");
		
		fillForm("howToCite-form", "howToCite-text");
	}
}
