package rega.genotype.ui.framework;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;

import net.sf.witty.wt.WApplication;
import net.sf.witty.wt.WEnvironment;

public class GenotypeApplication extends WApplication
{
	private ServletContext servletContext_;
	private GenotypeWindow window_;
	
	public GenotypeApplication(WEnvironment env, ServletContext servletContext)
	{
		super(env);
		
		servletContext_ = servletContext;
		window_ = new GenotypeWindow();
		window_.init();
		root().addWidget(window_);
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