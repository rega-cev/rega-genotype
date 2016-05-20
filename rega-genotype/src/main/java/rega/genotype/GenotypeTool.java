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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.InvocationTargetException;

import org.jdom.JDOMException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import rega.genotype.BlastAnalysis.Region;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.data.table.AbstractDataTableGenerator;
import rega.genotype.data.table.SequenceFilter;
import rega.genotype.ui.viruses.generic.GenericDefinition;
import rega.genotype.util.CsvDataTable;
import rega.genotype.util.DataTable;
import rega.genotype.utils.Settings;
import rega.genotype.viruses.generic.GenericTool;

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
	public enum AnalysesType {BlastOnly, Full}
    protected File workingDir = new File("."); // work dir is a new dir inside the job dir that contains all the data for current analyze.

    private GenotypeTool parent;
    private ResultTracer tracer;
	private ToolConfig toolConfig;

    /**
     * @param toolId organism url path component
     */
    public GenotypeTool(String url, File workingDir) {
    	this(Settings.getInstance().getConfig().getToolConfigByUrlPath(url), workingDir);
    }

    public GenotypeTool(ToolConfig toolConfig, File workingDir) {
    	this.toolConfig = toolConfig;
		this.parent = null;
    	this.workingDir = workingDir;
    }

    private static class ArgsParseResult {
    	String[] remainingArgs;
    	String workingDir;
    }
    
    private static ArgsParseResult parseArgs(String[] args) { 
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option helpOption = parser.addBooleanOption('h', "help");
        CmdLineParser.Option configPathOption = parser.addStringOption('c', "config");
        CmdLineParser.Option workingDirOption = parser.addStringOption('w', "workingDir");
        
        try {
        	parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
        	e.printStackTrace();
            System.err.println(e.getMessage());
            printUsage();
            return null;
        }
        
        if (parser.getOptionValue(helpOption) == Boolean.TRUE) {
            printUsage();
            return null;
        }
        
        ArgsParseResult result = new ArgsParseResult();

        String configPath = (String) parser.getOptionValue(configPathOption);
        if (configPath == null)
        	Settings.initSettings(Settings.getInstance(null));
        else
        	Settings.initSettings(new Settings(new File(configPath)));

        String workingDirTmp = (String) parser.getOptionValue(workingDirOption);        
        if (workingDirTmp != null)
        	result.workingDir = workingDirTmp;
        else
        	result.workingDir = ".";

        result.remainingArgs = parser.getRemainingArgs();
        
        return result;
	}

    private static void printUsage() {
		System.err.println("GenotypeTool: error parsing command-line.");
		System.err.println("usage: GenotypeTool [-c config] [-w workingDir] url [xml|csv] sequences.fasta result.xml");
		System.err.println("       GenotypeTool [-c config] [-w workingDir] url [xml|csv] SELF result.xml phylo-analysis.xml window-size step-size [analysis-id]");
		System.err.println();
		System.err.println("\tThe first option analyzes one or more sequences and writes the result to the tracefile result.xml");
		System.err.println("\tThe second option performs an internal analysis");
		System.err.println("\tUrl - url path component that defines the tool (path from config.json)");
		System.err.println();
		System.err.println("options:");
		System.err.println("\t-h,--help       print this text.");
		System.err.println("\t-c,--config     specify path to config file");
        System.err.println("\t-w,--workingDir specify path to the working directory (default .)");
	}

    public void analyze(String sequenceFile, String traceFile) throws IOException {
    	analyze(new FileInputStream(sequenceFile), traceFile, AnalysesType.Full);
    }

    public void analyze(InputStream sequenceFile, String traceFile) throws IOException {
    	analyze(sequenceFile, traceFile, AnalysesType.Full);
    }

	/**
	 * This function analyzes an input FASTA file, and writes results to a given
	 * trace file.
	 * 
	 * For each sequence in the input file, it invokes analyze(AbstractSequence)
	 */
    public void analyze(InputStream sequenceFile, String traceFile, AnalysesType analysesType) throws IOException {
        startTracer(traceFile);

        LineNumberReader reader
            = new LineNumberReader
                (new InputStreamReader(new BufferedInputStream(sequenceFile)));
        
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
                        analyze(s, analysesType);
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
     * @param region 
     */
    protected void conclude(String conclusion, String motivation, Region region) {
    	concludeRule(null,conclusion,motivation, region);
    }
    protected void concludeRule(String rule, String conclusion, String motivation, Region region) {
    	concludeRule(rule, conclusion, motivation, null, region);
    }

    /**
     * Conclude the "unassigned" conclusion.
     * @param region 
     */
    protected void conclude(String conclusion, String motivation, String id, Region region) {
    	concludeRule(null,conclusion,motivation,id, region);
    }
    protected void concludeRule(String rule, String conclusion, String motivation, String id, Region region) {
    	String s = "<conclusion type=\"unassigned\"";
    	if (id != null)
    		s += " id=" + getTracer().quote(id);
    	if (region != null)
    		s += " region=" + getTracer().quote(region.getName());    	
    	s += ">";

    	getTracer().printlnOpen(s);
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
     * @param region 
     */
    protected void conclude(AbstractAnalysis.Concludable conclusion, String motivation, Region region) {
    	concludeRule(null,conclusion,motivation, region);
    }
    protected void concludeRule(String rule, AbstractAnalysis.Concludable conclusion, String motivation, Region region) {
    	concludeRule(rule, conclusion, motivation, null, region);
    }

    /**
     * Conclude a plain conclusion.
     */
    protected void conclude(AbstractAnalysis.Concludable conclusion, CharSequence motivation, String id, Region region) {
    	concludeRule(null,conclusion,motivation,id, region);
    }
    protected void concludeRule(String rule, AbstractAnalysis.Concludable conclusion, CharSequence motivation, String id, Region region) {
    	String s = "<conclusion type=\"simple\"";
    	if (id != null)
    		s += " id=" + getTracer().quote(id);
    	if (region != null)
    		s += " region=" + getTracer().quote(region.getName());    	
    	s += ">";
 
        getTracer().printlnOpen(s);
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

    public void analyze(AbstractSequence s) throws AnalysisException {
    	analyze(s, AnalysesType.Full);
    }

    /**
     * Abstract function that analyzes a sequence.
     * 
     * You should reimplement this sequence to create a new genotyping tool.
     */
    abstract public void analyze(AbstractSequence s, AnalysesType analysisType) throws AnalysisException;

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
        return new AlignmentAnalyses(new File(file), this, workingDir);
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
    	
    	if (parseArgsResult.remainingArgs == null ||
    			parseArgsResult.remainingArgs.length < 5) {
    		printUsage();
    		return;
    	}
    	 
    	Class<?> analyzerClass = Class.forName(parseArgsResult.remainingArgs[0]);
    	String url = parseArgsResult.remainingArgs[1];
    	String csv = parseArgsResult.remainingArgs[2];
    	
    	ToolConfig toolConfig = Settings.getInstance().getConfig().getToolConfigByUrlPath(url);
    	if (toolConfig == null) {
    		System.err.println("Tool with url path " + url + "could not be found.");
    	}
    	
    	String sequenceFile = toolConfig.getConfiguration() + parseArgsResult.remainingArgs[3];
    	String traceFile = toolConfig.getConfiguration() + parseArgsResult.remainingArgs[4];
    	GenotypeTool genotypeTool = (GenotypeTool) analyzerClass.getConstructor(String.class, File.class).
    			newInstance(url, new File(parseArgsResult.workingDir));

    	if (parseArgsResult.remainingArgs.length == 5) {
    		// GenotypeTool [...] className sequences.fasta result.xml
    		genotypeTool.analyze(sequenceFile, traceFile);
    	} else if (parseArgsResult.remainingArgs.length == 8 || parseArgsResult.remainingArgs.length == 9) {
    		// GenotypeTool [...] className SELF result.xml phylo-analysis.xml window-size step-size [analysis-id]");
    		String analysisFile = toolConfig.getConfiguration() + parseArgsResult.remainingArgs[5];
    		int windowSize = Integer.parseInt(parseArgsResult.remainingArgs[6]);
    		int stepSize = Integer.parseInt(parseArgsResult.remainingArgs[7]);
    		String analysisId = null;
   			if (parseArgsResult.remainingArgs.length == 9)
   				analysisId = parseArgsResult.remainingArgs[8];
   	        genotypeTool.startTracer(traceFile);
   	        try {
   	    		genotypeTool.analyzeSelf(traceFile, analysisFile, windowSize, stepSize, analysisId);
   			} catch (AnalysisException e) {
   				e.printStackTrace();
   				System.err.println(e.getMessage());
   			}
   	        genotypeTool.stopTracer();
    	} else
    		printUsage();
    	
    	if(csv.equalsIgnoreCase("csv") && (parseArgsResult.remainingArgs.length == 5 ||
    		parseArgsResult.remainingArgs.length == 8 ||
    		parseArgsResult.remainingArgs.length == 9)) {
    		
    		if (!(genotypeTool instanceof GenericTool)) {
    			System.err.println("Not implemented: internal analysis is implemented only for generic tool.");
    			printUsage();
    			return;
    		}
    			
    		
    		DataTable t = new CsvDataTable(System.out, ',', '"');
    		
    		GenericDefinition genericDefinition;
			try {
				genericDefinition = new GenericDefinition(toolConfig);
			} catch (JDOMException e1) {
				e1.printStackTrace();
				return;
			}
    		
    		SequenceFilter sequenceFilter = new SequenceFilter() {
    			public boolean excludeSequence(GenotypeResultParser parser) {
    				return false;
    			}
    		};
    		AbstractDataTableGenerator acsvgen = genericDefinition.getDataTableGenerator(sequenceFilter , t);
    		try {
				acsvgen.parse(new InputSource(new FileReader(traceFile)));
			} catch (SAXException e) {
				System.err.println(e.getMessage());
			}
    	}
    }

	
	public String getXmlPathAsString() {
		return toolConfig.getConfiguration();
	}

	public File getWorkingDir() {
		return workingDir;
	}

	public ToolConfig getToolConfig() {
		return toolConfig;
	}
}