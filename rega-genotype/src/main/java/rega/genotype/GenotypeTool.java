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
import rega.genotype.BlastAnalysis.Result;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.data.table.AbstractDataTableGenerator;
import rega.genotype.data.table.SequenceFilter;
import rega.genotype.ngs.NgsAnalysis;
import rega.genotype.ngs.NgsFileSystem;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.viruses.generic.GenericDefinition;
import rega.genotype.util.CsvDataTable;
import rega.genotype.util.DataTable;
import rega.genotype.utils.FileUtil;
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
    	String ngsPairedEndFile1 = null;
    	String ngsPairedEndFile2 = null;
    	String ngsPairedEndFilesList = null;
    	Boolean assembleOnly = false; 
    }
    
    private static ArgsParseResult parseArgs(String[] args) { 
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option helpOption = parser.addBooleanOption('h', "help");
        CmdLineParser.Option configPathOption = parser.addStringOption('c', "config");
        CmdLineParser.Option workingDirOption = parser.addStringOption('w', "workingDir");
        CmdLineParser.Option pe1Option = parser.addStringOption("paired-end-1");
        CmdLineParser.Option pe2Option = parser.addStringOption("paired-end-2");
        CmdLineParser.Option assembleOnlyOption = parser.addStringOption("assemble-only");
        CmdLineParser.Option ngsFilesOption = parser.addStringOption("paired-end-files-list");

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

        String pe1 = (String) parser.getOptionValue(pe1Option);
        if (pe1 != null)
        	result.ngsPairedEndFile1 = pe1;

        String pe2 = (String) parser.getOptionValue(pe2Option);
        if (pe2 != null)
        	result.ngsPairedEndFile2 = pe2;

        String ngsFiles = (String) parser.getOptionValue(ngsFilesOption);
        if (ngsFiles != null)
        	result.ngsPairedEndFilesList = ngsFiles;

        String assembleOnly = (String) parser.getOptionValue(assembleOnlyOption);
        if (assembleOnly != null && assembleOnly.equals("true"))
        	result.assembleOnly = true;

        result.remainingArgs = parser.getRemainingArgs();

        return result;
    }

    private static void printUsage() {
		System.err.println("GenotypeTool: error parsing command-line.");
		System.err.println("usage: GenotypeTool [-c config] [-w workingDir] url [xml|csv] sequences.fasta result.xml");
		System.err.println("       GenotypeTool [-c config] [-w workingDir] url [xml|csv] NGS [-paired-end-1] [-paired-end-2] result.xml");
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
        System.err.println("\t NGS Options:");
        System.err.println("\t--paired-end-1  Analysis input NGS fastq paired-end 1 file absolute path, Only for NGS (in that case paired-end-2 must not be empty) .)");
        System.err.println("\t--paired-end-2  Analysis input NGS fastq paired-end 2 file absolute path, Only for NGS (in that case paired-end-1 must not be empty) .)");
        System.err.println("\t--assemble-only NGS: Do only the assembly step. All the previuse steps must be done in workingDir. This is useful only to test the assembly step.");
        System.err.println("\t--paired-end-files-list File with many pair-end secunce files names. namescan be used to run many ngs analysis 1 after the other. " +
        		"\tFormat: " +
        		"\tDIR_NAME={this analysis dir relative to workingDir (will be auto created)}" +
        		"\tPE1={paired-end-1 absolute path}" +
        		"\tPE2={paired-end-2 absolute path}" +
        		"..");
	}

    public void analyze(String sequenceFile, String traceFile) throws IOException {
    	analyze(new FileInputStream(sequenceFile), traceFile);
    }

	/**
	 * This function analyzes an input FASTA file, and writes results to a given
	 * trace file.
	 * 
	 * For each sequence in the input file, it invokes analyze(AbstractSequence)
	 */
    public void analyze(InputStream sequenceFile, String traceFile) throws IOException {
        startTracer(traceFile);

        LineNumberReader reader
            = new LineNumberReader
                (new InputStreamReader(new BufferedInputStream(sequenceFile)));

        try {
        	formatDB();
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
		} catch (ApplicationException e1) {
			System.err.println(e1.getMessage());
			e1.printStackTrace();
			tracer.printError(e1);
		} finally {
			stopTracer();
		}
    }

    public void stopTracer() {
        getTracer().finish();
    }

    public void startTracer(String traceFile) throws FileNotFoundException {
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
     * @param id: can be type or subtype 
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

    protected void conclude(BlastAnalysis blastAnalysis, Result blastResult) {
        if (blastResult.haveSupport() && blastResult.getConcludedCluster() != null) {
 			if (blastAnalysis.getAbsCutoff() != null && blastAnalysis.getRelativeCutoff() != null)
   				conclude(blastResult, "Assigned based on BLAST absolute score &gt;= " + blastAnalysis.getAbsCutoff() 
   						+ " and relative score &gt;= " + blastAnalysis.getRelativeCutoff(), null);
   			else if (blastAnalysis.getAbsCutoff() != null)
   				conclude(blastResult, "Assigned based on BLAST absolute score &gt;= " + blastAnalysis.getAbsCutoff(), null); 
   			else if (blastAnalysis.getRelativeCutoff() != null)
   				conclude(blastResult, "Assigned based on BLAST relative score &gt;= " + blastAnalysis.getAbsCutoff(), null);

        } else {
           	if (blastAnalysis.getAbsCutoff() != null && blastAnalysis.getRelativeCutoff() != null)
   				conclude("Unassigned", "Unassigned based on BLAST absolute score &gt;= " + blastAnalysis.getAbsCutoff() 
   						+ " and relative score &gt;= " + blastAnalysis.getRelativeCutoff(), null);
   			else if (blastAnalysis.getAbsCutoff() != null)
   				conclude("Unassigned", "Unassigned based on BLAST absolute score &gt;= " + blastAnalysis.getAbsCutoff(), null); 
   			else if (blastAnalysis.getRelativeCutoff() != null)
   				conclude("Unassigned", "Unassigned based on BLAST relative score &gt;= " + blastAnalysis.getAbsCutoff(), null);
        }
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
     * Should be used to init the environment for analyzing. (for blast format-db command) 
     * Format db will be call at the start of analyze.
     */
    abstract protected void formatDB() throws ApplicationException;
    
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
    		System.err.println("Tool with url path " + url
    				+ " could not be found.");
    		return;
    	}

    	String sequenceFile = toolConfig.getConfiguration() + parseArgsResult.remainingArgs[3];
    	String traceFile = parseArgsResult.workingDir + File.separator + parseArgsResult.remainingArgs[4];
    	GenotypeTool genotypeTool = (GenotypeTool) analyzerClass.getConstructor(String.class, File.class).
    			newInstance(url, new File(parseArgsResult.workingDir));

    	if (parseArgsResult.remainingArgs.length == 5) {
    		if (parseArgsResult.remainingArgs[3].equals("NGS")) {
    			if (parseArgsResult.ngsPairedEndFilesList != null) {
    				// GenotypeTool [...] className [-paired-end-files-list] result.xml
    				File workDir = new File(parseArgsResult.workingDir);
    				File ngsPairedEndFilesList = new File(parseArgsResult.ngsPairedEndFilesList);
    				if (!ngsPairedEndFilesList.exists()) {
    					System.err.println();
    					System.err.println("-paired-end-files-list file not found");
    					System.err.println();
    					printUsage();
    					return;
    				}

    				int rowNum = 0;
    				for (String[] row: FileUtil.readCSV(ngsPairedEndFilesList)){
    					rowNum++;
    					if (rowNum == 1)
    						continue; // header.
    					if (row.length != 3) {
    						System.err.println();
    						System.err.println("-paired-end-files-list file format error");
    						System.err.println();
    						printUsage();
    						return;
    					}
    					File currentWorkDir = new File(workDir, row[0]);
    					File pe1 = new File(row[1]);
    					File pe2 = new File(row[2]);

    					if (!NgsFileSystem.addFastqFiles(currentWorkDir, pe1, pe2)) {
    						System.err.println();
    						System.err.println("-paired-end-files-list pe file not found ");
    						System.err.println();
    						printUsage();
    						return;
    					}
    					NgsAnalysis ngsAnalysis = new NgsAnalysis(currentWorkDir);
    					ngsAnalysis.analyze();

    					genotypeTool.analyze(
    							currentWorkDir.getAbsolutePath() + File.separator + NgsFileSystem.SEQUENCES_FILE, 
    							traceFile);
    				}
    			} else {
    				// GenotypeTool [...] className [-paired-end-1][-paired-end-2] result.xml
    				if (!parseArgsResult.assembleOnly &&
    						(parseArgsResult.ngsPairedEndFile1 == null
    						|| parseArgsResult.ngsPairedEndFile2 == null)){
    					printUsage();
    					return;
    				}

    				File workDir = new File(parseArgsResult.workingDir);
    				if (!parseArgsResult.assembleOnly &&
    						!NgsFileSystem.addFastqFiles(workDir, 
    								new File(parseArgsResult.ngsPairedEndFile1), 
    								new File(parseArgsResult.ngsPairedEndFile2))){
    					System.err.println();
    					System.err.println("Check that ngs paired end files are ok!");
    					System.err.println();
    					printUsage();
    					return;
    				}

    				NgsAnalysis ngsAnalysis = new NgsAnalysis(workDir);
    				if (parseArgsResult.assembleOnly)
    					ngsAnalysis.assembleAll();
    				else
    					ngsAnalysis.analyze();

    				genotypeTool.analyze(
    						parseArgsResult.workingDir + File.separator + NgsFileSystem.SEQUENCES_FILE, 
    						traceFile);
    			}

    		} else {
    			// GenotypeTool [...] className sequences.fasta result.xml
    			genotypeTool.analyze(sequenceFile, traceFile);
    		}
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