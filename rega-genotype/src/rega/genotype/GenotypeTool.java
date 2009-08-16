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

/*
 * Created on Jul 6, 2004
 */

/**
 * @author kdforc0
 */
public abstract class GenotypeTool {
    private static String xmlBasePath = ".";
    private static String workingDir = ".";

    private GenotypeTool parent;
    private ResultTracer tracer;
    private String[] args;

    public GenotypeTool() {
    	this.parent = null;
    	this.args = null;
    }
    
    public GenotypeTool(String[] args) {
        this.parent = null;
        parseArgs(args);
    }

    static private String[] parseArgs2(String[] args) { 
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
            System.exit(2);
        }
        
        if (parser.getOptionValue(helpOption) == Boolean.TRUE) {
            printUsage();
            System.exit(0);
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

    private void parseArgs(String[] args) { 
        this.args = parseArgs2(args);
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
	
    public void analyze(String sequenceFile, String traceFile) throws IOException {
        startTracer(traceFile);

        LineNumberReader reader
            = new LineNumberReader
                (new InputStreamReader(new BufferedInputStream(new FileInputStream(sequenceFile))));
        
        try {
			for (;;) {
			    Sequence s = SequenceAlignment.readFastaFileSequence(reader);
			    if (s != null) {
                    s.removeGaps();

			        try {
			            System.err.println("Starting analysis of: " + s.getName());
			            long start = System.currentTimeMillis();

                        analyze(s);
                        tracer.flush();
 
			            long elapsedTimeMillis = System.currentTimeMillis()-start;
			            float elapsedTimeSec = elapsedTimeMillis/1000F;
			            
			            System.err.println("Completed analysis of: " + s.getName() + " (took " + elapsedTimeSec + "s)");
			        } catch (AnalysisException e) {
			            System.err.println(e.getMessage());
			            e.printStackTrace();
			            tracer.printError(e);
			        }
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

    /**
     * 
     */
    protected void stopTracer() {
        getTracer().finish();
    }

    /**
     * @param traceFile
     * @throws FileNotFoundException
     */
    protected void startTracer(String traceFile) throws FileNotFoundException {
        tracer = new ResultTracer(new File(traceFile));
    }

    protected void conclude(String conclusion, String motivation) {
        getTracer().printlnOpen("<conclusion type=\"unassigned\">");
        getTracer().printlnOpen("<assigned>");
        getTracer().add("id", "Unassigned");
        getTracer().add("name", (String) conclusion);
        getTracer().printlnClose("</assigned>");
        getTracer().add("motivation", motivation);
        getTracer().printlnClose("</conclusion>");    	
    }

    protected void conclude(AbstractAnalysis.Concludable conclusion, String motivation) {
        getTracer().printlnOpen("<conclusion type=\"simple\">");
        conclusion.writeConclusion(getTracer());
        getTracer().add("motivation", motivation);
        getTracer().printlnClose("</conclusion>");
    }

    protected void conclude(AbstractAnalysis.Concludable major, AbstractAnalysis.Concludable minor,
    						String motivation) {
        getTracer().printlnOpen("<conclusion type=\"composed\">");
        AbstractAnalysis.ComposedConclusion r = new AbstractAnalysis.ComposedConclusion(major, minor);
        r.writeConclusion(getTracer());
        getTracer().add("motivation", motivation);
        getTracer().printlnClose("</conclusion>");    	
    }
    
    abstract public void analyze(AbstractSequence s) throws AnalysisException;
    abstract public void analyzeSelf() throws AnalysisException;
    
    /**
     * @return Returns the args.
     */
    public String[] getArgs() {
        return args;
    }

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
     * @return Returns the parent.
     */
    public GenotypeTool getParent() {
        return parent;
    }

    /**
     * @param parent The parent to set.
     */
    public void setParent(GenotypeTool parent) {
        this.parent = parent;
    }
    
    public static void main(String[] args)
    	throws IOException, ParameterProblemException, FileFormatException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException {
    	
    	/*
    	 * Usage: GenotypeTool [-p,-c,-x] className [sequences.fasta] result.xml
    	 */	
    	String[] args2 = parseArgs2(args);

    	if (args2.length < 2) {
    		printUsage();
    		System.exit(2);
    	}
    	
    	Class analyzerClass = Class.forName(args2[0]);
    	GenotypeTool genotypeTool = (GenotypeTool) analyzerClass.getConstructor(File.class).newInstance(new File(workingDir));

    	genotypeTool.args = args2;

    	if (args2.length == 3) {
    		genotypeTool.analyze(args2[1], args2[2]);
    	} else
    		genotypeTool.analyzeSelf(args2[1]);
    }
}
