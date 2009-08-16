/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeWindow;

public class SubtypingProcessForm extends DocumentationForm {

	public SubtypingProcessForm(GenotypeWindow main) {
		super(main, "subtypingProcess-form");
		
		fillForm("subtypingProcess-form", "subtypingProcess-text");
	}
}