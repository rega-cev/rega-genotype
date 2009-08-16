/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.etv;

import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WString;

/**
 * Enterovirus implementation of the genotype application.
 */
@SuppressWarnings("serial")
public class EtvMain extends GenotypeMain {
	public EtvMain() {
		super();
		
		getConfiguration().setInlineCss(false);
	}
	
	@Override
	public WApplication createApplication(WEnvironment env) {
		GenotypeApplication app = new GenotypeApplication(env, this.getServletContext(), new EtvDefinition());
		app.setTitle(WString.tr("etvTool.title"));
		
		return app;
	}
}
