package rega.genotype.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import rega.genotype.config.ToolManifest;
import rega.genotype.ui.util.FileUtil;

@SuppressWarnings("serial")
public class ToolRepoService extends HttpServlet{
	// params
	public static String REQ_TYPE_PARAM = "req-type";
	public static String TOOL_PWD_PARAM = "tool-pwd";
	
	// req types
	public static String REQ_TYPE_PUBLISH  = "publish";
	public static String REQ_TYPE_GET_MANIFESTS = "get-manifests";
	public static String REQ_TYPE_GET_TOOL = "get-tool";

	private static String REPO_DIR = "repo-work-dir" + File.separator; // TODO 
	private static String REPO_DIR_TMP = REPO_DIR + "tmp" + File.separator;


	// TODO: store in file?
	//      <password, tool id>
	private Map<String, String> toolPasswords = new HashMap<String, String>();
	private List<ToolManifest> manifests = new ArrayList<ToolManifest>();

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		performRequest(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		performRequest(req, resp);
	}

	private void performRequest(HttpServletRequest req, HttpServletResponse resp)  throws ServletException, IOException {
		String reqType = req.getParameter(REQ_TYPE_PARAM);
		if (reqType.equals(REQ_TYPE_PUBLISH)) {
			String pwd = req.getParameter(TOOL_PWD_PARAM);
			
            String tmp = "" + (int)(Math.random()*10000000);
			File workDir = File.createTempFile(REPO_DIR_TMP + tmp , "zip");
			FileOutputStream out = new FileOutputStream(workDir);
			ServletInputStream bodyStream = req.getInputStream();
			IOUtils.copy(bodyStream, out);
			
			bodyStream.close();
	        out.close();

		} else if (reqType.equals(REQ_TYPE_GET_MANIFESTS)) {
			resp.setStatus(404);//TODO
		} else if (reqType.equals(REQ_TYPE_GET_TOOL)) {
			resp.setStatus(404);//TODO
		} else {
    		resp.setStatus(404);
		}
	}

	private boolean addTool(String password, File toolFile) {
		if (toolFile == null || !FileUtil.isValidZip(toolFile))
			return false;

		File manifestFile = new File(toolFile.getAbsolutePath() 
				+ ToolManifest.MANIFEST_FILE_NAME);
		ToolManifest manifest = ToolManifest.parseJson(FileUtil.readFile(new File(
				toolFile.getAbsolutePath() + ToolManifest.MANIFEST_FILE_NAME)));
		File toolDir = new File(REPO_DIR + manifest.getId() + manifest.getVersion());
		toolDir.mkdir();

		FileUtil.storeFile(toolFile, toolDir.getAbsolutePath());
		// TODO store pwd and manifest.
		
		return false; // TODO
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		// TODO Auto-generated method stub
		super.init(config);
	}

	public static String gerRepoServiceUrl() {
		//TODO
		return "http://localhost:8080/rega-genotype/repo-service";
	}
}
