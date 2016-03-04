package rega.genotype.ui.data;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Read json config (rega-genotype/rega-genotype/config)
 * 
 * @author michael
 *
 */
public class Config {
	private GeneralConfig generalConfig;
	private List<ToolConfig> tools = new ArrayList<Config.ToolConfig>();

	public static Config parseJson(String json) {
		GsonBuilder builder = new GsonBuilder();
		Gson gson = builder.create();
		return gson.fromJson(json, Config.class);
	}

	public String toJson() {
		GsonBuilder builder = new GsonBuilder();
		Gson gson = builder.create();
		return gson.toJson(this);
	}

	public GeneralConfig getGeneralConfig() {
		return generalConfig;
	}

	public void setGeneralConfig(GeneralConfig generalConfig) {
		this.generalConfig = generalConfig;
	}

	public ToolConfig getToolConfig(String toolId) {
		ToolConfig toolConfig = null;
		// find organism config
		for (ToolConfig c: getTools())
			if (c.getToolId().equals(toolId))
				toolConfig = c;

		return toolConfig;
	}

	public ToolConfig getToolConfigByUrlPath(String url) {
		ToolConfig toolConfig = null;
		// find organism config
		for (ToolConfig c: getTools())
			if (c.getPath().equals(url))
				toolConfig = c;

		return toolConfig;
	}

	public List<ToolConfig> getTools() {
		return tools;
	}

	public void setTools(List<ToolConfig> tools) {
		this.tools = tools;
	}

	// classes

	/**
	 * Con 
	 */
	public static class ToolsContainer {
		
	}
	public static class GeneralConfig {
		private String xmlBasePath; // used by not generic tool. will be removed. 
		private String paupCmd;
		private String clustalWCmd;
		private String blastPath;
		private String treePuzzleCmd;
		private String treeGraphCmd;
		private String epsToPdfCmd;
		private String imageMagickConvertCmd;
		private int maxAllowedSeqs;
		private String inkscapeCmd;
		
		public String getPaupCmd() {
			return paupCmd;
		}
		public void setPaupCmd(String paupCmd) {
			this.paupCmd = paupCmd;
		}
		public String getClustalWCmd() {
			return clustalWCmd;
		}
		public void setClustalWCmd(String clustalWCmd) {
			this.clustalWCmd = clustalWCmd;
		}
		public String getTreePuzzleCmd() {
			return treePuzzleCmd;
		}
		public void setTreePuzzleCmd(String treePuzzleCmd) {
			this.treePuzzleCmd = treePuzzleCmd;
		}
		public String getTreeGraphCmd() {
			return treeGraphCmd;
		}
		public void setTreeGraphCmd(String treeGraphCmd) {
			this.treeGraphCmd = treeGraphCmd;
		}
		public String getEpsToPdfCmd() {
			return epsToPdfCmd;
		}
		public void setEpsToPdfCmd(String epsToPdfCmd) {
			this.epsToPdfCmd = epsToPdfCmd;
		}
		public String getImageMagickConvertCmd() {
			return imageMagickConvertCmd;
		}
		public void setImageMagickConvertCmd(String imageMagickConvertCmd) {
			this.imageMagickConvertCmd = imageMagickConvertCmd;
		}
		public int getMaxAllowedSeqs() {
			return maxAllowedSeqs;
		}
		public void setMaxAllowedSeqs(int maxAllowedSeqs) {
			this.maxAllowedSeqs = maxAllowedSeqs;
		}
		public String getInkscapeCmd() {
			return inkscapeCmd;
		}
		public void setInkscapeCmd(String inkscapeCmd) {
			this.inkscapeCmd = inkscapeCmd;
		}
		public String getBlastPath() {
			return blastPath;
		}
		public void setBlastPath(String blastPath) {
			this.blastPath = blastPath;
		}
		public String getXmlBasePath() {
			return xmlBasePath;
		}
		public void setXmlBasePath(String xmlBasePath) {
			this.xmlBasePath = xmlBasePath;
		}
	}

	public static class ToolConfig {
		/* json example.
		{
	    name: "HAV Typing Tool",
	    path: "HAV",
	    version: "1.1",
	    configuration: "/home/michael/projects/rega-genotype-extenal/xml/HAV/",
			jobDir: "/home/michael/projects/rega-genotype-extenal/job/",
	    auto-update: "true"
	  	}
		 */
		private String name;
		private String toolId;
		private String path; // url path component
		private String configuration; // xmlPath
		private String jobDir;
		private boolean autoUpdate;

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getToolId() {
			return toolId;
		}
		public void setToolId(String toolId) {
			this.toolId = toolId;
		}
		public String getPath() {
			if (path != null)
				return path;
			else
				return toolId;
		}
		public void setPath(String path) {
			this.path = path;
		}
		public String getConfiguration() {
			return configuration;
		}
		public void setConfiguration(String configuration) {
			this.configuration = configuration;
		}
		public boolean isAutoUpdate() {
			return autoUpdate;
		}
		public void setAutoUpdate(boolean autoUpdate) {
			this.autoUpdate = autoUpdate;
		}
		public String getJobDir() {
			return jobDir;
		}
		public void setJobDir(String jobDir) {
			this.jobDir = jobDir;
		}
	}
}
