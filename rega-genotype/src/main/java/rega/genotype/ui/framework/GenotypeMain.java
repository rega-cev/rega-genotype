package rega.genotype.ui.framework;

import eu.webtoolkit.jwt.Configuration;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WebController;

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