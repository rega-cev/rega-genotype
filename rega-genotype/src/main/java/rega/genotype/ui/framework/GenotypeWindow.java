/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.framework;

import java.util.HashMap;
import java.util.Map;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.JobForm;
import rega.genotype.ui.forms.StartForm;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WStackedWidget;
import eu.webtoolkit.jwt.WWidget;

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
	private Map<String, WWidget> widgets = new HashMap<String, WWidget>();
	
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
		
		addForm("/", new StartForm(this));
		
		JobForm jobForm = new JobForm(this, od.getJobOverview(this));
		addForm(JobForm.JOB_URL, jobForm);
		jobForm.handleInternalPath();
	}
	
	public void changeInternalPath(String path) {
		WApplication app = WApplication.getInstance();
		app.setInternalPath(path, true);
	}
	
	public void addForm(String url, WWidget widget) {
		content.addWidget(widget);
		widgets.put(url, widget);
	}
}
