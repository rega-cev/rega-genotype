/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import eu.webtoolkit.jwt.WTemplate;

/**
 * A documentation form that visualizes the content of the template text passed to the constructor.
 */
public class DocumentationForm extends AbstractForm {
	public DocumentationForm(GenotypeWindow main, CharSequence content) {
		super(main);
		
		WTemplate t = new WTemplate(content, this);
		t.bindString("app.base.url", GenotypeMain.getApp().getEnvironment().getDeploymentPath());
		t.bindString("app.context", GenotypeMain.getApp().getServletContext().getContextPath());
	}

	@Override
	public void handleInternalPath(String internalPath) {

	}
}
