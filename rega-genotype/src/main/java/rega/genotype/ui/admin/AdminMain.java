package rega.genotype.ui.admin;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import rega.genotype.utils.Settings;
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
		
		super.init(config);
	}

	public static AdminMain getAdminMainInstance() {
		return (AdminMain) getInstance();
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		super.doPost(req, resp);
	}
}
