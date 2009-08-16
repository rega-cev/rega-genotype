/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;
import jargs.gnu.CmdLineParser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.InvocationTargetException;

import rega.genotype.ui.util.GenotypeLib;

/**
 * Main class for the genotype tool.
 * 
 * It serves two purposes:
 *  - provides the main() function for using the tool to analyze a number of sequences
 *    using a specific implementation of the tool for an organism
 *  - provides an abstract base class for providing a specific implementation of the
 *    tool for an organism
 *
 * Genotyping tools may be used standalone or may be chained together. e.g. one tool may
 * be providing a distinction between related organisisms, which is chained to other tools
 * that provide detailed subtyping analyses for each of these organisms.
 *
 * @author koen
 */
public abstract class GenotypeTool {
    private static String xmlBasePath = ".";
    private static String workingDir = ".";

    private GenotypeTool parent;
    private ResultTracer tracer;

    public GenotypeTool() {
    	this.parent = null;
    }

    static private String[] parseArgs(String[] args) { 
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option paupPathOption = parser.addStringOption('p', "paup");
        CmdLineParser.Option clustalPathOption = parser.addStringOption('c', "clustal");
        CmdLineParser.Option helpOption = parser.addBooleanOption('h', "help");
        CmdLineParser.Option xmlPathOption = parser.addStringOption('x', "xml");
        CmdLineParser.Option blastPathOption = parser.addStringOption('b', "blast");
        CmdLineParser.Option treePuzzleCmdOption = parser.addStringOption('t', "treepuzzle");
        CmdLineParser.Option workingDirOption = parser.addStringOption('w', "workingDir");
        CmdLineParser.Option treeGraphCmdOption = parser.addStringOption('g', "treegraph");
        
        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            System.err.println(e.getMessage());
            printUsage();
            return null;
        }
        
        if (parser.getOptionValue(helpOption) == Boolean.TRUE) {
            printUsage();
            return null;
        }
        
        String paupPath = (String) parser.getOptionValue(paupPathOption);        
        if (paupPath != null)
            PhyloClusterAnalysis.paupCommand = paupPath;

        String clustalPath = (String) parser.getOptionValue(clustalPathOption);        
        if (clustalPath != null)
            SequenceAlign.clustalWPath = clustalPath;

        String xmlPath = (String) parser.getOptionValue(xmlPathOption);        
        if (xmlPath != null)
            xmlBasePath = xmlPath;
        
        String blastPath = (String) parser.getOptionValue(blastPathOption);        
        if (blastPath != null)
        	BlastAnalysis.blastPath = blastPath;
        
        String treePuzzleCmd = (String) parser.getOptionValue(treePuzzleCmdOption);        
        if (treePuzzleCmd != null)
        	PhyloClusterAnalysis.puzzleCommand = treePuzzleCmd;
        
        String workingDirTmp = (String) parser.getOptionValue(workingDirOption);        
        if (workingDirTmp != null)
        	workingDir = workingDirTmp;
        
        String treeGraphCmd = (String) parser.getOptionValue(treeGraphCmdOption);        
        if (treeGraphCmd != null)
        	GenotypeLib.treeGraphCommand = treeGraphCmd;

        return parser.getRemainingArgs();
	}

    private static void printUsage() {
		System.err.println("GenotypeTool: error parsing command-line.");
		System.err.println("usage: GenotypeTool[-p pauppath] [-c clustalpath] [-x xmlpath] analysis [sequences.fasta] result.xml");
		System.err.println();
		System.err.println("\tPerforms the given analysis and writes the result to the tracefile result.xml");
		System.err.println("\tIf no sequences are given, an internal analysis is performed.");
		System.err.println();
		System.err.println("options:");
		System.err.println("\t-p,--paup      	specify path to paup");
        System.err.println("\t-c,--clustal   	specify path to clustal");
        System.err.println("\t-x,--xml       	specify path to xml files");
        System.err.println("\t-b,--blast     	specify path to blast executables");
        System.err.println("\t-t,--treepuzzle   specify path to treepuzzle executable");
        System.err.println("\t-g,--treegraph    specify path to treegraph executable");
        System.err.println("\t-w,--workingDir   specify path to the working directory (default .)");
	}

	private void analyzeSelf(String traceFile) throws FileNotFoundException {
        startTracer(traceFile);

        try {
			analyzeSelf();
		} catch (AnalysisException e) {
			System.err.println(e.getMessage());
		}
        
        stopTracer();
	}
	
	/**
	 * This function analyzes an input FASTA file, and writes results to a given
	 * trace file.
	 * 
	 * For each sequence in the input file, it invokes analyze(AbstractSequence)
	 */
    public void analyze(String sequenceFile, String traceFile) throws IOException {
        startTracer(traceFile);

        LineNumberReader reader
            = new LineNumberReader
                (new InputStreamReader(new BufferedInputStream(new FileInputStream(sequenceFile))));
        
        try {
			for (;;) {
			    Sequence s = SequenceAlignment.readFastaFileSequence(reader, SequenceAlignment.SEQUENCE_DNA);
			    if (s != null) {
                    s.removeGaps();

		            System.err.println("Starting analysis of: " + s.getName());

		            long start = System.currentTimeMillis();
			        try {
                        analyze(s);
			        } catch (AnalysisException e) {
			            System.err.println(e.getMessage());
			            e.printStackTrace();
			            tracer.printError(e);
			        }
                    tracer.flush();
                    
		            long elapsedTimeMillis = System.currentTimeMillis()-start;
		            float elapsedTimeSec = elapsedTimeMillis/1000F;
		            
		            System.err.println("Completed analysis of: " + s.getName() + " (took " + elapsedTimeSec + "s)");
			        SequenceAlign.forgetAll();
			    } else
			        break;
			}
		} catch (FileFormatException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            tracer.printError(e);
		}
        
        stopTracer();        
    }

    protected void stopTracer() {
        getTracer().finish();
    }

    protected void startTracer(String traceFile) throws FileNotFoundException {
        tracer = new ResultTracer(new File(traceFile));
    }

    /**
     * Conclude the "unassigned" conclusion.
     */
    protected void conclude(String conclusion, String motivation) {
    	conclude(conclusion, motivation, null);
    }

    /**
     * Conclude the "unassigned" conclusion.
     */
    protected void conclude(String conclusion, String motivation, String id) {
        getTracer().printlnOpen("<conclusion type=\"unassigned\""
        		+ (id != null ? " id=\"" + id + "\"" : "") + ">");
        getTracer().printlnOpen("<assigned>");
        getTracer().add("id", "Unassigned");
        getTracer().add("name", (String) conclusion);
        getTracer().printlnClose("</assigned>");
        getTracer().add("motivation", motivation);
        getTracer().printlnClose("</conclusion>");    	
    }

    /**
     * Conclude a plain conclusion.
     */
    protected void conclude(AbstractAnalysis.Concludable conclusion, String motivation) {
    	conclude(conclusion, motivation, null);
    }

    /**
     * Conclude a plain conclusion.
     */
    protected void conclude(AbstractAnalysis.Concludable conclusion, String motivation, String id) {
        getTracer().printlnOpen("<conclusion type=\"simple\""
        		+ (id != null ? " id=\"" + id + "\"" : "") + ">");
        conclusion.writeConclusion(getTracer());
        getTracer().add("motivation", motivation);
        getTracer().printlnClose("</conclusion>");
    }

    /**
     * Conclude a combined major/minor conclusion.
     */
    protected void conclude(AbstractAnalysis.Concludable major, AbstractAnalysis.Concludable minor,
    						String motivation) {
        getTracer().printlnOpen("<conclusion type=\"composed\">");
        AbstractAnalysis.ComposedConclusion r = new AbstractAnalysis.ComposedConclusion(major, minor);
        r.writeConclusion(getTracer());
        getTracer().add("motivation", motivation);
        getTracer().printlnClose("</conclusion>");    	
    }

    /**
     * Abstract function that analyzes a sequence.
     * 
     * You should reimplement this sequence to create a new genotyping tool.
     */
    abstract public void analyze(AbstractSequence s) throws AnalysisException;

    /**
     * Abstract function that provides a self-check analysis.
     */
    abstract public void analyzeSelf() throws AnalysisException;

    /**
     * Read analyses from a given XML file.
     * Each analysis is configured to use the workingDir to store intermediate results.
     */
    protected AlignmentAnalyses readAnalyses(String file, File workingDir)
            throws IOException, ParameterProblemException, FileFormatException {
        return new AlignmentAnalyses(new File(xmlBasePath + File.separator + file),
                                     this,
                                     workingDir);
    }

    public static void setXmlBasePath(String xmlBasePath) {
		GenotypeTool.xmlBasePath = xmlBasePath;
	}

	/**
     * @return Returns the tracer.
     */
    public ResultTracer getTracer() {
        if (tracer == null)
            return parent.getTracer();
        else
            return tracer;
    }

    /**
     * @return Returns the parent genotype tool for a nested genotyping tool.
     */
    public GenotypeTool getParent() {
        return parent;
    }

    public void setParent(GenotypeTool parent) {
        this.parent = parent;
    }
    
    @SuppressWarnings("unchecked")
	public static void main(String[] args)
    	throws IOException, ParameterProblemException, FileFormatException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException {
    	
    	/*
    	 * Usage: GenotypeTool [-p,-c,-x] className [sequences.fasta] result.xml
    	 */	
    	String[] args2 = parseArgs(args);
    	if(args==null)
    		return;

    	if (args2.length < 2) {
    		printUsage();
    		return;
    	}
    	
    	Class analyzerClass = Class.forName(args2[0]);
    	GenotypeTool genotypeTool = (GenotypeTool) analyzerClass.getConstructor(File.class).newInstance(new File(workingDir));

    	if (args2.length == 3) {
    		genotypeTool.analyze(args2[1], args2[2]);
    	} else
    		genotypeTool.analyzeSelf(args2[1]);
    }

	public static String getXmlBasePath() {
		return xmlBasePath;
	}
}
