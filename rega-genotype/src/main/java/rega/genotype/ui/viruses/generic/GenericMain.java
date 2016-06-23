/*
 * Copyright (C) 2013 Emweb
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.generic;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.jdom.JDOMException;

import rega.genotype.config.Config.ToolConfig;
import rega.genotype.ui.forms.DocumentationForm;
import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.framework.exeptions.RegaGenotypeExeption;
import eu.webtoolkit.jwt.Configuration.ErrorReporting;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WXmlLocalizedStrings;

/**
 * Generic implementation of the genotype application.
 */
@SuppressWarnings("serial")
public class GenericMain extends GenotypeMain {
	public GenericMain() {
		super();
		getConfiguration().setInternalDeploymentSize(1);
		getConfiguration().setMaximumRequestSize(1000000000);

		getConfiguration().setInlineCss(false);
		getConfiguration().setProgressiveBootstrap(true);
		getConfiguration().setErrorReporting(ErrorReporting.NoErrors);
	}
	
	@Override
	public WApplication createApplication(WEnvironment env) {		
		String[] deploymentPath = env.getDeploymentPath().split("/");
		String url = deploymentPath[deploymentPath.length - 1];
		getConfiguration().setFavicon("/"+deploymentPath[1]+"/pics/favicon1.ico");
		ToolConfig toolConfig;

		if (settings.getConfig() == null 
				|| settings.getConfig().getToolConfigByUrlPath(url) == null
				|| settings.getConfig().getToolConfigByUrlPath(url).getUniqueToolId() == null) {			
			WApplication app = new WApplication(env);
			app.getRoot().addWidget(new WText("Typing tool for organism " + url + " was not found."));
			return app;
		} else 
			toolConfig = settings.getConfig().getToolConfigByUrlPath(url);

		if (!toolConfig.isUi()) {
			WApplication app = new WApplication(env);
			app.getRoot().addWidget(new WText("Typing tool for organism " + url + " was not found. (UI is disabled)"));
			return app;
		}
		
		String urlComponent = toolConfig.getPath();
		GenotypeApplication app;
		try {
			app = new GenotypeApplication(env, 
					this.getServletContext(), settings, urlComponent);
		} catch (RegaGenotypeExeption e1) {
			e1.printStackTrace();
			WApplication a = new WApplication(env);
			a.getRoot().addWidget(new WText(e1.getMessage()));
			return a;
		}

		GenericDefinition definition;
		try {
			definition = new GenericDefinition(toolConfig);
		} catch (JDOMException e) {
			e.printStackTrace();
			showErrorMsg(app);
			return app;
		} catch (IOException e) {
			e.printStackTrace();
			showErrorMsg(app);
			return app;
		}
		
		WXmlLocalizedStrings resources = new WXmlLocalizedStrings();
		resources.use("/rega/genotype/ui/i18n/resources/common_resources");
		resources.use(definition.getXmlPath() + File.separator + "resources");
		app.setLocalizedStrings(resources);
		
		app.setTitle(WString.tr("tool.title"));

		app.useStyleSheet(new WLink("../style/genotype-rivm.css"));
		app.useStyleSheet(new WLink("../style/genotype-rivm-ie.css"), "IE lte 7");

		if (getConfiguration().internalDeploymentSize() == 1) {
			app.useStyleSheet(new WLink("../../style/genotype-rivm.css"));
			app.useStyleSheet(new WLink("../../style/genotype-rivm-ie.css"),
					"IE lte 7");

			app.useStyleSheet(new WLink("../../style/wt.css")); // do not use Wt's inline stylesheet...
			app.useStyleSheet(new WLink("../../style/wt_ie.css"), "IE lt 7"); // do not use Wt's inline stylesheet...
		}
		if (!WString.tr((String)"tool.meta.robots").equals((Object)"??tool.meta.robots??")) {
            app.addMetaHeader("robots", (CharSequence)WString.tr((String)"tool.meta.robots"));
        }
        if (!WString.tr((String)"tool.meta.title").equals((Object)"??tool.meta.title??")) {
            app.addMetaHeader("title", (CharSequence)WString.tr((String)"tool.meta.title"));
        }
        if (!WString.tr((String)"tool.meta.generator").equals((Object)"??tool.meta.generator??")) {
            app.addMetaHeader("generator", (CharSequence)WString.tr((String)"tool.meta.generator"));
        }
        if (!WString.tr((String)"tool.meta.Content-Language").equals((Object)"??tool.meta.Content-Language??")) {
            app.addMetaHeader("Content-Language", (CharSequence)WString.tr((String)"tool.meta.Content-Language"));
        }
        if (!WString.tr((String)"tool.meta.description").equals((Object)"??tool.meta.description??")) {
            app.addMetaHeader("description", (CharSequence)WString.tr((String)"tool.meta.description"));
        }
        if (!WString.tr((String)"tool.meta.keywords").equals((Object)"??tool.meta.keywords??")) {
            app.addMetaHeader("keywords", (CharSequence)WString.tr((String)"tool.meta.keywords"));
        }
        if (!WString.tr((String)"tool.meta.google-site-verification").equals((Object)"??tool.meta.google-site-verification??")) {
            app.addMetaHeader("google-site-verification", (CharSequence)WString.tr((String)"tool.meta.google-site-verification"));
        }
		GenotypeWindow window = new GenotypeWindow(definition);

		for (GenericDefinition.MenuItem item : definition.getMenuItems())
			window.addForm(item.label, item.path, new DocumentationForm(window, WString.tr(item.messageId)));
		
		window.init();

		app.getRoot().addWidget(window);
		
		return app;
	}
	
	private void showErrorMsg(GenotypeApplication app) {
		app.getRoot().addWidget(new WText("Typing tool for given url does not exist."));
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		//this.organism = config.getInitParameter("Organism");
		super.init(config);
	}
}
