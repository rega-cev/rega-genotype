/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.nov;

import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WString;

/**
 * NoV implementation of the genotype application.
 * 
 * @author simbre1
 *
 */
@SuppressWarnings("serial")
public class NovMain extends GenotypeMain {
	public NovMain() {
		super();
		
		configuration().setInlineCss(false);
	}
	
	@Override
	public WApplication createApplication(WEnvironment env) {
		GenotypeApplication app = new GenotypeApplication(env, this.getServletContext(), new NovDefinition());
		app.setTitle(WString.tr("nrvTool.title"));
		
		return app;
	}
}
