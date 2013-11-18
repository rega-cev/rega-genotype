/*
 * Copyright (C) 2013 Emweb
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.generic;

import java.io.File;
import java.net.MalformedURLException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import rega.genotype.GenotypeTool;
import rega.genotype.ui.forms.DocumentationForm;
import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WXmlLocalizedStrings;

/**
 * Enterovirus implementation of the genotype application.
 */
@SuppressWarnings("serial")
public class GenericMain extends GenotypeMain {
	private String organism;

	public GenericMain() {
		super();
		
		getConfiguration().setInlineCss(false);
		getConfiguration().setProgressiveBootstrap(true);
	}
	
	@Override
	public WApplication createApplication(WEnvironment env) {
		GenericDefinition definition = new GenericDefinition(organism);
		GenotypeApplication app = new GenotypeApplication(env, this.getServletContext(), definition, settings);

		WXmlLocalizedStrings resources = new WXmlLocalizedStrings();
		resources.use("/rega/genotype/ui/i18n/resources/common_resources");
		resources.use(definition.getXmlFolder() + "resources");
		app.setLocalizedStrings(resources);
		
		app.setTitle(WString.tr("tool.title"));
		app.useStyleSheet(new WLink("../style/genotype-rivm.css"));
		app.useStyleSheet(new WLink("../style/genotype-rivm-ie.css"), "IE lte 7");

		GenotypeWindow window = new GenotypeWindow(definition);

		for (GenericDefinition.MenuItem item : definition.getMenuItems())
			window.addForm(item.label, item.path, new DocumentationForm(window, WString.tr(item.messageId)));
		
		window.init();

		app.getRoot().addWidget(window);
		
		return app;
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		this.organism = config.getInitParameter("Organism");
		if (organism == null)
			throw new ServletException("Need 'Organism' parameter");

		Settings.initSettings(this.settings = Settings.getInstance());
		
		super.init(config);
	}
}
