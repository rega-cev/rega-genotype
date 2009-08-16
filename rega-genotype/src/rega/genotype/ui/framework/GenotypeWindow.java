package rega.genotype.ui.framework;

import net.sf.witty.wt.WApplication;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WTable;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.StartForm;
import rega.genotype.ui.i18n.resources.GenotypeResourceManager;

public class GenotypeWindow extends WContainerWidget
{
	private Header header;
	private Footer footer;
	
	private WTable table;
	private WContainerWidget activeForm;
	
	private OrganismDefinition od;
	
	private GenotypeResourceManager resourceManager;
	
	public GenotypeResourceManager getResourceManager() {
		return resourceManager;
	}

	public GenotypeWindow(OrganismDefinition od)
	{
		super();
		this.od = od;
	}
	
	private void loadI18nResources()
	{
		resourceManager = new GenotypeResourceManager("/rega/genotype/ui/i18n/resources/common_resources.xml", od.getResourcesFile());
		WApplication.instance().messageResourceBundle().useResource(resourceManager);
	}

	public void init() {
		loadI18nResources();

		setStyleClass("root");
		WApplication.instance().useStyleSheet("style/regadb.css");
		WApplication.instance().useStyleSheet("style/calendar.css");
		WApplication.instance().useStyleSheet("style/querytool.css");

		header = new Header(this);
		table = new WTable(this);
		activeForm = new StartForm(od, table.elementAt(0,1), this);
		footer = new Footer(this);
	}
}
