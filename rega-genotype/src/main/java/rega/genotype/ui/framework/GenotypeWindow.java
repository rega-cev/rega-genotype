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
import rega.genotype.ui.forms.DocumentationForm;
import rega.genotype.ui.forms.JobForm;
import rega.genotype.ui.forms.StartForm;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WStackedWidget;

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
		
		addForm(START_URL, new StartForm(this));
		addForm("contact-us", new DocumentationForm(this, tr("contact-us-doc")));
		addForm("how-to-cite", new DocumentationForm(this, tr("how-to-cite-doc")));
		
		JobForm jobForm = new JobForm(this, od.getJobOverview(this));
		addForm(JobForm.JOB_URL, jobForm);
		
		handleInternalPath(app.getInternalPath());
		app.internalPathChanged().addListener(this,
				new Signal1.Listener<String>() {
					public void trigger(String internalPath) {
						handleInternalPath(internalPath);
					}
				});
	}
	
	private void handleInternalPath(String internalPath) {
		GenotypeApplication app = GenotypeMain.getApp();
		
		for (Form f : forms)
			if (app.internalPathMatches(f.path)) {
				content.setCurrentWidget(f.form);
				f.form.handleInternalPath(internalPath.substring(f.path.length()));
				f.form.resize(WLength.Auto, WLength.Auto);
			}
	}
	
	public void changeInternalPath(String path) {
		WApplication app = WApplication.getInstance();
		app.setInternalPath(path, true);
	}
	
	public void addForm(String url, AbstractForm widget) {
		content.addWidget(widget);
		forms.add(new Form("/" + url, widget));
	}
}
