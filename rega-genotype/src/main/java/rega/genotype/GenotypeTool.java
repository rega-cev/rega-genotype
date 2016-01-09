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

import rega.genotype.utils.Settings;

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
    protected File workingDir = new File(".");

    private GenotypeTool parent;
    private ResultTracer tracer;

    public GenotypeTool() {
    	this.parent = null;
    }

    private static class ArgsParseResult {
    	String[] remainingArgs;
    	String workingDir;
    }
    
    private static ArgsParseResult parseArgs(String[] args) { 
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
        
        ArgsParseResult result = new ArgsParseResult();
        
        Settings.initSettings(Settings.getInstance());
        
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
        	result.workingDir = workingDirTmp;
        else
        	result.workingDir = ".";
        
        String treeGraphCmd = (String) parser.getOptionValue(treeGraphCmdOption);        
        if (treeGraphCmd != null)
        	Settings.treeGraphCommand = treeGraphCmd;

        result.remainingArgs = parser.getRemainingArgs();
        
        return result;
	}

    private static void printUsage() {
		System.err.println("GenotypeTool: error parsing command-line.");
		System.err.println("usage: GenotypeTool [-p pauppath] [-c clustalpath] [-x xmlpath] analysis sequences.fasta result.xml");
		System.err.println("       GenotypeTool [-p pauppath] [-c clustalpath] [-x xmlpath] analysis SELF result.xml phylo-analysis.xml window-size step-size [analysis-id]");
		System.err.println();
		System.err.println("\tThe first option analyzes one or more sequences and writes the result to the tracefile result.xml");
		System.err.println("\tThe second option performs an internal analysis");
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
				if (cancelAnalysis()) {
					System.err.println("Cancelled job: " + currentJob());
					break;
				}
				
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
		} finally {
			stopTracer();
		}
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
    	concludeRule(null,conclusion,motivation);
    }
    protected void concludeRule(String rule, String conclusion, String motivation) {
    	concludeRule(rule, conclusion, motivation, null);
    }

    /**
     * Conclude the "unassigned" conclusion.
     */
    protected void conclude(String conclusion, String motivation, String id) {
    	concludeRule(null,conclusion,motivation,id);
    }
    protected void concludeRule(String rule, String conclusion, String motivation, String id) {
        getTracer().printlnOpen("<conclusion type=\"unassigned\""
        		+ (id != null ? " id=\"" + id + "\"" : "") + ">");
        getTracer().printlnOpen("<assigned>");
        getTracer().add("id", "Unassigned");
        getTracer().add("name", (String) conclusion);
        getTracer().printlnClose("</assigned>");
        getTracer().add("motivation", motivation);
    	getTracer().add("rule", rule==null ? "":rule);
        getTracer().printlnClose("</conclusion>");    	
    }

    /**
     * Conclude a plain conclusion.
     */
    protected void conclude(AbstractAnalysis.Concludable conclusion, String motivation) {
    	concludeRule(null,conclusion,motivation);
    }
    protected void concludeRule(String rule, AbstractAnalysis.Concludable conclusion, String motivation) {
    	concludeRule(rule, conclusion, motivation, null);
    }

    /**
     * Conclude a plain conclusion.
     */
    protected void conclude(AbstractAnalysis.Concludable conclusion, CharSequence motivation, String id) {
    	concludeRule(null,conclusion,motivation,id);
    }
    protected void concludeRule(String rule, AbstractAnalysis.Concludable conclusion, CharSequence motivation, String id) {
        getTracer().printlnOpen("<conclusion type=\"simple\""
        		+ (id != null ? " id=\"" + id + "\"" : "") + ">");
        conclusion.writeConclusion(getTracer());
        getTracer().add("motivation", motivation);
    	getTracer().add("rule", rule==null ? "":rule);
        getTracer().printlnClose("</conclusion>");
    }

    /**
     * Conclude a combined major/minor conclusion.
     */
    protected void conclude(AbstractAnalysis.Concludable major, AbstractAnalysis.Concludable minor,
			String motivation) {
    	concludeRule(null,major,minor,motivation);
    }
    protected void concludeRule(String rule, AbstractAnalysis.Concludable major, AbstractAnalysis.Concludable minor,
    						String motivation) {
        getTracer().printlnOpen("<conclusion type=\"composed\">");
        AbstractAnalysis.ComposedConclusion r = new AbstractAnalysis.ComposedConclusion(major, minor);
        r.writeConclusion(getTracer());
        getTracer().add("motivation", motivation);
        getTracer().add("rule", rule==null ? "":rule);
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
    public void analyzeSelf(String traceFile, String analysisFile, int windowSize, int stepSize, String analysisId) throws AnalysisException
    {
    	throw new RuntimeException("analyzeSelf() is not implemented for this TypingTool");
    }

    abstract protected String currentJob();
    
    abstract protected boolean cancelAnalysis();
    
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
    
	public static void main(String[] args)
    	throws IOException, ParameterProblemException, FileFormatException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException {
    	
    	ArgsParseResult parseArgsResult = parseArgs(args);
    	if (parseArgsResult.remainingArgs == null)
    		return;

    	if (parseArgsResult.remainingArgs.length < 3) {
    		printUsage();
    		return;
    	}
    	
    	Class<?> analyzerClass = Class.forName(parseArgsResult.remainingArgs[0]);
    	String sequenceFile = parseArgsResult.remainingArgs[1];
    	String traceFile = parseArgsResult.remainingArgs[2];
    	GenotypeTool genotypeTool = (GenotypeTool) analyzerClass.getConstructor(File.class).newInstance(new File(parseArgsResult.workingDir));

    	if (parseArgsResult.remainingArgs.length == 3) {
    		// GenotypeTool [...] className sequences.fasta result.xml
    		genotypeTool.analyze(sequenceFile, traceFile);
    	} else if (parseArgsResult.remainingArgs.length == 6 || parseArgsResult.remainingArgs.length == 7) {
    		// GenotypeTool [...] className SELF result.xml phylo-analysis.xml window-size step-size [analysis-id]");
    		String analysisFile = parseArgsResult.remainingArgs[3];
    		int windowSize = Integer.parseInt(parseArgsResult.remainingArgs[4]);
    		int stepSize = Integer.parseInt(parseArgsResult.remainingArgs[5]);
    		String analysisId = null;
   			if (parseArgsResult.remainingArgs.length == 7)
   				analysisId = parseArgsResult.remainingArgs[6];
   	        genotypeTool.startTracer(traceFile);
   	        try {
   	    		genotypeTool.analyzeSelf(traceFile, analysisFile, windowSize, stepSize, analysisId);
   			} catch (AnalysisException e) {
   				System.err.println(e.getMessage());
   			}
   	        genotypeTool.stopTracer();
    	} else
    		printUsage();
    }

	public static String getXmlBasePath() {
		return xmlBasePath;
	}
}
