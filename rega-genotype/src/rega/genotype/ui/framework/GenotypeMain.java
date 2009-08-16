package rega.genotype.ui.framework;

import net.sf.regadb.util.settings.RegaDBSettings;
import net.sf.witty.wt.Configuration;
import net.sf.witty.wt.WApplication;
import net.sf.witty.wt.WEnvironment;
import net.sf.witty.wt.WebController;

public class GenotypeMain extends WebController
{
	public GenotypeMain()
	{
		super(new Configuration());
	}

	@Override
	public WApplication createApplication(WEnvironment env)
	{
		GenotypeApplication app = new GenotypeApplication(env, this.getServletContext());
        
        RegaDBSettings.getInstance().initProxySettings();
        
        return app;
	}
	
	public static GenotypeApplication getApp()
	{
		return (GenotypeApplication)WApplication.instance();
	}
}