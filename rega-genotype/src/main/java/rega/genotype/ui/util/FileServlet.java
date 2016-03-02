package rega.genotype.ui.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import rega.genotype.utils.Settings;

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
		// TODO: this can be security problem. check that user can not get below the
		// Organism dir.
		return SERVLET_PATH + "?id=" + id;
	}

	/*
	 * Return the file path of the given id. The id should contains the
	 * extension of the file.
	 * expected id = {url path component}/path in organism dir
	 * return "" if id syntax is not correct.
	 */
	public String getFilePath(String id) {
		Pattern p = Pattern.compile("\\{(?<url>[^\\}]+)\\}(?<interanl>.+)");
		Matcher m = p.matcher(id);
		if (!m.matches())
			return "";

		String url = m.group("url");
		String internalPath = m.group("interanl");

		File xmlPath = Settings.getInstance(getServletContext()).getXmlPath(url);
		
		return xmlPath + internalPath;
	}
}
