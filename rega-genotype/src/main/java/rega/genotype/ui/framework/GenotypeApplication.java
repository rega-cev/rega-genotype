/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.framework;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;

import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WLink;

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
	
	public GenotypeApplication(WEnvironment env, ServletContext servletContext)
	{
		super(env);

		useStyleSheet(new WLink("../style/wt.css"));               // do not use Wt's inline stylesheet...
		useStyleSheet(new WLink("../style/wt_ie.css"), "IE lt 7"); // do not use Wt's inline stylesheet...

		servletContext_ = servletContext;
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
		try	{
			file = File.createTempFile(prefix, postfix, directory);
		} catch (IOException e)	{
			e.printStackTrace();
		}
		
		return file;
	}
}