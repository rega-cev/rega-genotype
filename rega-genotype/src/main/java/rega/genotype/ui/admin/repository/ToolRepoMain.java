package rega.genotype.ui.admin.repository;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WtServlet;

/**
 * Servlet for a repository of all published Typingtools
 * @author michael
 */
public class ToolRepoMain extends WtServlet{
	private static final long serialVersionUID = 1L;
	
	@Override
	public WApplication createApplication(WEnvironment env) {
		return new ToolRepoApplication(env);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		// init the settings
		Settings.getInstance(config.getServletContext()).getConfig();

		getConfiguration().setMaximumRequestSize(2000*1024*1024); // 2000 MB maximum file requests, should match servlet config
		
		super.init(config);
	}
}
