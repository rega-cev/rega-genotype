/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.framework;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.AbstractForm;
import rega.genotype.ui.forms.DocumentationForm;
import rega.genotype.ui.forms.JobForm;
import rega.genotype.ui.forms.StartForm;
import rega.genotype.ui.i18n.resources.GenotypeResourceManager;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.TextFormat;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WMenu;
import eu.webtoolkit.jwt.WMenuItem;
import eu.webtoolkit.jwt.WStackedWidget;
import eu.webtoolkit.jwt.WTemplate;
import eu.webtoolkit.jwt.WText;

/**
 * The frame of the application.
 * Defines the general user interface of the application.
 * 
 * @author simbre1
 *
 */
public class GenotypeWindow extends WContainerWidget
{
	private static final String START_URL = "/";
	private static final String EXAMPLES_URL = "/examples";
	private static final String INTRODUCTION_URL = "/introduction";
	private static final String METHOD_URL = "/method";
	private static final String DECISIONTREES_URL = "/decisiontrees";
	private static final String TUTORIAL_URL = "/tutorial";
	private static final String CITE_URL = "/cite";
	private static final String CONTACT_URL = "/contact";

	private WStackedWidget content;
	
	private OrganismDefinition od;
	
	public OrganismDefinition getOrganismDefinition() {
		return od;
	}

	private GenotypeResourceManager resourceManager;

	public GenotypeWindow(OrganismDefinition od)
	{
		super();
		this.od = od;
	}

	public GenotypeResourceManager getResourceManager() {
		return resourceManager;
	}
	private void loadI18nResources()
	{
		resourceManager = new GenotypeResourceManager("/rega/genotype/ui/i18n/resources/common_resources.xml", od.getOrganismDirectory()+"resources.xml");
		WApplication.getInstance().setLocalizedStrings(resourceManager);
	}

	public void init() {
		loadI18nResources();

		setStyleClass("root");
		setId("");
		WApplication app = WApplication.getInstance();
		
		app.useStyleSheet(Settings.getInstance().getStyleSheet(od));
		
		WTemplate main = new WTemplate(this);
		main.setStyleClass("azure");
		main.setTemplateText(resourceManager.getOrganismElementAsString("app", "template"), TextFormat.XHTMLUnsafeText);

		WImage headerImage = GenotypeLib.getWImageFromResource(od, "header.gif", this);
		main.bindWidget("header-image", headerImage);
		if (headerImage != null) {
			headerImage.setAlternateText("header");
			headerImage.setStyleClass("header");
			headerImage.setId("");
		}

		content = new WStackedWidget();
		main.bindWidget("content", content);
		content.setId("content");
		content.setStyleClass("content");

		WContainerWidget navigation = new WContainerWidget();
		main.bindWidget("navigation", navigation);
		navigation.setStyleClass("nav_bar");
		
		WMenu menu = new WMenu(content, Orientation.Horizontal, navigation);
		menu.setRenderAsList(true);
		menu.setStyleClass("nav_main");
		
		WText footer = new WText(resourceManager.getOrganismValue("main-form", "footer"));
		footer.setStyleClass("footer");
		footer.setId("");
		main.bindWidget("footer", footer);

		/*
		 * Set up the menu: links to all the forms:
		 */
		addLink(menu, tr("main.navigation.start"), START_URL, new StartForm(this));

		JobForm jobForm = new JobForm(this, od.getJobOverview(this));
		final String monitorNavigation = "main.navigation.monitor";
		final WMenuItem jobItem = addLink(menu, tr(monitorNavigation).arg(""), "", jobForm);
		jobForm.getJobIdChanged().addListener(this, new Signal1.Listener<String>() {
			public void trigger(String id) {
				jobItem.setPathComponent(JobForm.JOB_URL + "/" + id);
				jobItem.setText(tr(monitorNavigation).arg(id));
			}
		});

		addLink(menu, tr("main.navigation.howToCite"), CITE_URL, createDocForm("howToCite-form", "howToCite-text"));
		
		addLink(menu, tr("main.navigation.introduction"), INTRODUCTION_URL, createDocForm("introduction-form", "introduction-text"));
		
		addLink(menu, tr("main.navigation.tutorial"), TUTORIAL_URL, createDocForm("tutorial-form", "tutorial-text"));
		
		addLink(menu, tr("main.navigation.decisionTrees"), DECISIONTREES_URL, createDocForm("decisionTrees-form", "decisionTrees-text"));
		
		addLink(menu, tr("main.navigation.subtypingProcess"), METHOD_URL, createDocForm("subtypingProcess-form", "subtypingProcess-text"));
		
		addLink(menu, tr("main.navigation.exampleSequences"), EXAMPLES_URL, createDocForm("exampleSequences-form", "exampleSequences-sequences"));
		
		addLink(menu, tr("main.navigation.contactUs"), CONTACT_URL, createDocForm("contactUs-form", "contactUs-text"));
	}
	
	private DocumentationForm createDocForm(String name, String content) {
		try {
			return new DocumentationForm(this, name, content);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return null;
		}
	}
	
	private WMenuItem addLink(final WMenu menu, CharSequence text, String url, AbstractForm form) {
		if (form == null) 
			return null;
		
		WMenuItem i = new WMenuItem(text, form);
		menu.addItem(i);
		i.setPathComponent(url);
		
		return i;
	}
	
	public void changeInternalPath(String path) {
		WApplication app = WApplication.getInstance();
		app.setInternalPath(path, true);
		if (!app.getEnvironment().hasAjax())
			app.redirect(app.getBookmarkUrl(path));
	}
}
