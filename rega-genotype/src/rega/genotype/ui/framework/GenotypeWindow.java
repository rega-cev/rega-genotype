package rega.genotype.ui.framework;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import net.sf.witty.wt.SignalListener;
import net.sf.witty.wt.WApplication;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WImage;
import net.sf.witty.wt.WMouseEvent;
import net.sf.witty.wt.WResource;
import net.sf.witty.wt.WTable;
import net.sf.witty.wt.WText;

import org.apache.commons.io.IOUtils;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.DetailsForm;
import rega.genotype.ui.forms.HowToCiteForm;
import rega.genotype.ui.forms.IForm;
import rega.genotype.ui.forms.StartForm;
import rega.genotype.ui.forms.TutorialForm;
import rega.genotype.ui.i18n.resources.GenotypeResourceManager;
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
	private WText subtypingProcess;
	private WText exampleSequences;
	private WText contactUs;
	
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
		
		//TODO make utility function to make this kind of image (also used in defaultseqassignmentform+tutorialform)
		this.header = new WImage(new WResource() {
            @Override
            public String resourceMimeType() {
                return "image/gif";
            }
            @Override
            protected void streamResourceData(OutputStream stream) {
                try {
                    IOUtils.copy(this.getClass().getClassLoader().getResourceAsStream(od.getOrganismDirectory()+"header.gif"), stream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, table.elementAt(0, 0));

		WContainerWidget navigation = new WContainerWidget(table.elementAt(2, 0));
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
		subtypingProcess = new WText(tr("main.navigation.subtypingProcess"), navigation);
		subtypingProcess.setStyleClass("link");
		exampleSequences = new WText(tr("main.navigation.exampleSequences"), navigation);
		exampleSequences.setStyleClass("link");
		contactUs = new WText(tr("main.navigation.contactUs"), navigation);
		contactUs.setStyleClass("link");
		
		this.footer = new WText(resourceManager.getOrganismValue("main-form", "footer"), table.elementAt(3, 0));
		
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
