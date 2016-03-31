package rega.genotype.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import rega.genotype.config.ToolIndexes;
import rega.genotype.config.ToolIndexes.ToolIndex;
import rega.genotype.config.ToolManifest;
import rega.genotype.utils.FileUtil;
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
	
	// responce 
	public static String RESPONCE_ERRORS = "errors";

	// local vars.
	private String repoDir = "repo-work-dir" + File.separator;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String reqType = getLastUrlPathComponent(req.getRequestURL().toString());
		String pwd = req.getHeader(TOOL_PWD_PARAM);
		if (reqType.equals(REQ_TYPE_PUBLISH)) {

			// copy to tmp dir 
			File tmpFile = File.createTempFile("tool", ".zip");
			FileOutputStream out = new FileOutputStream(tmpFile);
			ServletInputStream bodyStream = req.getInputStream();
			IOUtils.copy(bodyStream, out);
			bodyStream.close();
	        out.close();

	        StringBuilder errors = new StringBuilder();
	        if (!addTool(pwd, tmpFile, errors)) {
	        	resp.setHeader(RESPONCE_ERRORS, errors.toString());
	        	resp.setStatus(404);
				return;
	        }
		} else {
    		resp.setStatus(404);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String reqType = getLastUrlPathComponent(req.getRequestURL().toString());
		if (reqType.equals(REQ_TYPE_GET_MANIFESTS)) {			
			String json = storeJsonObjectsInArray(getManifests());
			resp.setContentType("application/json");
			
			resp.getWriter().print(json);
			resp.getWriter().close();		
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

	@Override
	public void init(ServletConfig config) throws ServletException {
		repoDir = config.getServletContext().getInitParameter("baseRepoDir");
		super.init(config);
	}

	private File getToolFile(String toolId, String toolVersion) {
		return new File(repoDir + toolId + toolVersion + ".zip");
	}

	private boolean addTool(String password, File toolFile, StringBuilder errors) {
		if (toolFile == null || !FileUtil.isValidZip(toolFile)) {
			errors.append("Invalid tool file");
			return false;
		}

        // extract tool id and version
		ToolManifest manifest = ToolManifest.parseJson(
				FileUtil.getFileContent(toolFile, ToolManifest.MANIFEST_FILE_NAME));
		
		File toolDir = getToolFile(manifest.getId(), manifest.getVersion());
		if (toolDir.exists()) {
			errors.append("Tool already exists in repository.");
			return false;
		}
		
		File toolIndexsFile = new File(repoDir + ToolIndexes.TOOL_INDEXS_FILE_NAME);
		ToolIndexes indexes;
		if (toolIndexsFile.exists()) {
			indexes = ToolIndexes.parseJsonAsList(FileUtil.readFile(toolIndexsFile));
			// Check publisher pwd
			ToolIndex index = indexes.getIndex(manifest.getId());
			if (index != null && !index.getPublisherPassword().equals(password)){
				errors.append("Only the tool original publisher " + index.getPublisherName() +" can publish new versions.");
				return false;
			}
		} else {
			indexes = new ToolIndexes();
		}
		// Add ToolIndex for new tools
		indexes.getIndexes().add(new ToolIndex(
				password, manifest.getId(), manifest.getPublisherName(),
				repoDir + manifest.getId() + manifest.getVersion() + ".zip"));
		try {
			indexes.save(repoDir);
		} catch (IOException e1) {
			e1.printStackTrace();
			errors.append("Server internal error.");
			return false;
		}
		
		
		toolDir.getParentFile().mkdirs();
		try {
			Files.copy(toolFile.toPath(), toolDir.toPath());
		} catch (IOException e) {
			errors.append("Server internal error.");
			e.printStackTrace();
			return false;
		}

		
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
		File repoDirFile = new File(repoDir);
		repoDirFile.mkdirs();
		if (repoDirFile.listFiles() != null) {
			for (File f: repoDirFile.listFiles()){
				if (FilenameUtils.getExtension(f.getAbsolutePath()).equals("zip")
						&& FileUtil.isValidZip(f)){
					String json = FileUtil.getFileContent(
							f, ToolManifest.MANIFEST_FILE_NAME);
					if (json != null)
						ans.add(json);
				}
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
}
