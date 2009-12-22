/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import eu.webtoolkit.jwt.StringUtils;
import eu.webtoolkit.jwt.WWebWidget;

/**
 * A sax parser which parses the rega-genotype analysis result file and stores 
 * all values in a Map per sequences. Sequences can be accessed by implementing 
 * the endSequence() method.
 */
public abstract class GenotypeResultParser extends DefaultHandler {

	private List<String> currentPath = new ArrayList<String>();

	protected StringBuilder values = new StringBuilder();
	
	private Map<String, String> valuesMap = new HashMap<String, String>();
	private List<String> elements = new ArrayList<String>();
		
	protected int sequenceIndex = -1;
	private int filteredSequences = 0;

	private boolean stop = false;

	public GenotypeResultParser() {
	}
	
	public int getFilteredSequences() {
		return filteredSequences;
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(!stop) {
			String specifier = "";
			
			for(int i = 0; i<attributes.getLength(); i++) {
				if(attributes.getQName(i).equals("id")) {
					specifier += "[@"+ attributes.getQName(i) +"=\'" + attributes.getValue(i) + "\']";
				} else {
					valuesMap.put(getCurrentPath()+"/"+qName+"/@"+attributes.getQName(i), attributes.getValue(i));
				}
			}
	
			addToCurrentPath(qName + specifier);
		}
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    	if(!stop) {
	    	String value = values.toString().trim();
	    	if(!value.equals("")) {
	    		valuesMap.put(getCurrentPath(), value);
	    	} else {
	    		elements.add(getCurrentPath());
	    	}
	    	values.delete(0, values.length());
	    	
	    	
	    	if(getCurrentPath().equals("/genotype_result/sequence")) {
	    		sequenceIndex++;
	    		if (!skipSequence()) {
	    			endSequence();
	    		} else {
	    			filteredSequences++;
	    		}
	    		if(!stop) {
	    		valuesMap.clear();
	    		elements.clear();
	    		}
	    	}
	
	    	removeFromCurrentPath();
    	}
    }
    
    public abstract void endSequence();
    
    public abstract boolean skipSequence();

	public void endFile() { }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
    	if(!stop) {
    		values.append(new String(ch, start, length));
    	}
    }
    
    private void parse(InputSource source)  throws SAXException, IOException {
    	reset();
    	XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setContentHandler(this);
        xmlReader.setErrorHandler(this);
        try {
        	xmlReader.parse(source);
        	endFile();
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
	
	private String toXPath(String path){
		return '/'+ path.replace('.', '/')
				   .replace("[","[@id='")
				   .replace("]", "']");
	}
    
    private void reset() {
    	currentPath.clear();
    	sequenceIndex = -1;
    	filteredSequences = 0;
    }
    
    protected String getCurrentPath() {
    	StringBuilder tmp = new StringBuilder("/");
    	
    	for(int i = 0; i<currentPath.size(); i++) {
    		tmp.append(currentPath.get(i));
    		if(i!=currentPath.size()-1)
    			tmp.append('/');
    	}
    	
    	return tmp.toString();
    }
    
    private void addToCurrentPath(String p) {
    	currentPath.add(p);
    }
    
    private void removeFromCurrentPath() {
    	currentPath.remove(currentPath.size()-1);
    }
    
	public int getSequenceIndex() {
		return sequenceIndex;
	}
    
    public String getValue(String name) {
    	return valuesMap.get(name);
    }
    
    public String getEscapedValue(String name) {
    	String value = getValue(name);
    	if(value==null)
    		return null;
    	else
    		return WWebWidget.escapeText(value, true);
    }
    
    public boolean elementExists(String name) {
    	return elements.contains(name);
    }
    
    public void stopParsing() {
    	stop = true;
    }

	public void dumpDebug() {
		System.err.println("Start dump:");
		
		System.err.println("Value map:");
		for (Map.Entry<String, String> e:valuesMap.entrySet())
			System.err.println(e.getKey() + ": " + e.getValue());

		System.err.println("Elements:");
		for (String e:elements)
			System.err.println(e);

		System.err.println("End dump:");
	}
	
	public boolean stopped(){
		return stop;
	}

	public static class SkipToSequenceParser extends GenotypeResultParser {
		private int selectedSequenceIndex;
		private Document doc;
		private Element root;
		private Stack<Element> stack;

		public SkipToSequenceParser(int selectedSequenceIndex) {
			super();

			this.selectedSequenceIndex = selectedSequenceIndex; 
			
			stack = new Stack<Element>();
			root = new Element("genotype_result");
			doc = new Document(root);
			stack.push(root);
		}
		
		public int getSelectedSequenceIndex(){
			return selectedSequenceIndex;
		}
		
		public String getValue(String xpath){
			Object o = getObject(xpath);
			if(o != null){
				if(o instanceof Element)
					return ((Element)o).getText();
				if(o instanceof Attribute)
					return ((Attribute)o).getValue();
			}
			return null;
		}
		
		public boolean elementExists(String xpath) {
			return getElement(xpath) != null;
	    }
		
		public Element getElement(String xpath){
			Object o = getObject(xpath);
			if(o != null && o instanceof Element)
				return (Element)o;
			else
				return null;
		}
		
		private Object getObject(String xpath){
			try {
				XPath x = XPath.newInstance(xpath);
				return x.selectSingleNode(doc);
			} catch (JDOMException e) {
				return null;
			}
		}
		
		@Override
		public void endSequence() {
			if(getSequenceIndex() == getSelectedSequenceIndex())
				stopParsing();
		}
		
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(!stopped()){
				if(qName.equals("sequence") && getCurrentPath().equals("/genotype_result"))
					++sequenceIndex;
				
				if(getSequenceIndex() == getSelectedSequenceIndex()){
					Element e = new Element(qName);
					for(int i = 0; i<attributes.getLength(); i++)
						e.setAttribute(attributes.getQName(i),attributes.getValue(i));
					stack.peek().addContent(e);
					stack.push(e);
				}
			}
		}
		
	    @Override
	    public void characters(char[] ch, int start, int length) throws SAXException {
	    	if(!stopped() && getSequenceIndex() == getSelectedSequenceIndex()) {
	    		values.append(new String(ch, start, length));
	    	}
	    }
	    
	    @Override
	    public void endElement(String uri, String localName, String qName) throws SAXException {
	    	if(!stopped()) {
		    	if(getSequenceIndex() == getSelectedSequenceIndex()){
		    		String value = values.toString().trim();
			    	if(!value.equals(""))
			    		stack.peek().setText(value);
			    	values.delete(0, values.length());
			    	
		    		stack.pop();
		    	}
		    	if(qName.equals("sequence") && getCurrentPath().equals("/genotype_result")) {
		    		endSequence();
		    	}
	    	}
	    }
		
	    protected String getCurrentPath(){
	    	StringBuilder sb = new StringBuilder("/");
	    	boolean first = true;
	    	for(Element e : stack){
	    		if(first)
	    			first = false;
	    		else
	    			sb.append('/');
	    		sb.append(e.getName());
	    	}
	    	return sb.toString();
	    }
	    
	    public void dumpDebug(){
	    	System.err.println("Start debug:");
	    	dumpDebug(root, "");
	    	System.err.println("End dump:");
	    }
	    private void dumpDebug(Element e, String pfx){
	    	System.err.print(pfx + e.getName());
	    	for(Object o : e.getAttributes()){
	    		Attribute a = (Attribute)o;
	    		System.err.print(" "+ a.getName() +"='"+ a.getValue() +"'");
	    	}
	    	System.err.println();
	    	if(e.getTextTrim().length() > 0)
	    		System.err.println(e.getText());
	    	
	    	for(Object o : e.getChildren())
	    		dumpDebug((Element)o,pfx+"-");
	    }

		public boolean indexOutOfBounds() {
			return getSelectedSequenceIndex() > getSequenceIndex();
		}

		@Override
		public boolean skipSequence() {
			return false;
		}
	}

	public static GenotypeResultParser parseFile(File jobDir, int selectedSequenceIndex) {
		SkipToSequenceParser p = new SkipToSequenceParser(selectedSequenceIndex);
		p.parseFile(jobDir);

		if (!p.indexOutOfBounds())
			return p;
		else
			return null;
	}
	
	public static void main(String args[]){
		File jobDir = new File(args[0]);
		String sequence = args.length > 1 ? args[1]:null;
		
		GenotypeResultParser p = GenotypeResultParser.parseFile(jobDir, Integer.parseInt(sequence));
		p.dumpDebug();
		System.err.println(p.getValue("/genotype_result/sequence/result/start"));
		System.err.println(p.getSequenceIndex());
	}
}
