package rega.genotype.ui.framework;

import net.sf.witty.wt.WApplication;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WTable;
import net.sf.witty.wt.i8n.WStdMessageResource;
import rega.genotype.ui.framework.forms.HIVForm;

public class GenotypeWindow extends WContainerWidget
{
	private Header header;
	private Footer footer;
	
	private WTable table;
	private Navigation navigation;
	private WContainerWidget activeForm;
	
	public GenotypeWindow()
	{
		super();
	}
	
	private void loadI18nResources()
	{
		WApplication.instance().messageResourceBundle().useResource(new WStdMessageResource("rega.genotype.ui.i18n.resources.genotype"));
	}

	public void init() {
		loadI18nResources();

		setStyleClass("root");
		WApplication.instance().useStyleSheet("style/regadb.css");
		WApplication.instance().useStyleSheet("style/calendar.css");
		WApplication.instance().useStyleSheet("style/querytool.css");

		header = new Header(this);
		table = new WTable(this);
		navigation = new Navigation(table.elementAt(0, 0));
		activeForm = new HIVForm(table.elementAt(0,1));
		footer = new Footer(this);
	}
}
