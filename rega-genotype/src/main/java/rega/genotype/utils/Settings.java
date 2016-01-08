/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import rega.genotype.BlastAnalysis;
import rega.genotype.GenotypeTool;
import rega.genotype.PhyloClusterAnalysis;
import rega.genotype.SequenceAlign;
import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;

/**
 * Singleton class which contains the application settings, parses them from the xml configuration file.
 * 
 * @author simbre1
 *
 */
public class Settings {
	public final static String defaultStyleSheet = "../style/genotype.css";
	
	public Settings(File f) {
		System.err.println("Loading config file: " + f.getAbsolutePath());
		if (!f.exists())
			throw new RuntimeException("Config file could not be found!");
		parseConfFile(f);
	}
	
	public File getXmlPath() {
		return xmlPath;
	}
	
	public String getPaupCmd() {
		return paupCmd;
	}
	
	public String getClustalWCmd() {
		return clustalWCmd;
	}
	
	public File getBlastPath() {
		return blastPath;
	}
	
	public String getTreePuzzleCmd() {
		return treePuzzleCmd;
	}
	
	public String getTreeGraphCmd() {
		return treeGraphCmd;
	}
	
	public String getEpsToPdfCmd() {
		return epsToPdfCmd;
	}
	
	public String getImageMagickConvertCmd() {
		return imageMagickConvertCmd;
	}
	
	public File getJobDir(String organismName) {
		File f = jobDirs.get(organismName);
		if (f == null)
			f = defaultJobDir;

		return f;
	}
	
	public int getMaxAllowedSeqs() {
		return maxAllowedSeqs;
	}
	
	public List<File> getJobDirs() {
		List<File> dirs = new ArrayList<File>();
		dirs.addAll(jobDirs.values());
		return dirs;
	}
	
	private File xmlPath;
	private String paupCmd;
	private String clustalWCmd;
	private File blastPath;
	private String treePuzzleCmd;
	private String treeGraphCmd;
	private String epsToPdfCmd;
	private String imageMagickConvertCmd;
	private int maxAllowedSeqs;
	private File defaultJobDir;	
	private Map<String, File> jobDirs = new HashMap<String, File>();
	public static String treeGraphCommand = "/usr/bin/tgf";

    @SuppressWarnings("unchecked")
	private void parseConfFile(File confFile) {
        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(confFile);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Element root = doc.getRootElement();

        List children = root.getChildren("property");
        Element e;
        String name;
        for (Object o : children) {
            e = ((Element) o);
            name = e.getAttributeValue("name");
            if (name.equals("xmlPath")) {
            	xmlPath = new File(e.getValue().trim());
            } else if(name.equals("paupCmd")) {
            	paupCmd = e.getValue().trim();
            } else if(name.equals("clustalWCmd")) {
            	clustalWCmd = e.getValue().trim();
            } else if(name.equals("blastPath")) {
            	blastPath = new File(e.getValue().trim());
            } else if(name.equals("treePuzzleCmd")) {
            	treePuzzleCmd = e.getValue().trim();
            } else if(name.equals("treeGraphCmd")) {
            	treeGraphCmd = e.getValue().trim();
            } else if(name.equals("epsToPdfCmd")) {
            	epsToPdfCmd = e.getValue().trim();
            } else if(name.equals("imageMagickConvertCmd")) {
            	imageMagickConvertCmd = e.getValue().trim();
            } else if(name.equals("jobDir")) {
            	defaultJobDir = new File(e.getValue().trim());
            } else if(name.startsWith("jobDir-")) {
            	String organism = name.split("-")[1];
            	jobDirs.put(organism, new File(e.getValue().trim()));
            } else if(name.equals("maxAllowedSequences")) {
            	maxAllowedSeqs = Integer.parseInt(e.getValue().trim());
            }
        }
    }
    
	public static void initSettings(Settings s) {
		PhyloClusterAnalysis.paupCommand = s.getPaupCmd();
		SequenceAlign.clustalWPath = s.getClustalWCmd();
		GenotypeTool.setXmlBasePath(s.getXmlPath().getAbsolutePath() + File.separatorChar);
		BlastAnalysis.blastPath = s.getBlastPath().getAbsolutePath() + File.separatorChar;
		PhyloClusterAnalysis.puzzleCommand = s.getTreePuzzleCmd();
		treeGraphCommand = s.getTreeGraphCmd();
	}

	public static Settings getInstance() {
		GenotypeApplication app = GenotypeMain.getApp();
		if (app == null)
			return getInstance(null);
		else
			return app.getSettings();
	}

	public static Settings getInstance(ServletContext context) {
        String configFile = null;
        
        if (context != null) {
        	configFile = context.getInitParameter("configFile");
        	if (configFile != null)
        		return new Settings(new File(configFile));
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
                configFile = "/etc/rega_genotype/";
        }
        configFile += File.separatorChar + "global-conf.xml";
        
        return new Settings(new File(configFile));
	}
}