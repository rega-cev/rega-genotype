package rega.genotype.ui.framework;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

import net.sf.witty.wt.WApplication;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WImage;
import net.sf.witty.wt.WResource;
import net.sf.witty.wt.WTable;
import net.sf.witty.wt.WText;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.StartForm;
import rega.genotype.ui.i18n.resources.GenotypeResourceManager;

public class GenotypeWindow extends WContainerWidget
{
	private WTable table;
	private WContainerWidget activeForm;
	
	private WImage header;
	private WText footer;
	
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
		resourceManager = new GenotypeResourceManager("/rega/genotype/ui/i18n/resources/common_resources.xml", od.getOrganismDirectory()+"resources.xml");
		WApplication.instance().messageResourceBundle().useResource(resourceManager);
	}

	public void init() {
		loadI18nResources();

		setStyleClass("root");
		WApplication.instance().useStyleSheet("style/genotype.css");

		table = new WTable(this);
		
		this.header = new WImage(new WResource() {
            @Override
            public String resourceMimeType() {
                return "image/png";
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

		setForm(new StartForm(od, null, this));
		
		this.footer = new WText(resourceManager.getOrganismValue("main-form", "footer"), table.elementAt(2, 0));
	}
	
	public void setForm(WContainerWidget form) {
		if(activeForm!=null)
			activeForm.setParent(null);
		activeForm = form;
		table.putElementAt(1,0, form);
	}
}
