package rega.genotype.ui.framework;

import java.io.File;

import net.sf.witty.wt.SignalListener;
import net.sf.witty.wt.WApplication;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WImage;
import net.sf.witty.wt.WMouseEvent;
import net.sf.witty.wt.WTable;
import net.sf.witty.wt.WText;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.ContactUsForm;
import rega.genotype.ui.forms.DecisionTreesForm;
import rega.genotype.ui.forms.DetailsForm;
import rega.genotype.ui.forms.ExampleSequencesForm;
import rega.genotype.ui.forms.HowToCiteForm;
import rega.genotype.ui.forms.IForm;
import rega.genotype.ui.forms.StartForm;
import rega.genotype.ui.forms.SubtypingProcessForm;
import rega.genotype.ui.forms.TutorialForm;
import rega.genotype.ui.i18n.resources.GenotypeResourceManager;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.ui.util.Settings;
import rega.genotype.ui.util.StateLink;

public class GenotypeWindow extends WContainerWidget
{
	private WTable table;
	private IForm activeForm;
	
	private WImage header;
	private WText footer;
	
	private OrganismDefinition od;
	
	public OrganismDefinition getOrganismDefinition() {
		return od;
	}

	private GenotypeResourceManager resourceManager;
	
	private WText start;
	private StartForm startForm;
	private StateLink monitor;
	private AbstractJobOverview monitorForm;
	private DetailsForm detailsForm;
	private WText howToCite;
	private HowToCiteForm howToCiteForm;
	private WText tutorial;
	private TutorialForm tutorialForm;
	private WText decisionTrees;
	private DecisionTreesForm decisionTreesForm;
	private WText subtypingProcess;
	private SubtypingProcessForm subtypingProcessForm;
	private WText exampleSequences;
	private ExampleSequencesForm exampleSequencesForm;
	private WText contactUs;
	private ContactUsForm contactUsForm;
	
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
		WApplication.instance().messageResourceBundle().useResource(resourceManager);
	}

	public void init() {
		loadI18nResources();

		setStyleClass("root");
		WApplication.instance().useStyleSheet("style/genotype.css");

		table = new WTable(this);
		table.setStyleClass("window");
		
		this.header = GenotypeLib.getWImageFromResource(od, "header.gif", table.elementAt(0, 0));

		WContainerWidget navigation = new WContainerWidget(table.elementAt(2, 0));
		navigation.setStyleClass("navigation");
		
		start = new WText(tr("main.navigation.start"), navigation);
		start.clicked.addListener(new SignalListener<WMouseEvent>(){
			public void notify(WMouseEvent a) {
				startForm();
			}
		});
		start.setStyleClass("link");
		monitor = new StateLink("main.navigation.monitor", navigation, "${jobId}"){
			public void clickAction(String value) {
				monitorForm(new File(Settings.getInstance().getJobDir().getAbsolutePath()+File.separatorChar+value));
			}
		};
		howToCite = new WText(tr("main.navigation.howToCite"), navigation);
		howToCite.setStyleClass("link");
		howToCite.clicked.addListener(new SignalListener<WMouseEvent>(){
			public void notify(WMouseEvent a) {
				if(howToCiteForm==null)
					howToCiteForm = new HowToCiteForm(GenotypeWindow.this);
				setForm(howToCiteForm);
			}
		});
		tutorial = new WText(tr("main.navigation.tutorial"), navigation);
		tutorial.setStyleClass("link");
		tutorial.clicked.addListener(new SignalListener<WMouseEvent>(){
			public void notify(WMouseEvent a) {
				if(tutorialForm==null)
					tutorialForm = new TutorialForm(GenotypeWindow.this);
				setForm(tutorialForm);
			}
		});
		decisionTrees = new WText(tr("main.navigation.decisionTrees"), navigation);
		decisionTrees.setStyleClass("link");
		decisionTrees.clicked.addListener(new SignalListener<WMouseEvent>(){
			public void notify(WMouseEvent a) {
				if(decisionTreesForm==null)
					decisionTreesForm = new DecisionTreesForm(GenotypeWindow.this);
				setForm(decisionTreesForm);
			}
		});
		subtypingProcess = new WText(tr("main.navigation.subtypingProcess"), navigation);
		subtypingProcess.setStyleClass("link");
		subtypingProcess.clicked.addListener(new SignalListener<WMouseEvent>() {
			public void notify(WMouseEvent a) {
				if(subtypingProcessForm==null)
					subtypingProcessForm = new SubtypingProcessForm(GenotypeWindow.this);
				setForm(subtypingProcessForm);
			}
		});
		exampleSequences = new WText(tr("main.navigation.exampleSequences"), navigation);
		exampleSequences.setStyleClass("link");
		exampleSequences.clicked.addListener(new SignalListener<WMouseEvent>() {
			public void notify(WMouseEvent a) {
				if(exampleSequencesForm==null)
					exampleSequencesForm = new ExampleSequencesForm(GenotypeWindow.this);
				setForm(exampleSequencesForm);
			}
		});
		contactUs = new WText(tr("main.navigation.contactUs"), navigation);
		contactUs.setStyleClass("link");
		contactUs.clicked.addListener(new SignalListener<WMouseEvent>() {
			public void notify(WMouseEvent a) {
				if(contactUsForm==null) 
					contactUsForm = new ContactUsForm(GenotypeWindow.this);
				setForm(contactUsForm);
			}
		});
		
		this.footer = new WText(resourceManager.getOrganismValue("main-form", "footer"), table.elementAt(3, 0));
		footer.setStyleClass("footer");
		
		startForm();
	}
	
	private void setForm(IForm form) {
		if(activeForm!=null)
			activeForm.setParent(null);
		activeForm = form;
		table.putElementAt(1,0, form);
	}
	
	public void startForm() {
		if(startForm==null)
			startForm = new StartForm(this);
		startForm.init();
		setForm(startForm);
	}
	
	public void monitorForm(File jobDir) {
		if(monitorForm==null)
			monitorForm = od.getJobOverview(this);
		monitorForm.init(jobDir);
		monitor.setVarValue(jobDir.getAbsolutePath().substring(jobDir.getAbsolutePath().lastIndexOf(File.separatorChar)+1));
		setForm(monitorForm);
	}
	
	public void detailsForm(File jobDir, int selectedSequenceIndex) {
		if(detailsForm==null)
			detailsForm = new DetailsForm(this);
		detailsForm.init(jobDir, selectedSequenceIndex);
		setForm(detailsForm);
	}
}
