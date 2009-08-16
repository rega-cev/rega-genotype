package rega.genotype.ui.framework;

import net.sf.witty.wt.Configuration;
import net.sf.witty.wt.WApplication;
import net.sf.witty.wt.WEnvironment;
import net.sf.witty.wt.WebController;

public abstract class GenotypeMain extends WebController
{
	public GenotypeMain()
	{
		super(new Configuration());
	}

	public abstract WApplication createApplication(WEnvironment env);
	
	public static GenotypeApplication getApp()
	{
		return (GenotypeApplication)WApplication.instance();
	}
}