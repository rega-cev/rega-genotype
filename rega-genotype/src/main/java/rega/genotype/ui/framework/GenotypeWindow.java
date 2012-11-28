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
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WMenu;
import eu.webtoolkit.jwt.WMenuItem;
import eu.webtoolkit.jwt.WStackedWidget;
import eu.webtoolkit.jwt.WTemplate;

/**
 * The frame of the application.
 * Defines the general user interface of the application.
 * 
 * @author simbre1
 *
 */
public class GenotypeWindow extends WContainerWidget
{
	private static final String START_URL = "";
	private static final String EXAMPLES_URL = "examples";
	private static final String INTRODUCTION_URL = "introduction";
	private static final String METHOD_URL = "method";
	private static final String DECISIONTREES_URL = "decisiontrees";
	private static final String TUTORIAL_URL = "tutorial";
	private static final String CITE_URL = "cite";
	private static final String CONTACT_URL = "contact";

	private WStackedWidget content;
	
	private OrganismDefinition od;
	
	public OrganismDefinition getOrganismDefinition() {
		return od;
	}

	public GenotypeWindow(OrganismDefinition od)
	{
		super();
		this.od = od;
	}

	public void init() {
		setStyleClass("root");
		setId("");
		WApplication app = WApplication.getInstance();

		WTemplate main = new WTemplate(tr("main"), this);
		main.bindString("app.url", app.resolveRelativeUrl(app.getBookmarkUrl("/")));

		content = new WStackedWidget();
		main.bindWidget("content", content);
		content.setId("content");
		content.setStyleClass("content");

		WContainerWidget navigation = new WContainerWidget();
		main.bindWidget("navigation", navigation);
		navigation.setStyleClass("nav_bar");
		
		final WMenu menu = new WMenu(content, Orientation.Horizontal, navigation);
		menu.setRenderAsList(true);
		menu.setStyleClass("nav_main");

		/*
		 * Set up the menu: links to all the forms:
		 */
		addLink(menu, tr("main.navigation.start"), START_URL, new StartForm(this));

		JobForm jobForm = new JobForm(this, od.getJobOverview(this));
		final String monitorNavigation = "main.navigation.monitor";
		final WMenuItem jobItem = addLink(menu, tr(monitorNavigation).arg(""), JobForm.JOB_URL, jobForm);
		jobForm.getJobIdChanged().addListener(this, new Signal1.Listener<String>() {
			public void trigger(String id) {
				jobItem.setPathComponent(JobForm.JOB_URL + "/" + id);
				jobItem.setText(tr(monitorNavigation).arg(id));
				if (menu.getCurrentItem() != jobItem)
					jobItem.select();
			}
		});

		addLink(menu, tr("main.navigation.howToCite"), CITE_URL, new DocumentationForm(this, tr("howToCite-text")));
		
		addLink(menu, tr("main.navigation.introduction"), INTRODUCTION_URL, new DocumentationForm(this, "introduction-text"));
		
		addLink(menu, tr("main.navigation.tutorial"), TUTORIAL_URL, new DocumentationForm(this, "tutorial-text"));
		
		addLink(menu, tr("main.navigation.decisionTrees"), DECISIONTREES_URL, new DocumentationForm(this, "decisionTrees-text"));
		
		addLink(menu, tr("main.navigation.subtypingProcess"), METHOD_URL, new DocumentationForm(this, "subtypingProcess-text"));
		
		addLink(menu, tr("main.navigation.exampleSequences"), EXAMPLES_URL, new DocumentationForm(this, "exampleSequences-text"));
		
		addLink(menu, tr("main.navigation.contactUs"), CONTACT_URL, new DocumentationForm(this, "contactUs-text"));
		
		menu.setInternalPathEnabled("/");
		
		jobForm.handleInternalPath();
	}
	
	private WMenuItem addLink(final WMenu menu, CharSequence text, String url, AbstractForm form) {
		if (form == null) 
			return null;
		
		WMenuItem i = new WMenuItem(text, form);
		menu.addItem(i);
		i.setPathComponent(url);

		WAnchor a = (WAnchor) i.getItemWidget();
		a.setWordWrap(true);

		return i;
	}
	
	public void changeInternalPath(String path) {
		WApplication app = WApplication.getInstance();
		app.setInternalPath(path, true);
	}
}
