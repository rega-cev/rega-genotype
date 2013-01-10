/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.hiv;

import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WCombinedLocalizedStrings;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WXmlLocalizedStrings;

/**
 * HIV implementation of the genotype application.
 * 
 * @author simbre1
 *
 */

@SuppressWarnings("serial")
public class HivMain extends GenotypeMain {
	@Override
	public WApplication createApplication(WEnvironment env) {
		GenotypeApplication app = new GenotypeApplication(env, this.getServletContext());
		
		WCombinedLocalizedStrings resources = new WCombinedLocalizedStrings();
		
		WXmlLocalizedStrings commonResources = new WXmlLocalizedStrings();
		commonResources.use("/rega/genotype/ui/i18n/resources/common_resources");
		resources.add(commonResources);
		
		WXmlLocalizedStrings hivResources = new WXmlLocalizedStrings();
		hivResources.use("/rega/genotype/ui/viruses/hiv/resources");
		resources.add(hivResources);
		
		app.setLocalizedStrings(resources);
		
		app.useStyleSheet("../style/hiv/genotype.css");
		
		GenotypeWindow window = new GenotypeWindow(new HivDefinition());
		window.init();
		
		window.addDocumentationLink(tr("main.navigation.documentation"), "examples", tr("documentation-text"));	
		window.addDocumentationLink(tr("main.navigation.contactUs"), "contact", tr("contactUs-text"));
			
		app.getRoot().addWidget(window);
		
		return app;
	}
	
	private WString tr(String key) {
		return WString.tr(key);
	}
}
