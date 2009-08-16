package rega.genotype.ui.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public abstract class SaxParser extends DefaultHandler {
	//TODO
	//header('Content-type: application/ms-excell');
	
	//TODO check with incomplete xml files
	//TODO check with xml file with 1 uncomplete sequence
	
	//TODO make sure this is not called if no xml file exists
	private StringBuilder currentPath = new StringBuilder();

	private StringBuilder values = new StringBuilder();
	
	protected Map<String, String> valuesMap = new HashMap<String, String>();
	protected List<String> elements = new ArrayList<String>();
	
	protected boolean writtenFullSequence = false;
	
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
    		endSequence();
    		valuesMap.clear();
    		elements.clear();
    		writtenFullSequence = true;
    	}

    	removeFromCurrentPath();
    }
    
    public abstract void endSequence();
    
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
    	values.append(new String(ch, start, length));
    }
    
    public void parse(InputSource source)  throws SAXException, IOException {
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setContentHandler(this);
        xmlReader.setErrorHandler(this);
        xmlReader.parse(source);
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
}
