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
import eu.webtoolkit.jwt.WString;
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

	private WStackedWidget content;
	private WMenu menu;
	
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
		GenotypeApplication app = GenotypeMain.getApp();

		content = new WStackedWidget(app.getRoot());
		content.setId("content");
		content.setStyleClass("content");
		
		this.menu = new WMenu(content, Orientation.Horizontal);
		menu.setRenderAsList(true);

		/*
		 * Set up the menu: links to all the forms:
		 */
		addLink(tr("main.navigation.start"), START_URL, new StartForm(this));

		JobForm jobForm = new JobForm(this, od.getJobOverview(this));
		final String monitorNavigation = "main.navigation.monitor";
		final WMenuItem jobItem = addLink(tr(monitorNavigation).arg(""), JobForm.JOB_URL, jobForm);
		jobForm.getJobIdChanged().addListener(this, new Signal1.Listener<String>() {
			public void trigger(String id) {
				jobItem.setPathComponent(JobForm.JOB_URL + "/" + id);
				jobItem.setText(tr(monitorNavigation).arg(id));
				if (menu.getCurrentItem() != jobItem)
					jobItem.select();
			}
		});

		menu.setInternalPathEnabled("/");
		
		jobForm.handleInternalPath();
	}
	
	public WMenuItem addDocumentationLink(CharSequence text, String url, CharSequence docText) {
		return addLink(text, url, new DocumentationForm(this, docText));
	}
	
	private WMenuItem addLink(CharSequence text, String url, AbstractForm form) {
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

	public WMenu getMenu() {
		return menu;
	}
}
