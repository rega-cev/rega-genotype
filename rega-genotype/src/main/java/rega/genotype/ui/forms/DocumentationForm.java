/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.framework.widgets.Template;
import eu.webtoolkit.jwt.WApplication;

/**
 * A documentation form that visualizes the content of the template text passed to the constructor.
 */
public class DocumentationForm extends AbstractForm {
	public DocumentationForm(GenotypeWindow main, CharSequence content) {
		super(main);
		
		WApplication app = WApplication.getInstance();
		
		Template t = new Template(content, this);
		t.setInternalPathEncoding(true);
		t.bindString("app.url", app.resolveRelativeUrl(app.getBookmarkUrl("/")));
		t.bindString("app.base.url", app.getEnvironment().getDeploymentPath());
		t.bindString("app.context", GenotypeMain.getApp().getServletContext().getContextPath());
	}

	@Override
	public void handleInternalPath(String internalPath) {

	}
}
