/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.framework;

import java.util.ArrayList;
import java.util.List;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.AbstractForm;
import rega.genotype.ui.forms.JobForm;
import rega.genotype.ui.forms.StartForm;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WMenu;
import eu.webtoolkit.jwt.WMenuItem;
import eu.webtoolkit.jwt.WStackedWidget;
import eu.webtoolkit.jwt.WTemplate;
import eu.webtoolkit.jwt.Signal1.Listener;

/**
 * The frame of the application.
 * Defines the general user interface of the application.
 * 
 * @author simbre1
 *
 */
public class GenotypeWindow extends WContainerWidget
{
	private static class Form {
		public Form(String path, AbstractForm form) {
			this.path = path;
			this.form = form;
		}
		
		String path;
		AbstractForm form;
	}
	
	private static final String START_URL = "";

	private WStackedWidget content;
	private List<Form> forms = new ArrayList<Form>();
	
	private OrganismDefinition od;

	private WMenu menu;
	
	public OrganismDefinition getOrganismDefinition() {
		return od;
	}

	public GenotypeWindow(OrganismDefinition od)
	{
		super();
		this.od = od;

		WApplication app = WApplication.getInstance();
		WTemplate main = new WTemplate(tr("main"), this);
		
		main.bindString("app.url", app.resolveRelativeUrl(app.getBookmarkUrl("/")));

		content = new WStackedWidget();
		content.setId("content");

		menu = new WMenu(content);
		menu.setId("menu");
		menu.setInternalPathEnabled("/");

		main.bindWidget("content", content);
		main.bindWidget("navigation", menu);
		
		addForm("Submit Job", START_URL, new StartForm(this));
		
		JobForm jobForm = new JobForm(this, od.getJobOverview(this));
		final WMenuItem item = addForm(tr("main.navigation.monitor").arg(""), JobForm.JOB_URL, jobForm);
		jobForm.jobIdChanged().addListener(this, new Listener<String>() {
			public void trigger(String jobId) {
				item.setText(tr("main.navigation.monitor").arg(jobId));
				item.setPathComponent(JobForm.JOB_URL + "/" + jobId);
			}
		});
	}

	public void init() {
		WApplication app = WApplication.getInstance();
		handleInternalPath(app.getInternalPath());

		app.internalPathChanged().addListener(this,
				new Signal1.Listener<String>() {
					public void trigger(String internalPath) {
						handleInternalPath(internalPath);
					}
				});
	}
	
	private void handleInternalPath(String internalPath) {
		WApplication app = WApplication.getInstance();

		for (Form f : forms)
			if (app.internalPathMatches(f.path))
				f.form.handleInternalPath(internalPath.substring(f.path.length()));
	}
	
	public void changeInternalPath(String path) {
		WApplication app = WApplication.getInstance();
		app.setInternalPath(path, true);
	}
	
	public WMenuItem addForm(CharSequence text, String url, AbstractForm widget) {
		forms.add(new Form(menu.getInternalBasePath() + url, widget));
		WMenuItem item = menu.addItem(text, widget);
		item.setPathComponent(url);
		return item;
	}
}
