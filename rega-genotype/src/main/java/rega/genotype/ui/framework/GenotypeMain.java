/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.framework;

import eu.webtoolkit.jwt.Configuration;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WebController;

/**
 * The entry point of the application.
 * 
 * @author simbre1
 *
 */
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