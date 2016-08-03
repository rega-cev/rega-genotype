/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.framework;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;

import rega.genotype.config.Config.ToolConfig;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.framework.exeptions.RegaGenotypeExeption;
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
	
	private ToolConfig toolConfig;
	
	public GenotypeApplication(WEnvironment env,
			ServletContext servletContext, Settings settings ,String urlPath) throws RegaGenotypeExeption
	{
		super(env);
		this.toolConfig = Settings.getInstance().getConfig().getToolConfigByUrlPath(urlPath);
		
		if (settings.getConfig() == null)
			throw new RegaGenotypeExeption("Missing config file. Go to {host}/rega-genotype/admin/global to create new config file.");

		this.settings = settings;

		servletContext_ = servletContext;

		enableUpdates(); // ParserDispatcher uses triggerUpdate. 
	}
	
	public ServletContext getServletContext()
	{
		return servletContext_;
	}
	
	public Settings getSettings()
	{
		return settings;
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

	public ToolConfig getToolConfig() {
		return toolConfig;
	}
}