package rega.genotype.ui.admin;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import rega.genotype.config.ToolUpdateService;
import rega.genotype.singletons.Settings;
import rega.genotype.singletons.ToolEditingSynchronizer;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WtServlet;

/**
 * Servlet for admin area.
 * @author michael
 */
public class AdminMain extends WtServlet{
	private static final long serialVersionUID = 1L;

	@Override
	public WApplication createApplication(WEnvironment env) {
		return new AdminApplication(env);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		// init the settings
		Settings.getInstance(config.getServletContext()).getConfig();

		getConfiguration().setMaximumRequestSize(2000*1024*1024); // 2000 MB maximum file requests, should match servlet config
		
		ToolUpdateService.startAutoUpdateTask();

		new ToolEditingSynchronizer(); // init the singleton.

		super.init(config);
	}

	public static AdminMain getAdminMainInstance() {
		return (AdminMain) getInstance();
	}
}
