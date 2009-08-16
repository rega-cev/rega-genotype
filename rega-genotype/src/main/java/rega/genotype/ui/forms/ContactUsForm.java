/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeWindow;

public class ContactUsForm extends DocumentationForm {
	public ContactUsForm(GenotypeWindow main) {
		super(main, "contactUs-form");
		
		fillForm("contactUs-form", "contactUs-text");
	}
}
