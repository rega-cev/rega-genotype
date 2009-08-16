package rega.genotype.ui.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public abstract class SaxParser extends DefaultHandler {
	private StringBuilder currentPath = new StringBuilder();

	private StringBuilder values = new StringBuilder();
	
	private Map<String, String> valuesMap = new HashMap<String, String>();
	private List<String> elements = new ArrayList<String>();
		
	private int sequenceIndex = -1;

	public SaxParser() {
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		String specifier = "";
		
		for(int i = 0; i<attributes.getLength(); i++) {
			if(attributes.getQName(i).equals("id")) {
				specifier += '[' + attributes.getValue(i) + ']';
			} else {
				valuesMap.put(getCurrentPath()+"."+qName+"[\'"+attributes.getQName(i)+"\']", attributes.getValue(i));
			}
		}

		addToCurrentPath(qName + specifier);
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    	String value = values.toString().trim();
    	if(!value.equals("")) {
    		valuesMap.put(getCurrentPath(), value);
    	} else {
    		elements.add(getCurrentPath());
    	}
    	values.delete(0, values.length());
    	
    	if(getCurrentPath().equals("genotype_result.sequence")) {
    		sequenceIndex++;
    		endSequence();
    		valuesMap.clear();
    		elements.clear();
    	}

    	removeFromCurrentPath();
    }
    
    public abstract void endSequence();
    
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
    	values.append(new String(ch, start, length));
    }
    
    private void parse(InputSource source)  throws SAXException, IOException {
    	reset();
    	XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setContentHandler(this);
        xmlReader.setErrorHandler(this);
        try {
        xmlReader.parse(source);
        } catch (SAXParseException spe) {
        	if(!spe.getMessage().equals("XML document structures must start and end within the same entity."))
        		spe.printStackTrace();
        }
    }
    
    public void parseFile(File jobDir) {
    	File resultFile = new File(jobDir.getAbsolutePath()+File.separatorChar+"result.xml");
    	
    	if(resultFile.exists()) {
	    	try {
				parse(new InputSource(new FileReader(resultFile)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    }
    
    private void reset() {
    	currentPath.delete(0, currentPath.length());
    	sequenceIndex = -1;
    }
    
    private String getCurrentPath() {
    	return currentPath.toString();
    }
    
    private void addToCurrentPath(String p) {
    	if(currentPath.length()>0)
    		currentPath.append('.');
    	currentPath.append(p);
    }
    
    private void removeFromCurrentPath() {
    	int lastDot = currentPath.lastIndexOf(".");
    	if(lastDot==-1)
    		lastDot = 0;
    	currentPath.delete(lastDot, currentPath.length());
    }
    
	public int getSequenceIndex() {
		return sequenceIndex;
	}
    
    public String getValue(String name) {
    	return valuesMap.get(name);
    }
    
    public boolean elementExists(String name) {
    	return elements.contains(name);
    }
}
