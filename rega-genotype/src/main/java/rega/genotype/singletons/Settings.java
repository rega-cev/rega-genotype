/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.singletons;

import java.io.File;

import javax.servlet.ServletContext;

import rega.genotype.config.Config;
import rega.genotype.utils.FileUtil;

/**
 * Singleton class which contains the application settings, parses them from the xml configuration file.
 * 
 * @author simbre1
 *
 */
public class Settings {
	public final static String defaultStyleSheet = "../style/genotype.css";
	public static String treeGraphCommand = "/usr/bin/tgf";

	private static Settings instance = null;
	
	private Config config;
	/*
	 * work dir structure:
	 * base-work-dir:
	 * 		- config.json
	 * 		- xml: contains all tool related data.
	 * 		- job: contains working dirs per job
	 */
	private String baseDir;
	
	public Settings(File file) {
		System.err.println("Loading config file: " + file.getAbsolutePath());

		if (file.exists()) {
			String json = FileUtil.readFile(file);
			config = Config.parseJson(json);
		}
	}

	/**
	 * @param url url path component that defines the tool 
	 * @return the directory that contains all the tool files or null if that dir does not exist.
	 */
	public File getXmlPath(String url) {
		if (config.getToolConfigByUrlPath(url) == null)
			return null;
		return new File(config.getToolConfigByUrlPath(url).getConfiguration());
	}
	/**
	 * @param url url path component that defines the tool 
	 * @return the directory that contains all the tool files.
	 */
	public String getXmlPathAsString(String url) {
		return config.getToolConfigByUrlPath(url).getConfiguration();
	}
	public File getJobDir(String url) {
		return new File(config.getToolConfigByUrlPath(url).getJobDir());
	}

	public String getPaupCmd() {
		return config.getGeneralConfig().getPaupCmd();
	}

	public String getClustalWCmd() {
		return config.getGeneralConfig().getClustalWCmd();
	}

	public String getBaseXmlDir() {
		return baseDir  + File.separator + "xml";
	}
	public String getBaseJobDir() {
		return baseDir  + File.separator + "job";
	}
	public String getBasePackagedToolsDir() {
		return baseDir  + File.separator + "packaged-tools";
	}
	public String getBaseDir() {
		return baseDir;
	}

	public String getBlastPathStr() {
		return getBlastPath().getAbsolutePath() + File.separatorChar;
	}

	public File getBlastPath() {
		return new File(config.getGeneralConfig().getBlastPath());
	}

	public String getTreePuzzleCmd() {
		return config.getGeneralConfig().getTreePuzzleCmd();
	}

	public String getTreeGraphCmd() {
		return config.getGeneralConfig().getTreeGraphCmd();
	}

	public String getEpsToPdfCmd() {
		return config.getGeneralConfig().getEpsToPdfCmd();
	}

	public String getImageMagickConvertCmd() {
		return config.getGeneralConfig().getImageMagickConvertCmd();
	}

	public int getMaxAllowedSeqs() {
		return config.getGeneralConfig().getMaxAllowedSeqs();
	}

	public final Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public static void initSettings(Settings s) {
		instance = s;
		if (s.getConfig() != null)
			treeGraphCommand = s.getTreeGraphCmd();
	}

	public static Settings getInstance() {
		return instance;
	}

	public static Settings getInstance(ServletContext context) {
		return getInstance(context, false);
	}

	/**
	 * Will construct the instance if not yet constructed. 
	 * Need to be called by every servlet because we dont know what servlet will run first. 
	 * @param context
	 * @return
	 */
	public static Settings getInstance(ServletContext context, boolean isUnitTest) {
		if (instance != null)
			return instance;

		String baseDir = isUnitTest ? "base-unit-test-work-dir/" : getBaseDir(context);
		instance = new Settings(new File(baseDir + "config.json"));
		if (!baseDir.endsWith("/"))
			baseDir += "/";
		instance.baseDir =  baseDir;

        return instance;
	}

	/**
	 * @param context
	 * @return A file that contains the config.
	 */
	private static String getBaseDir(ServletContext context) {
		String baseDir = null;

		/*
		 * For a real deployment:
		 *  - use servlet-context init parameter for configuration of the configuration file
		 *  - or REGA_GENOTYPE_WORK_DIR env variable for the CLI tool
		 *  
		 * For development:
		 *  - we default to ./base-work-dir/
		 */

		if (context != null) 
			baseDir = context.getInitParameter("baseWorkDir");

		if (baseDir == null) {
			System.err.println("REGA_GENOTYPE_WORK_DIR"+":" + System.getenv("REGA_GENOTYPE_WORK_DIR"));
			baseDir = System.getenv("REGA_GENOTYPE_WORK_DIR");
		}

		if (baseDir == null) {
			String osName = System.getProperty("os.name");
			osName = osName.toLowerCase();
			if (osName.startsWith("windows"))
				baseDir = "C:\\Program files\\rega_genotype\\";
			else
				baseDir = "./base-work-dir/";
		}

		new File(baseDir).mkdirs(); // make sure that the dir exists
        return baseDir;
	}
}