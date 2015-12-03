package rega.genotype.ui.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import rega.genotype.ui.framework.GenotypeMain;

/**
 * Serve files on a url.
 * defined in web.xml
 * load-on-startup 2
 */
public class FileServlet extends HttpServlet {

	private static final long serialVersionUID = 71546732567L;
	private static String SERVLET_PATH;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String id = req.getParameter("id");
		String filename = getFilePath(id);

		File file = new File(filename);

		if (!file.exists()) {
			resp.setStatus(404);
			return;
		}

		resp.addHeader("Cache-Control", "max-age=2592000, private");

		// Stream file
		FileInputStream stream = new FileInputStream(file);
		byte b[] = new byte[stream.available()];
		stream.read(b);
		stream.close();
		resp.getOutputStream().write(b);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		SERVLET_PATH = config.getServletContext().getContextPath() + "/files";
	}

	public static String getFileUrl() {
		return getFileUrl("");
	}

	public static String getFileUrl(String id) {
		return SERVLET_PATH + "?id=" + id;
	}

	/*
	 * Return the file path of the given id. The id should contains the
	 * extension of the file
	 */
	public static String getFilePath(String id) {
		String xmlPath = ((GenotypeMain) GenotypeMain.
				getInstance()).getSettings().getXmlPath().getAbsolutePath();
		return xmlPath + id;
	}
}
