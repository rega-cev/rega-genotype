package rega.genotype.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import eu.webtoolkit.jwt.utils.StreamUtils;

@SuppressWarnings("serial")
public class ToolRepoService extends HttpServlet{
	// params
	public static String REQ_TYPE_PARAM = "req-type";
	public static String TOOL_PWD_PARAM = "tool-pwd";

	public static String TOOL_ID_PARAM = "tool-id";
	public static String TOOL_VERSION_PARAM = "tool-version";

	// req types
	public static String REQ_TYPE_PUBLISH  = "publish";
	public static String REQ_TYPE_GET_MANIFESTS = "get-manifests";
	public static String REQ_TYPE_GET_TOOL = "get-tool";

	private static String REPO_DIR = "repo-work-dir" + File.separator; // TODO 
	private static String REPO_DIR_TMP = REPO_DIR + "tmp" + File.separator;


	// TODO: store in file?
	//      <password, tool id>
	private Map<String, String> toolPasswords = new HashMap<String, String>();
	//private List<ToolManifest> manifests = new ArrayList<ToolManifest>();

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		performRequest(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doGet(req, resp);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		// TODO Auto-generated method stub
		super.init(config);
	}

	private void performRequest(HttpServletRequest req, HttpServletResponse resp)  throws ServletException, IOException {		
		String reqType = getLastUrlPathComponent(req.getRequestURL().toString());
		String pwd = req.getHeader(TOOL_PWD_PARAM);//TODO
		if (reqType.equals(REQ_TYPE_PUBLISH)) {
			
			// copy to tmp dir 
			File tmpFile = File.createTempFile("tool", ".zip");
			FileOutputStream out = new FileOutputStream(tmpFile);
			ServletInputStream bodyStream = req.getInputStream();
			IOUtils.copy(bodyStream, out);
			bodyStream.close();
	        out.close();
     
	        if (!addTool(pwd, tmpFile)) {
	        	resp.setStatus(404);
				return;
	        }
		} else if (reqType.equals(REQ_TYPE_GET_MANIFESTS)) {			
			String json = storeJsonObjectsInArray(getManifests());
			resp.setContentType("application/json");
			StreamUtils.copy(new ByteArrayInputStream(
					json.getBytes(StandardCharsets.UTF_8)), resp.getOutputStream());
			resp.getOutputStream().flush();			
		} else if (reqType.equals(REQ_TYPE_GET_TOOL)) {
			String id = req.getParameter(TOOL_ID_PARAM);
			String version = req.getParameter(TOOL_VERSION_PARAM);
			File toolFile = getToolFile(id, version);
			if (toolFile.exists()) {
				StreamUtils.copy(new FileInputStream(toolFile), resp.getOutputStream());
				resp.getOutputStream().flush();	
			} else
				resp.setStatus(404);
			
		} else {
    		resp.setStatus(404);
		}
	}

	private File getToolFile(String toolId, String toolVersion) {
		return new File(REPO_DIR + toolId + toolVersion + ".zip");
	}
	
	private boolean addTool(String password, File toolFile) {
		if (toolFile == null || !FileUtil.isValidZip(toolFile))
			return false;

        // extract tool id and version
		ToolManifest manifest = ToolManifest.parseJson(
				FileUtil.getFileContent(toolFile, ToolManifest.MANIFEST_FILE_NAME));
		
		File toolDir = getToolFile(manifest.getId(), manifest.getVersion());
		toolDir.getParentFile().mkdirs();

		try {
			Files.copy(toolFile.toPath(), toolDir.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		// TODO store pwd and manifest.
		
		return true;
	}

	// utils
	
	private String storeJsonObjectsInArray(List<String> jsonObjects) {
		String ans = "[";
		for (int i = 0; i < jsonObjects.size(); ++i) {
			if (i != 0)
				ans += "\n ,";
			ans += jsonObjects.get(i);
		}

		ans += "]";
		return ans;
	}

	private List<String> getManifests() {
		List<String> ans = new ArrayList<String>();
		File repoDir = new File(REPO_DIR);
		for (File f: repoDir.listFiles()){
			if (FileUtil.isValidZip(f)){
				String json = FileUtil.getFileContent(
						f, ToolManifest.MANIFEST_FILE_NAME);
				if (json != null)
					ans.add(json);
			}
		}

		return ans;
	}

	private String getLastUrlPathComponent(String url) {
		try {
			String[] split = new URL(url).getPath().split("/");
			if (split.length > 0)
				return split[split.length - 1];
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return null;
	}

	// URLS
	
	private static String gerRepoServiceUrl() {
		//TODO
		return "http://localhost:8080/rega-genotype/repo-service";
	}

	public static String getReqPublishUrl() {
		return ToolRepoService.gerRepoServiceUrl() 
				+ File.separator  +  ToolRepoService.REQ_TYPE_PUBLISH;
	}

	public static String getReqManifestsUrl() {
		return ToolRepoService.gerRepoServiceUrl() 
				+ File.separator  +  ToolRepoService.REQ_TYPE_GET_MANIFESTS;
	}

	public static String getReqToolUrl(String toolId, String toolVersion) {
		return ToolRepoService.gerRepoServiceUrl() 
				+ File.separator  +  ToolRepoService.REQ_TYPE_GET_TOOL 
				+ "?" + TOOL_ID_PARAM + "=" + toolId
				+ "&" + TOOL_VERSION_PARAM + "=" + toolVersion;
	}
}
