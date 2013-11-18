/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.etv;

import rega.genotype.ui.forms.DocumentationForm;
import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WXmlLocalizedStrings;

/**
 * Enterovirus implementation of the genotype application.
 */
@SuppressWarnings("serial")
public class EtvMain extends GenotypeMain {
	public EtvMain() {
		super();
		
		getConfiguration().setInlineCss(false);
		getConfiguration().setProgressiveBootstrap(true);
	}
	
	@Override
	public WApplication createApplication(WEnvironment env) {
		GenotypeApplication app = new GenotypeApplication(env, this.getServletContext(), new EtvDefinition(), settings);
		app.setTitle(WString.tr("etvTool.title"));
		app.useStyleSheet(new WLink("../style/genotype-rivm.css"));
		app.useStyleSheet(new WLink("../style/genotype-rivm-ie.css"), "IE lte 7");

		WXmlLocalizedStrings resources = new WXmlLocalizedStrings();
		resources.use("/rega/genotype/ui/i18n/resources/common_resources");
		resources.use("/rega/genotype/ui/viruses/etv/resources");
		app.setLocalizedStrings(resources);
		
		GenotypeWindow window = new GenotypeWindow(new EtvDefinition());

		window.addForm("How to cite", "how-to-cite", new DocumentationForm(window, WString.tr("how-to-cite-doc")));
		window.addForm("Introduction", "introduction", new DocumentationForm(window, WString.tr("introduction-doc")));
		window.addForm("How to use", "tutorial", new DocumentationForm(window, WString.tr("tutorial-doc")));
		window.addForm("(Sub)typing process", "method", new DocumentationForm(window, WString.tr("method-doc")));
		window.addForm("Example sequences", "examples", new DocumentationForm(window, WString.tr("examples-doc")));
		
		window.init();

		app.getRoot().addWidget(window);
		
		return app;
	}
}
