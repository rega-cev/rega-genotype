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

/**
 * A utility class that serializes genotyping results to the results.xml file.
 * 
 * @author koen
 */
public class ResultTracer {
    PrintWriter w;
    File        file;
    AbstractSequence currentSequence;
    int indent;

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
        if (result.getSequence().sourceSequence() != currentSequence) {
            finishCurrentSequence();
            startNewSequence(result.getSequence());
        }

        result.writeXML(this);
    }

    private void startNewSequence(AbstractSequence sequence) {
    	if (sequence != null) {
            w.println("  <sequence name=\"" + sequence.getName() + "\" length=\""
                    + sequence.getLength() + "\">");
          w.println("    <nucleotides>");
          w.println("      " + sequence.getSequence());
          w.println("    </nucleotides>");
          
          indent = 4;
    	} else
    	  indent = 2;

    	currentSequence = sequence;
    }

    private void finishCurrentSequence() {
        if (currentSequence != null)
            w.println("  </sequence>");
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

    private void writeXMLEnd() {
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

    public void add(String tag, String inner) {
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

        indent = 4;
        printlnOpen("<error>");
        println(e.getMessage());
        printlnClose("</error>");
	}

	public void printError(FileFormatException e) {
		indent = 2;
        printlnOpen("<error>");
        println(e.getMessage());
        printlnClose("</error>");		
	}
}
