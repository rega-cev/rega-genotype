/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.parasites.giardia;

import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WString;

/**
 * Enterovirus implementation of the genotype application.
 */
@SuppressWarnings("serial")
public class GiardiaMain extends GenotypeMain {
	public GiardiaMain() {
		super();
		
		getConfiguration().setInlineCss(false);
	}
	
	@Override
	public WApplication createApplication(WEnvironment env) {
		GenotypeApplication app = new GenotypeApplication(env, this.getServletContext(), new GiardiaDefinition(), settings);
		app.setTitle(WString.tr("giardiaTool.title"));
		
		app.useStyleSheet("../style/genotype-rivm.css");
		app.useStyleSheet("../style/genotype-rivm-ie.css", "IE lte 7");
		
		return app;
	}
}
