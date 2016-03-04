/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.framework;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;

import rega.genotype.utils.Settings;
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
	private Settings settings;
	
	protected String toolId;// url Path Component
	
	public GenotypeApplication(WEnvironment env,
			ServletContext servletContext, Settings settings ,String toolId)
	{
		super(env);
		this.settings = settings;

		useStyleSheet(new WLink("../style/wt.css"));               // do not use Wt's inline stylesheet...
		useStyleSheet(new WLink("../style/wt_ie.css"), "IE lt 7"); // do not use Wt's inline stylesheet...

		servletContext_ = servletContext;
		this.toolId = toolId;
	}

	public ServletContext getServletContext()
	{
		return servletContext_;
	}
	
	public Settings getSettings()
	{
		return settings;
	}

	public String getToolId() {
		return toolId;
	}

	public static GenotypeApplication getGenotypeApplication() {
		WApplication app = WApplication.getInstance();
		if (app != null && app instanceof GenotypeApplication)
			return (GenotypeApplication) getInstance();
		else
			return null;
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