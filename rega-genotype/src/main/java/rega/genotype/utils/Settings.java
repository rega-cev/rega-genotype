/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.utils;

import java.io.File;

import javax.servlet.ServletContext;

import rega.genotype.BlastAnalysis;
import rega.genotype.GenotypeTool;
import rega.genotype.PhyloClusterAnalysis;
import rega.genotype.SequenceAlign;
import rega.genotype.ui.data.Config;
import rega.genotype.ui.util.FileUtil;

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
	
	public Settings(File f) {
		System.err.println("Loading config file: " + f.getAbsolutePath());
		if (!f.exists())
			throw new RuntimeException("Config file could not be found!");

		String json = FileUtil.readFile(f.getAbsolutePath());
		config = Config.parseJson(json);
	}

	public File getXmlPath(String toolId) {
		return new File(config.getToolConfig(toolId).getConfiguration());
	}

	public String getXmlPathAsString(String toolId) {
		return config.getToolConfig(toolId).getConfiguration();
	}

	public File getJobDir(String toolId) {
		return new File(config.getToolConfig(toolId).getJobDir());
	}

	public String getPaupCmd() {
		return config.getGeneralConfig().getPaupCmd();
	}

	public String getClustalWCmd() {
		return config.getGeneralConfig().getClustalWCmd();
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

	public File getXmlBasePath(){
		return new File(config.getGeneralConfig().getXmlBasePath());
	}

	public Config getConfig() {
		return config;
	}

	public static void initSettings(Settings s) {
		instance = s;
		PhyloClusterAnalysis.paupCommand = s.getPaupCmd();
		SequenceAlign.clustalWPath = s.getClustalWCmd();
		GenotypeTool.setXmlBasePath(s.getXmlBasePath().getAbsolutePath() + File.separatorChar);
		BlastAnalysis.blastPath = s.getBlastPath().getAbsolutePath() + File.separatorChar;
		PhyloClusterAnalysis.puzzleCommand = s.getTreePuzzleCmd();
		treeGraphCommand = s.getTreeGraphCmd();
	}

	public static Settings getInstance() {
		return instance;
	}

	public static Settings getInstance(ServletContext context) {
		if (instance != null)
			return instance;
		
        String configFile = null;
        
        /*
         * For a real deployment:
         *  - use servlet-context init parameter for configuration of the configuration file
         *  - or REGA_GENOTYPE_CONF_DIR env variable for the CLI tool
         *  
         * For development:
         *  - we default to ./etc/
         */

        if (context != null) {
        	configFile = context.getInitParameter("configFile");
        	if (configFile != null){
        		instance =  new Settings(new File(configFile));
        		return instance;
        	}
        } 
        
        if (configFile == null) {
            System.err.println("REGA_GENOTYPE_CONF_DIR"+":" + System.getenv("REGA_GENOTYPE_CONF_DIR"));
        	configFile = System.getenv("REGA_GENOTYPE_CONF_DIR");
        }
        
        if (configFile == null) {
            String osName = System.getProperty("os.name");
            osName = osName.toLowerCase();
            if (osName.startsWith("windows"))
                configFile = "C:\\Program files\\rega_genotype\\";
            else
                configFile = "etc/";
        }
        configFile += File.separatorChar + "config";
        
        instance = new Settings(new File(configFile));
        
        return instance;
	}
}