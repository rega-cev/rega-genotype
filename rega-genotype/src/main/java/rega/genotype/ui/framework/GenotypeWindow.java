/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.framework;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.AbstractForm;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.DetailsForm;
import rega.genotype.ui.forms.DocumentationForm;
import rega.genotype.ui.forms.StartForm;
import rega.genotype.ui.i18n.resources.GenotypeResourceManager;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.ui.util.Settings;
import rega.genotype.ui.util.StateLink;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WString;
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
	private static final String JOB_URL = "/job";
	private static final String CONTACT_URL = "/contact";

	private Map<String, AbstractForm> forms = new HashMap<String, AbstractForm>();
	
	private AbstractForm activeForm;
		
	private WContainerWidget content;
	
	private WImage header;
	private WText footer;
	
	private OrganismDefinition od;
	
	public OrganismDefinition getOrganismDefinition() {
		return od;
	}

	private GenotypeResourceManager resourceManager;
	
	private StartForm startForm;
	private StateLink monitor;
	private AbstractJobOverview monitorForm;
	private DetailsForm detailsForm;

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
		
		app.useStyleSheet("../style/genotype.css");

		header = GenotypeLib.getWImageFromResource(od, "header.gif", this);
		header.setAlternateText("header");
		header.setId("");
		header.setStyleClass("header");

		content = new WContainerWidget(this);
		content.setId("content");
		content.setStyleClass("content");

		WContainerWidget navigationContainer = new WContainerWidget(this);
		navigationContainer.setId("");
		navigationContainer.setStyleClass("navigation");

		WContainerWidget navigation = new WContainerWidget(navigationContainer);
		navigation.setId("");

		footer = new WText(resourceManager.getOrganismValue("main-form", "footer"), this);
		footer.setId("");
		footer.setStyleClass("footer");

		/*
		 * Set up the footer: links to all the forms:
		 */
		addLink(navigation, tr("main.navigation.start"), START_URL, startForm = new StartForm(this));

		monitor = new StateLink(tr("main.navigation.monitor"), JOB_URL, navigation);
		monitor.setId("monitor-link");

		try {
			DocumentationForm form = new DocumentationForm(this, "howToCite-form", "howToCite-text");
			addLink(navigation, tr("main.navigation.howToCite"), CITE_URL, form);
		} catch (Exception e) {
		}

		try {
			DocumentationForm form = new DocumentationForm(this, "introduction-form", "introduction-text");
			addLink(navigation, tr("main.navigation.introduction"), INTRODUCTION_URL, form);
		} catch (Exception e) {
		}

		try {
			DocumentationForm form = new DocumentationForm(this, "tutorial-form", "tutorial-text");
			addLink(navigation, tr("main.navigation.tutorial"), TUTORIAL_URL, form);
		} catch (Exception e) {
		}

		try {
			DocumentationForm form = new DocumentationForm(this, "decisionTrees-form", "decisionTrees-text");
			addLink(navigation, tr("main.navigation.decisionTrees"), DECISIONTREES_URL, form);
		} catch (Exception e) {
		}

		try {
			DocumentationForm form = new DocumentationForm(this, "subtypingProcess-form", "subtypingProcess-text");
			addLink(navigation, tr("main.navigation.subtypingProcess"), METHOD_URL, form);
		} catch (Exception e) {
		}

		try {
			DocumentationForm form = new DocumentationForm(this, "exampleSequences-form", "exampleSequences-sequences");
			addLink(navigation, tr("main.navigation.exampleSequences"), EXAMPLES_URL, form);
		} catch (Exception e) {
		}

		try {
			DocumentationForm form = new DocumentationForm(this, "contactUs-form", "contactUs-text");
			addLink(navigation, tr("main.navigation.contactUs"), CONTACT_URL, form);
		} catch (Exception e) {
		}

		GenotypeMain.getApp().internalPathChanged().addListener(this, new Signal1.Listener<String>() {

			public void trigger(String basePath) {
				if (basePath.equals("/")) {
					String newPath = "/" + GenotypeMain.getApp().getInternalPathNextPart(basePath);

					AbstractForm f = forms.get(newPath);

					if (f != null)
						setForm(f);
				} else if (basePath.equals("/job/")) {
					String jobId = GenotypeMain.getApp().getInternalPathNextPart(basePath);
					if (existsJob(jobId)) {
						File jobDir = getJobDir(jobId);
						if (monitorForm == null)
							monitorForm = od.getJobOverview(GenotypeWindow.this);
						monitorForm.init(jobDir, jobId);
						monitor.setVarValue(jobId(jobDir));
					} else
						setForm(startForm);
				}
			} });

		startForm.init();
		setForm(startForm);
	}

	protected boolean existsJob(String jobId) {
		return getJobDir(jobId).exists();
	}

	private void addLink(WContainerWidget parent, WString text, String url, AbstractForm form) {
		WAnchor a = new WAnchor("", text, parent);
		a.setId("");
		a.setRefInternalPath(url);
		a.setStyleClass("link");
		forms.put(url, form);
	}

	public void setForm(AbstractForm form) {
		form.show();

		if (form == activeForm)
			return;

		if(activeForm!=null)
			activeForm.hide();
		activeForm = form;

		if (form.getParent() == null)
			content.addWidget(form);
	}
	
	public void monitorForm(File jobDir, boolean setUrl) {
		if (monitorForm==null)
			monitorForm = od.getJobOverview(this);
		monitorForm.init(jobDir, jobId(jobDir));
		monitor.setVarValue(jobId(jobDir));
		
		if (setUrl) {
			WApplication app = WApplication.getInstance();
			app.setInternalPath(monitor.internalPath());
			if (!app.getEnvironment().hasAjax())
				app.redirect(app.getBookmarkUrl(monitor.internalPath()));
		}

		setForm(monitorForm);
	}

	public void detailsForm(File jobDir, int selectedSequenceIndex) {
		if (detailsForm==null)
			detailsForm = new DetailsForm(this);

		monitor.setVarValue(jobId(jobDir));
		detailsForm.init(jobDir, selectedSequenceIndex);

		setForm(detailsForm);
	}

	public File getJobDir(String jobId) {
		return new File(Settings.getInstance().getJobDir(getOrganismDefinition()).getAbsolutePath()+File.separatorChar+jobId);
	}

	public static String reportPath(File jobDir, int sequenceIndex) {
		return jobPath(jobDir) + '/' + String.valueOf(sequenceIndex);
	}

	public static String jobPath(File jobDir) {
		return JOB_URL + '/' + jobId(jobDir);
	}
	
	public static String jobId(File jobDir) {
		return jobDir.getAbsolutePath().substring(jobDir.getAbsolutePath().lastIndexOf(File.separatorChar)+1);
	}	
}
