/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.DecimalFormat;
import java.util.List;
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

import eu.webtoolkit.jwt.Signal1;

/**
 * A sax parser which parses the rega-genotype analysis result file and offers
 * an XPath interface to an individual sequence result.
 */
public class GenotypeResultParser extends DefaultHandler
{
	private Document doc;
	private Element root;
	private Stack<Element> stack;
	private StringBuilder values = new StringBuilder();
	
	private int sequenceIndex;
	private int selectedSequenceIndex;
	private int filteredSequences;

	private boolean stop = false;

	public enum ParsingState {
		Pasring, // analysis is not yet finished, parser reached end of file.
		Ended,   // analysis finished and parser reached end of file.
		Stopped  // parsing was stopped
	}

	/**
	 * signal that end of file is reached. Example: if result.xml is not yet finished a parser thread can wait till it is done.
	 */
	private Signal1<ParsingState> eof = new Signal1<ParsingState>();

	public GenotypeResultParser() {
		this(-1);
	}

	public GenotypeResultParser(int selectedSequenceIndex) {
		this.selectedSequenceIndex = selectedSequenceIndex;

		reset();
	}
	
	public int getSelectedSequenceIndex(){
		return selectedSequenceIndex;
	}
	
	public String getValue(String xpath){
		Object o = getObject(xpath);
		
		if (o != null) {
			if (o instanceof Element)
				return ((Element)o).getText();
			if (o instanceof Attribute)
				return ((Attribute)o).getValue();
			if (o instanceof Number) {
				Double d = ((Number)o).doubleValue();
				DecimalFormat df = new DecimalFormat("##.#######");
				return df.format(d);
			}
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
	
	@SuppressWarnings("unchecked")
	public List<Element> getElements(String xpath){
		try {
			XPath x = XPath.newInstance(xpath);
			return x.selectNodes(doc);
		} catch (JDOMException e) {
			return null;
		}		
	}
	
	private Object getObject(String xpath){
		try {
			XPath x = XPath.newInstance(xpath);
			return x.selectSingleNode(doc);
		} catch (JDOMException e) {
			return null;
		}
	}
	
	public void endSequence() {
		if (getSequenceIndex() == getSelectedSequenceIndex())
			stopParsing();
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (!stopped()) {
			if (qName.equals("sequence") && getCurrentPath().equals("/genotype_result")) {
				if (root != null)
					root.removeContent();
				++sequenceIndex;
			}
			
			if (handlingSequence() || doc == null) {
				Element e = new Element(qName);
				for (int i = 0; i<attributes.getLength(); i++)
					e.setAttribute(attributes.getQName(i),attributes.getValue(i));
				if (doc != null)
					stack.peek().addContent(e);
				else {
					root = e;
					doc = new Document(root);
				}
				stack.push(e);
			}
		}
	}
	
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
    	if (!stopped() && handlingSequence()) {
    		values.append(new String(ch, start, length));
    	}
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    	if (!stopped()) {
	    	if (handlingSequence()) {
	    		String value = values.toString().trim();

	    		if (!value.equals(""))
		    		stack.peek().setText(value);
		    	values.delete(0, values.length());

	    		stack.pop();
	    	}

	    	if (qName.equals("sequence") && getCurrentPath().equals("/genotype_result"))
	    		endSequence();
    	}
    }

	private boolean handlingSequence() {
		return getSelectedSequenceIndex() == -1 || getSequenceIndex() == getSelectedSequenceIndex();
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
    
    	for (Object o : e.getAttributes()){
    		Attribute a = (Attribute)o;
    		System.err.print(" "+ a.getName() +"='"+ a.getValue() +"'");
    	}
    	
    	System.err.println();
    	
    	if (e.getTextTrim().length() > 0)
    		System.err.println(e.getText());
    	
    	for (Object o : e.getChildren())
    		dumpDebug((Element)o,pfx+"-");
    }

	public boolean indexOutOfBounds() {
		return getSelectedSequenceIndex() > getSequenceIndex();
	}

	public int getFilteredSequences() {
		return filteredSequences;
	}
    
    public boolean skipSequence() {
    	return false;
    }

	/**
	 * Read xml file, if end of file is reached but there is a missing closing
	 * tag (normally </genotype_result>) the reader will go 1 char back and call
	 * paused()
	 * 
	 * @author michael
	 */
	private class GenotypeResultReader extends LineNumberReader {
		public GenotypeResultReader(java.io.Reader in) {
			super(in);
		}

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			int ans = super.read(cbuf, off, len);
			if (ans != -1)
				mark(ans);
			else {
				if (stack.size() != 0) { // continue reading later
					reset();
					eof.trigger(ParsingState.Pasring);
				}
			}
			return ans;
		}
	}

	public void endFile() { 
		eof.trigger(ParsingState.Ended);
	}

	public void parse(InputSource source) throws SAXException, IOException {
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setContentHandler(this);
        xmlReader.setErrorHandler(this);
        try {
        	xmlReader.parse(source);
        	endFile();
        } catch (SAXParseException spe) {
        	if(!spe.getMessage().equals("XML document structures must start and end within the same entity."))
        		throw new RuntimeException(spe);
        }
    }

	public void parseFile(File jobDir) {
    	File resultFile = new File(jobDir.getAbsolutePath()+File.separatorChar+"result.xml");
    	
    	if(resultFile.exists()) {
    		try {
				GenotypeResultReader reader =
						new GenotypeResultReader(new InputStreamReader(
								new FileInputStream(resultFile)));
				InputSource inputSource = new InputSource(reader);
				parse(inputSource);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
    	}
    }
	
    private void reset() {
     	sequenceIndex = -1;
    	filteredSequences = 0;

    	stack = new Stack<Element>();
		doc = null;
    }
    
	public int getSequenceIndex() {
		return sequenceIndex;
	}
    
    public void stopParsing() {
    	stop = true;
    	eof.trigger(ParsingState.Stopped);
    }

	public boolean stopped(){
		return stop;
	}

	public Signal1<ParsingState> eof() {
		return eof;
	}

	public static GenotypeResultParser parseFile(File jobDir, int selectedSequenceIndex) {
		GenotypeResultParser p = new GenotypeResultParser(selectedSequenceIndex);
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
