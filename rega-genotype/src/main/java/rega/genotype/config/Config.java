package rega.genotype.config;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.ApplicationException;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.GsonUtil;
import eu.webtoolkit.jwt.WApplication;

/**
 * Read json config
 * Contains general system (env) configuration and a list of specific tool 
 * configurations for this server. 
 * 
 * @author michael
 *
 */
public class Config {
	public static final String CONFIG_FILE_NAME = "config.json";

	private GeneralConfig generalConfig = new GeneralConfig();
	private List<ToolConfig> tools = new ArrayList<Config.ToolConfig>(); //TODO: use set 

	public Config(){}
	
	public static Config parseJson(String json) {
		return GsonUtil.parseJson(json, Config.class);
	}

	public String toJson() {
		return GsonUtil.toJson(this);
	}

	public Config copy() {
		return parseJson(toJson());
	}

	public void save() throws IOException {
		save(Settings.getInstance().getBaseDir() + File.separator);
	}

	private synchronized void save(String externalDir) throws IOException {
		if (getGeneralConfig().getPublisherPassword() == null 
				|| getGeneralConfig().getPublisherPassword().isEmpty()) 
			getGeneralConfig().setPublisherPassword(
					new BigInteger(130, new SecureRandom()).toString(32));

		FileUtil.writeStringToFile(new File(externalDir + CONFIG_FILE_NAME), toJson());
		Settings.getInstance().setConfig(this);
	}

	public GeneralConfig getGeneralConfig() {
		return generalConfig;
	}

	public void setGeneralConfig(GeneralConfig generalConfig) {
		this.generalConfig = generalConfig;
	}

	public ToolConfig getToolConfigById(String toolId, String version) {
		ToolConfig toolConfig = null;
		// find organism config
		for (ToolConfig c: getTools())
			if (c.getUniqueToolId() != null) {
				ToolManifest m = c.getToolMenifest();
				if (m.getId().equals(toolId) && m.getVersion().equals(version))
					toolConfig = c;
			}
		return toolConfig;
	}

	public ToolManifest getToolManifestById(String toolId, String version) {
		ToolConfig toolConfigById = getToolConfigById(toolId, version);
		if (toolConfigById != null)
			return toolConfigById.getToolMenifest();
		else
			return null;
	}
	
	public ToolConfig getLastPublishedToolConfig(String toolId) {
		ToolConfig ans = null;
		// find organism config
		for (ToolConfig c: getTools())
			if (c.getToolMenifest() != null) {
				ToolManifest m = c.getToolMenifest();
				if (m.getId().equals(toolId) 
						&& m.getPublicationDate() != null
						&& (ans == null 
							|| m.getPublicationDate().compareTo(
								ans.getToolMenifest().getPublicationDate()) > 0))
					ans = c;
			}
		return ans;
	}

	public String getToolId(String taxonomyId) {
		if (taxonomyId == null)
			return null;

		for (ToolConfig c :Settings.getInstance().getConfig().getTools()) {
			ToolManifest m = c.getToolMenifest();
			if (m.getTaxonomyId() != null && m.getTaxonomyId().equals(taxonomyId))
				return m.getId();
		}

		return null;
	}

	public ToolConfig getCurrentVersion(String toolId) {
		if (toolId == null)
			return null;

		for (ToolConfig c :Settings.getInstance().getConfig().getTools()) {
			ToolManifest m = c.getToolMenifest();
			if (c.isCurrentUsedVersion() && m != null 
					&& m.getId().equals(toolId))
				return c;
		}

		return getLastPublishedToolConfig(toolId);
	}

	public ToolConfig getBlastTool(String id, String version) {
		for (ToolConfig c: getTools()) {
			if (c.getToolMenifest() != null 
					&& c.getToolMenifest().isBlastTool()
					&& c.getToolMenifest().getId().equals(id)
					&& c.getToolMenifest().getVersion().equals(version))
				return c;
		}

		return null;
	}

	/**
	 * Query the diamond aa db file from ngs module.
	 * This should be used by the diamond step of ngs analysis. 
	 * @return the file or null if the file / module are not found.
	 */
	public File getDiamondBlastDb() {
		ToolConfig ngsModuleConfig = getCurrentVersion(NgsModule.NGS_MODULE_ID);
		if (ngsModuleConfig == null)
			return null;

		NgsModule ngsModule = NgsModule.read(ngsModuleConfig.getConfigurationFile());
		
		File ans = new File(ngsModuleConfig.getConfigurationFile(),
				ngsModule.getAADbFileName());
		return ans.exists() ? ans : null;
	}

	/**
	 * Query the dna database file of all viruses file from ngs module.
	 * This should be used by contigs assembly step.
	 * @return the file or null if the file / module are not found.
	 */
	public File getNcbiVirusesDb() {
		ToolConfig ngsModuleConfig = getCurrentVersion(NgsModule.NGS_MODULE_ID);
		if (ngsModuleConfig == null)
			return null;

		NgsModule ngsModule = NgsModule.read(ngsModuleConfig.getConfigurationFile());
		
		File ans = new File(ngsModuleConfig.getConfigurationFile(), 
				ngsModule.getNcbiVirusesFileName());
		return ans.exists() ? ans : null;
	}

	public NgsModule getNgsModule() {
		ToolConfig ngsModuleConfig = getCurrentVersion(NgsModule.NGS_MODULE_ID);
		if (ngsModuleConfig == null)
			return null;

		return NgsModule.read(ngsModuleConfig.getConfigurationFile());
	}

	public File getNgsModulePath() {
		ToolConfig ngsModuleConfig = getCurrentVersion(NgsModule.NGS_MODULE_ID);
		if (ngsModuleConfig == null)
			return null;

		return ngsModuleConfig.getConfigurationFile();
	}

	public File trimomaticPath() throws ApplicationException{
		File ngsModulePath = Settings.getInstance().getConfig().getNgsModulePath();
		if (ngsModulePath == null)
			throw new ApplicationException("NGS module is missing contact server admin.");

		return NgsModule.trimmomaticPath(ngsModulePath);
	}

	public File adaptersFilePath() throws ApplicationException{
		File ngsModulePath = Settings.getInstance().getConfig().getNgsModulePath();
		if (ngsModulePath == null)
			throw new ApplicationException("NGS module is missing contact server admin.");

		return NgsModule.adaptersFilePath(ngsModulePath);
	}

	/**
	 * Set the published flag for every tool config.
	 * @param remoteManifests the manifests from remote repository.
	 */
	public void refreshToolCofigState(List<ToolManifest> remoteManifests) {
		if (remoteManifests == null)
			remoteManifests = new ArrayList<ToolManifest>();

		for (ToolConfig c: getTools()){
			// c.setPublished(false);
			if (c.getToolMenifest() != null) {
				// find same
				boolean foundInRemote = false;
				for (ToolManifest m: remoteManifests)
					if (m.isSameSignature(c.getToolMenifest())) {
						c.setPublished(true);
						foundInRemote = true;
					}
				if (!foundInRemote && c.isPublished()) 
					c.setRetracted(true); // someone must have removed this tool.
			}
		}
	}
	
	/**
	 * @param url url path component that defines the tool 
	 * @return
	 */
	public ToolConfig getToolConfigByUrlPath(String url) {
		ToolConfig toolConfig = null;
		// find organism config
		for (ToolConfig c: getTools())
			if (c.getPath().equals(url))
				toolConfig = c;

		return toolConfig;
	}

	public ToolConfig getToolConfigByConfiguration(String xmlDir) {
		if (xmlDir.isEmpty())
			return null;

		ToolConfig toolConfig = null;
		for (ToolConfig c: getTools())
			if (FileUtil.isSameFile(c.getConfiguration(), xmlDir))
				toolConfig = c;

		return toolConfig;
	}

	public List<ToolConfig> getTools() {
		return tools;
	}
	
	public boolean removeTool(ToolConfig tool) {
		ToolConfig sameConfig = getToolConfigByConfiguration(tool.getConfiguration());
		if (getToolConfigByConfiguration(tool.getConfiguration()) != null)
			return tools.remove(sameConfig);
		else 
			return false;
	}

	public void putTool(ToolConfig tool) {
		ToolConfig sameConfig = getToolConfigByConfiguration(tool.getConfiguration());
		if (getToolConfigByConfiguration(tool.getConfiguration()) != null)
			tools.remove(sameConfig);

		tools.add(tool);
	}

	public List<ToolManifest> getManifests(){
		List<ToolManifest> ans = new ArrayList<ToolManifest>();
		for (ToolConfig c: tools){
			ToolManifest m = c.getToolMenifest();
			if (m != null)
				ans.add(m);
		}
		return ans;
	}
	
	// classes

	public static class GeneralConfig {
		private String paupCmd = "paup4b10";
		private String clustalWCmd = "clustalW";
		private String blastPath = "/usr/bin/";
		private String treePuzzleCmd = "puzzle";
		private String treeGraphCmd = "tgf";
		private String epsToPdfCmd = "epstopdf";
		private String imageMagickConvertCmd = "convert";
		private int maxAllowedSeqs = 2000;
		private String inkscapeCmd = "inkscape";
		private String edirectPath = "/usr/bin/edirect/";
		//NGS
		private String diamondPath = "diamond";
		private String fastqcCmd = "fastqc";
		private String spadesCmd = "spades";
		private String bioPythonPath = "";
		private String sequencetool = "";

		private String publisherName; // Unique publisher name for the server copied to ToolManifest.
		private String publisherPassword; // Unique publisher name for the server created with GeneralConfig. used by Repo server and also sored there.
		private String repoUrl; // url of repository server.
		private String adminPassword;

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
		public String getDiamondPath() {
			return diamondPath;
		}
		public void setDiamondPath(String diamondPath) {
			this.diamondPath = diamondPath;
		}
		public String getPublisherName() {
			return publisherName;
		}
		public void setPublisherName(String publisherName) {
			this.publisherName = publisherName;
		}
		public String getPublisherPassword() {
			return publisherPassword;
		}
		public void setPublisherPassword(String publisherPassword) {
			this.publisherPassword = publisherPassword;
		}
		public String getRepoUrl() {
			//default
			if (repoUrl == null || repoUrl.isEmpty())
				return "http://typingtools.emweb.be/repository-test/repo-service";
			return repoUrl;
		}
		public void setRepoUrl(String repoUrl) {
			this.repoUrl = repoUrl;
		}
		public String getAdminPassword() {
			return adminPassword;
		}
		public void setAdminPassword(String adminPassword) {
			this.adminPassword = adminPassword;
		}
		public String getFastqcCmd() {
			return fastqcCmd;
		}
		public void setFastqcCmd(String fastqcCmd) {
			this.fastqcCmd = fastqcCmd;
		}
		public String getSpadesCmd() {
			return spadesCmd;
		}
		public void setSpadesCmd(String spadesCmd) {
			this.spadesCmd = spadesCmd;
		}
		public String getEdirectPath() {
			return edirectPath;
		}
		public void setEdirectPath(String edirectPath) {
			this.edirectPath = edirectPath;
		}
		public String getBioPythonPath() {
			return bioPythonPath;
		}
		public void setBioPythonPath(String bioPythonPath) {
			this.bioPythonPath = bioPythonPath;
		}
		public String getSequencetool() {
			return sequencetool;
		}
		public void setSequencetool(String sequencetool) {
			this.sequencetool = sequencetool;
		}
	}

	public static class ToolConfig {
		private String path = new String(); // unique url path component that defines the tool (default toolId)
		private String configuration = new String(); // xmlPath - the directory that contains all the tool files.
		private String jobDir = new String(); // will contain all the work dirs of the tool. (Generated analysis data)
		private boolean autoUpdate;
		private boolean webService;
		private boolean ui;
		private boolean published = false; // used to remember the published state when off line.
		private boolean retracted = false; // used to remember the retracted state when off line.
		// if true the pan viral tool will redirect to this tool version. 
		// Unique per tool id.
		private boolean currentUsedVersion = false; 
		// ToolMenifest read manifests from configuration dir.
		transient private ToolManifest manifest = null;

		public ToolConfig copy() {
			ToolConfig c = new ToolConfig();
			c.autoUpdate = autoUpdate;
			c.webService = webService;
			c.ui = ui;

			return c;
		}

		/**
		 * create and set job, xml dirs for a new tool.
		 */
		public void genetareDirs() {
			genetareJobDir(null);
			genetareConfigurationDir();
		}
		public void genetareConfigurationDir() {
			genetareConfigurationDir(null);
		}

		public String genetareDir(String parentDir, String suggestDirName) {
			String toolDir = null;
			try {
				if (suggestDirName == null)
					toolDir = FileUtil.createTempDirectory("tool-dir", 
							new File(parentDir)).getAbsolutePath();
				else {
					toolDir = parentDir + File.separator + suggestDirName;
					new File(toolDir).mkdirs();
				}
			} catch (IOException e) {
				e.printStackTrace();
				assert(false); 
			}
			return toolDir;
		}

		public void genetareConfigurationDir(String suggestDirName) {
			setConfiguration(genetareDir(Settings.getInstance().getBaseXmlDir(), suggestDirName) + File.separator);
		}
		
		public void genetareJobDir(String suggestDirName) {
			setJobDir(genetareDir(Settings.getInstance().getBaseJobDir(), suggestDirName) + File.separator);
		}

		public ToolManifest getToolMenifest() {
			File f = new File(configuration + File.separator + "manifest.json");
			if (f.exists() && manifest == null) {
				String json = FileUtil.readFile(f);
				manifest = ToolManifest.parseJson(json);
			}
			return manifest;
		}

		public void invalidateToolManifest() {
			manifest = null;
		}

		public String getUniqueToolId() {
			return getToolMenifest() == null ? null : getToolMenifest().getUniqueToolId();
		}
		public String getId() {
			return getToolMenifest() == null ? null : getToolMenifest().getId();
		}
		public String getVersion() {
			return getToolMenifest() == null ? null : getToolMenifest().getVersion();
		}

		public String getFullUrl() {
			return WApplication.getInstance().getEnvironment().
					getDeploymentPath() + getPath();
		}

		public String getPath() {
			if (path != null)
				return path;
			else
				return getToolMenifest() == null ? null : getToolMenifest().getUniqueToolId();
		}
		public void setPath(String path) {
			this.path = path;
		}
		public File getConfigurationFile() {
			return new File(configuration);
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
		public String getVerificationDir() {
			return jobDir + File.separator + "verification";
		}
		public String getJobDir() {
			return jobDir;
		}
		public void setJobDir(String jobDir) {
			this.jobDir = jobDir;
		}
		public boolean isWebService() {
			return webService;
		}
		public void setWebService(boolean webService) {
			this.webService = webService;
		}
		public boolean isUi() {
			return ui;
		}
		public void setUi(boolean ui) {
			this.ui = ui;
		}

		public boolean isPublished() {
			return published;
		}

		public void setPublished(boolean published) {
			this.published = published;
		}

		public boolean isRetracted() {
			return retracted;
		}

		public void setRetracted(boolean retracted) {
			this.retracted = retracted;
		}

		public boolean isCurrentUsedVersion() {
			return currentUsedVersion;
		}

		/**
		 * If true the pan viral tool will redirect to this tool version. 
		 * Unique per tool id !!
		 * @param usedByPanViralTool
		 */
		public void setCurrentUsedVersion(boolean currentUsedVersion) {
			this.currentUsedVersion = currentUsedVersion;
		}
	}
}
