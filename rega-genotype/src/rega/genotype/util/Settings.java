package rega.genotype.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class Settings {
	private Settings(File f) {
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
	
	private File xmlPath;
	private String paupCmd;
	private String clustalWCmd;
	private File blastPath;
	private String treePuzzleCmd;
	
	private static Settings instance;
	
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
            }
        }
    }
    
	public static Settings getInstance() {
        String configFile = System.getenv("REGA_GENOTYPE_CONF_DIR");
        
        if(configFile==null) {
            String osName = System.getProperty("os.name");
            osName = osName.toLowerCase();
            if(osName.startsWith("windows"))
                configFile = "C:\\Program files\\rega_genotype\\global-conf.xml";
            else
                configFile = "/etc/rega_genotype/global-conf.xml";
        } else {
            configFile += File.separatorChar + "global-conf.xml";
        }
        
        return new Settings(new File(configFile));
	}
	
	public static void main(String [] args) {
		Settings s = Settings.getInstance();
		System.err.println(s.getClustalWCmd());
		System.err.println(s.getPaupCmd());
		System.err.println(s.getBlastPath());
		System.err.println(s.getTreePuzzleCmd());
		System.err.println(s.getXmlPath());
	}
}
