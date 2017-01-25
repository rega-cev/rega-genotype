/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Stack;

/**
 * A utility class that serializes genotyping results to the results.xml file.
 * 
 * @author koen
 */
public class ResultTracer {
    protected PrintWriter w;
    protected File        file;
    protected AbstractSequence currentSequence;
    protected int indent;
	protected Stack<String> openElements = new Stack<String>();

    public ResultTracer(OutputStream output) {
        this.file = null;
        this.w = new PrintWriter(output);
        this.indent = 0;
        writeXMLHeader();
        currentSequence = null;
    }

    public ResultTracer(File file) throws FileNotFoundException {
        this.file = file;
        this.w = new PrintWriter(new FileOutputStream(file));
        writeXMLHeader();
        currentSequence = null;
    }

    public void addResult(AbstractAnalysis.Result result) {
        if (result.getSequence() != null
        	&& result.getSequence().sourceSequence() != currentSequence) {
            finishCurrentSequence();
            startNewSequence(result.getSequence());
        }

        result.writeXML(this);
    }

    private String escapeXml(String s) {
    	s = s.replaceAll("&", "&amp;");
    	s = s.replaceAll("<", "&lt;");
    	s = s.replaceAll(">", "&gt;");
    	s = s.replaceAll("\"", "&quot;");
    	s = s.replaceAll("\'", "&#039;");
    	
    	return s;
    }
    
    private void startNewSequence(AbstractSequence sequence) {
    	if (sequence != null) {
           printlnOpen("<sequence name=\"" + escapeXml(sequence.getName()) + "\" length=\""
                    + sequence.getLength() + "\" description=\"" + escapeXml(sequence.getDescription()) +"\">");
          printlnOpen("<nucleotides>");
          println(sequence.getSequence());
          printlnClose("</nucleotides>");

    	} 
    	currentSequence = sequence;
    }

    public void finishCurrentSequence() {
        if (currentSequence != null)
            printlnClose("</sequence>");
        w.flush();
        currentSequence = null;
    }

    public void finish() {
        writeXMLEnd();
    }

    private void writeXMLHeader() {
        printlnOpen("<genotype_result>");
        w.flush();
    }

    protected void writeXMLEnd() {
        finishCurrentSequence();
            
        printlnClose("</genotype_result>");
        w.flush();
    }

    void flush() {
        w.flush();
    }
    
    void increaseIndent() {
        indent += 2;
    }
    
    void decreaseIndent() {
        indent -= 2;
    }

    public void println(String s) {
        w.println(indent() + s);
    }

    /**
     * open xml element the element can be closed with printlnCloseLastElement.
     * @param tag xml tag example: sequence
     * @param attributes xml attributes example: name="gi_9629352__0 consensus" length="6423" description=" "
     */
    public void printlnOpenElement(String tag, String attributes) {
    	openElements.push(tag);
        printlnOpen("<" + tag + " " + attributes + ">");
    }

    public void printlnOpenElement(String tag) {
    	openElements.push(tag);
        printlnOpen("<" + tag + ">");
    }

    /**
     * Close and removes the last element that was open with 
     * printlnOpenElement
     */
    public void printlnCloseLastElement() {
        printlnClose("</" + openElements.pop() + ">");
    }

    public void printlnCloseLastElement(String tag) {
    	if (!openElements.peek().equals(tag))
    		throw new RuntimeException("Tracer error: wrong tag");
        printlnCloseLastElement();
    }

    public void printlnOpen(String s) {
        println(s);
        increaseIndent();
    }

    public void printlnClose(String s) {
        decreaseIndent();
        println(s);
    }
    
    private String indent() {
        StringBuffer s = new StringBuffer(indent);
        for (int i = 0; i < indent; ++i) {
            s.append(' ');
        }
        
        return s.toString();
    }

    public String quote(String id) {
        return "\"" + id + "\"";
    }

    public void printNoindent(String s) {
        w.print(s);
    }

    public void add(String tag, CharSequence inner) {
        println("<" + tag + ">" + inner + "</" + tag + ">");
    }

    public void add(String tag, float value) {
        add(tag, String.valueOf(Math.round(value * 1000)/1000.));
    }

    public void add(String tag, double value) {
        add(tag, String.valueOf(Math.round(value * 1000)/1000.));
    }

    public void add(String tag, int value) {
        add(tag, Integer.toString(value));
    }

    public void add(String tag, Long value) {
        add(tag, Long.toString(value));
    }

    public File getResourceFile(String extension) {
        File result = null;

        do {
            String name = "r" + (int)(Math.random()*10000000) + "." + extension;

            result = new File(getOutputPath() + File.separator + name);
        } while (result.exists());

        return result;
    }

    public String getOutputPath() {
        if (file.getParent() != null)
            return file.getParent();
        else
            return ".";
    }

	public void printError(AnalysisException e) {
        if (e.getSequence().sourceSequence() != currentSequence) {
            finishCurrentSequence();
            startNewSequence(e.getSequence());
        }

        printlnOpen("<error>");
        println(e.getMessage());
        printlnClose("</error>");
	}

	public void printError(Exception e) {
        printlnOpen("<error>");
        println(e.getMessage());
        printlnClose("</error>");		
	}
}
