/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.framework;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.ui.util.Settings;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;

/**
 * The application.
 * OrganismDefinition determines which kind of virus the application will be used for.
 * 
 * @author simbre1
 *
 */
public class GenotypeApplication extends WApplication
{
	private ServletContext servletContext_;
	private GenotypeWindow window_;
	
	public GenotypeApplication(WEnvironment env, ServletContext servletContext, OrganismDefinition od)
	{
		super(env);

		useStyleSheet("../style/wt.css");               // do not use Wt's inline stylesheet...
		useStyleSheet("../style/wt_ie.css", "IE lt 7"); // do not use Wt's inline stylesheet...

		servletContext_ = servletContext;
		window_ = new GenotypeWindow(od);
		window_.init();
		getRoot().addWidget(window_);
	}

	public GenotypeWindow getWindow()
	{
		return window_;
	}

	public ServletContext getServletContext()
	{
		return servletContext_;
	}
	
	/*
	 * This function creates a temporary file
	 * If something goes wrong during this process
	 * a null reference is returned
	 * */
	public File createTempFile(String prefix, String postfix)
	{
		File directory = (File)getServletContext().getAttribute("javax.servlet.context.tmpdir");
		File file = null;
		try
		{
			file = File.createTempFile(prefix, postfix, directory);
		}
		catch (IOException e)
		{
			
		}
		
		return file;
	}
}