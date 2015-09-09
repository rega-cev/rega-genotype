/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.framework;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WtServlet;

/**
 * The entry point of the application.
 * 
 * @author simbre1
 *
 */
@SuppressWarnings("serial")
public abstract class GenotypeMain extends WtServlet
{
	protected Settings settings;

	public GenotypeMain() {
		super();
		
		// progressive bootstrap is broken because Wt is not aware of image URLs in documentation forms 
		// getConfiguration().setProgressiveBootstrap(true);
	}

	public static GenotypeApplication getApp() {
		return (GenotypeApplication)WApplication.getInstance();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		Settings.initSettings(this.settings = Settings.getInstance(config.getServletContext()));
		
		super.init(config);
	}	
}