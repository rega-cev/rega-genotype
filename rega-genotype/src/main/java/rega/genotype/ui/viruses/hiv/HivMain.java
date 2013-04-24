/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.hiv;

import rega.genotype.ui.forms.DocumentationForm;
import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
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
		
		window.addForm("documentation", new DocumentationForm(window, tr("documentation-text")));	
		window.addForm("contact-us", new DocumentationForm(window, tr("contactUs-text")));
		window.addForm("how-to-cite", new DocumentationForm(window, tr("howToCite-text")));
			
		app.getRoot().addWidget(window);
		
		return app;
	}
	
	private WString tr(String key) {
		return WString.tr(key);
	}
}
