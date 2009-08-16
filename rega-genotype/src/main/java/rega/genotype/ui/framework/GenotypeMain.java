/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.framework;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.ui.util.Settings;
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
	public GenotypeMain() {
		super();
		
		getConfiguration().setSendXHTMLMimeType(true);
	}
	
	public static GenotypeApplication getApp() {
		return (GenotypeApplication)WApplication.getInstance();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		GenotypeLib.initSettings(Settings.getInstance());
		
		super.init(config);
	}

}