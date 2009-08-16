/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rega.genotype.AlignmentAnalyses.Cluster;

/**
 * Implements a clustering analysis using phylogenetic methods:
 *  - paup or mrbayes for phylogeny estimation and bootstrapping
 *  - tree puzzle for phylogenetic signal analysis
 *  
 * @author koen
 */
public class PhyloClusterAnalysis extends AbstractAnalysis {
    private static final String PAUP_LOG       = "paup.log";
    private static final String PAUP_TREE      = "paup.tre";
    private static final String PAUP_ALIGNMENT = "paup.nex";
    private static final String PAUP_BACKBONE  = "${BACKBONE-CLUSTERS}";
    private static final String PUZZLE_LMA     = "infile.svg";
    private static final String PUZZLE_REPORT  = "infile.puzzle";
    private static final String PUZZLE_DIST    = "infile.dist";
    static private final int MRBAYES_ANALYSIS  = 0;
    static private final int PAUP_ANALYSIS     = 1;
  
    static public String mrBayesCommand = "mrbayes";
    static public String paupCommand = "paup";
    static public String puzzleCommand = "puzzle";

    private List<Cluster> clusters;
    private Double cutoff;
    private String commandBlock;
    private Map<String, Float> patterns;
    public PhyloClusterAnalysis(AlignmentAnalyses owner,
                                String id, List<Cluster> clusters, String paupBlock, Double cutoff,
                                File workingDir) {
        super(owner, id);
        this.clusters = clusters;
        this.commandBlock = paupBlock;
        this.cutoff = cutoff;
        this.workingDir = workingDir;
    }

    public class Result extends AbstractAnalysis.Result implements Scannable, Concludable {
        private Cluster bestCluster;
        private float   support;
        private float   supportOuter;  // cluster without query sequence
        private float   supportInner;  // parts of cluster with query sequence        
        private Double  signal;

        private String paupLog;
        private String alignment;
        private String tree;
        private String puzzle;

        List<Double> allSupports;
        
        public Result(AbstractSequence sequence, Cluster cluster, float support,
                      float supportOuter, float supportInner, Double signal,
                      List<Double> allSupports, String paupLog, String alignment,
                      String tree, String puzzle) {
            super(sequence);
            this.bestCluster = cluster;
            this.support = support;
            this.supportInner = supportInner;
            this.supportOuter = supportOuter;
            this.signal = signal;
            this.paupLog = paupLog;
            this.alignment = alignment;
            this.tree = tree;
            this.puzzle = puzzle;
            this.allSupports = allSupports;
        }

        public List<Double> getAllSupports() {
            return allSupports;
        }

        public float getSupportInner() {
            return supportInner;
        }

        public float getSupportOuter() {
            return supportOuter;
        }

        public List<String> scanLabels() {
            List<String> result = new ArrayList<String>();
            for (int i = 0; i < clusters.size(); ++i) {
                result.add(clusters.get(i).getId());
            }
            
            if (signal != null)
                result.add("signal");
            
            return result;
        }
        
		public List<String> scanDiscreteLabels() {
            List<String> result = new ArrayList<String>();

          	result.add("best");
           	result.add("assigned");
            
            return result;
        }

        public List<Double> scanValues() {
            List<Double> result = new ArrayList<Double>();
            for (int i = 0; i < allSupports.size(); ++i) {
                result.add(allSupports.get(i));
            }            
            
            if (signal != null)
                result.add(signal);
            
            return result;
        }
        
        public List<String> scanDiscreteValues() {
            List<String> result = new ArrayList<String>();

           	result.add(bestCluster.getId());
           	if (haveSupport())
           		result.add(bestCluster.getId());
           	else
           		result.add(null);

            return result;
        }

        public void writeXML(ResultTracer tracer) {
            writeXMLBegin(tracer);
            tracer.printlnOpen("<best>");
            tracer.add("id", bestCluster.getId());
            tracer.add("name", bestCluster.getName());
            tracer.add("support", getSupport());
            if (bestCluster.getDescription() != null) {
                tracer.add("description", bestCluster.getDescription());
            }

            if (haveOption("inner"))
                tracer.add("inner", getSupportInner());
            
            if (haveOption("outer"))
                tracer.add("outer", getSupportOuter());
            tracer.printlnClose("</best>");
            
            Cluster c = getSpecificCluster();
            
            if (c != null) {
                tracer.printlnOpen("<specific>");
                tracer.add("id", c.getId());
                tracer.add("name", c.getName());
                tracer.add("support", getSupport(c));
                if (c.getDescription() != null) {
                    tracer.add("description", c.getDescription());
                }
                
                if (haveOption("inner"))
                    tracer.add("inner", getSupportInner());
                
                if (haveOption("outer"))
                    tracer.add("outer", getSupportOuter());
                tracer.printlnClose("</specific>");
            }

            if (signal != null)
                tracer.add("signal", getSignal());

            if (alignment != null)
                tracer.add("alignment", alignment);
            
            if (paupLog != null)
                tracer.add("log", paupLog);
            
            if (tree != null)
                tracer.add("tree", tree);
            
            if (puzzle != null)
                tracer.add("puzzle", puzzle);
            
            writeXMLEnd(tracer);
        }

        private double getSupport(Cluster c) {
            return allSupports.get(clusters.indexOf(c));
        }

        private Cluster getSpecificCluster() {
            Cluster result = null;
            float   resultSupport = 0;

            if (cutoff != null) {
                for (int i = 0; i < allSupports.size(); ++i) {
                    if (allSupports.get(i) >= cutoff) {
                        if ((result == null)
                            || (result.depth() < clusters.get(i).depth())
                            || ((result.depth() == clusters.get(i).depth())
                                && (resultSupport < allSupports.get(i)))) {
                            result = clusters.get(i);
                            resultSupport = allSupports.get(i).floatValue();
                        }
                    }
                }
            }

            return result;
        }

        public float getSignal() {
            return signal.floatValue();
        }

        public Cluster getBestCluster() {
            return bestCluster;
        }

        public boolean haveSupport() {
            if (cutoff == null)
                return false;
            else
                return support >= cutoff;
        }
        
        public float getSupport() {
            return support;
        }

        public void writeConclusion(ResultTracer tracer) {
            Cluster c = getSpecificCluster();

            if (c == null)
            	c = getBestCluster();
            
            writeConclusion(tracer, c);
        }

        public void writeConclusion(ResultTracer tracer, Cluster c) {
            tracer.printlnOpen("<assigned>");
            tracer.add("id", c.getId());
            tracer.add("name", c.getName());
            tracer.add("support", getSupport(c));
            if (c.getDescription() != null) {
               tracer.add("description", c.getDescription());
            }
            if (haveOption("inner"))
                tracer.add("inner", getSupportInner());
            
            if (haveOption("outer"))
                tracer.add("outer", getSupportOuter());

            tracer.printlnClose("</assigned>");
        }

		public Cluster getConcludedCluster() {
			return getSpecificCluster();
		}

		public float getConcludedSupport() {
			return (float) getSupport(getConcludedCluster());
		}

		public Concludable concludeForCluster(final Cluster cluster) {
			return new Concludable() {
				public Cluster getConcludedCluster() {
					return cluster;
				}

				public float getConcludedSupport() {
					return (float) getSupport(cluster);
				}

				public void writeConclusion(ResultTracer tracer) {
					Result.this.writeConclusion(tracer, cluster);
				}	
			};
		}

    }

	private boolean runMrBayes(SequenceAlignment a, File outputDir,
						       String mrBayesText) throws ApplicationException {
		File nexFile = writeNexusFile(a, outputDir, mrBayesText, true);

		Runtime runtime = Runtime.getRuntime();
		Process bayes = null;
		try {
			/*
			 * Run MrBayes
			 */
			String cmds[] = { mrBayesCommand, nexFile.getAbsolutePath()};
			System.err.println("Starting: " + cmds[0] + " " + cmds[1]);

			if (outputDir.isDirectory())
				bayes = runtime.exec(cmds, null, outputDir);
			else
				bayes = runtime.exec(cmds, null);

			OutputStream output = bayes.getOutputStream();
			output.write('\n');
			output.flush();
            output.close();
			int result = bayes.waitFor();

			if (result != 0) {
				/*
				 * Apparently MrBayes always exits with error code 1
				 * Duh!
				 */
				//throw new ApplicationException("MrBayes exited with error: " + result);
			}
		} catch (InterruptedIOException e) {
			bayes.destroy();
			return false;
		} catch (IOException e) {
			if (bayes != null)
				bayes.destroy();
			throw new ApplicationException("Error: I/O Error while invoking MrBayes: "
				+ e.getMessage());
		} catch (InterruptedException e) {
			bayes.destroy();
			return false;
		}

		return true;
	}

    private boolean runPaup(SequenceAlignment a, File outputDir, String paupText) throws ApplicationException {
		File nexFile = writeNexusFile(a, outputDir, paupText, true);
		Runtime runtime = Runtime.getRuntime();
		Process paup = null;

		try {
			/*
			 * Run paup
			 */
			String cmd = paupCommand + " -n " + nexFile.getAbsolutePath();

            System.err.println(cmd);
			paup = runtime.exec(cmd, null, outputDir);

            InputStream stderr = paup.getErrorStream();
            InputStreamReader isr = new InputStreamReader(stderr);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ( (line = br.readLine()) != null)
            	System.err.println(line);
            br.close();
            
            int result = paup.waitFor();

            paup.getErrorStream().close();
            paup.getInputStream().close();
            paup.getOutputStream().close();

			if (result != 0) {
				throw new ApplicationException("Paup exited with error: " + result);
			}
            
            nexFile.delete();
		} catch (InterruptedIOException e) {
			paup.destroy();
			return false;
		} catch (IOException e) {
			if (paup != null)
				paup.destroy();
			throw new ApplicationException("Error: I/O Error while invoking Paup: "
				+ e.getMessage());
		} catch (InterruptedException e) {
			paup.destroy();
			return false;
		}

		return true;
	}

	private File writeNexusFile(SequenceAlignment a, File workingDir,
			                    String appText, boolean needQuit)
			throws ApplicationException {
        File f = getTempFile("tmp.nex");

        try {
			FileOutputStream outFile = new FileOutputStream(f);
			a.writeOutput(outFile, SequenceAlignment.FILETYPE_NEXUS);
		    
			/*
			 * Append App block.
			 */
			appendAppBlock(appText, outFile, needQuit);
			//outFile.flush();
            //outFile.getFD().sync();
			outFile.close();
		
		} catch (FileNotFoundException e) {
			throw new ApplicationException("Error: Cannot open " + f.getAbsolutePath() + " for writing");
		} catch (IOException e) {
			throw new ApplicationException("Error: I/O Error while writing " + f.getAbsolutePath());
		} catch (ParameterProblemException e) {
			throw new RuntimeException(e);
		}
		return f;
	}

	private void appendAppBlock(String appText, OutputStream outputFile,
							    boolean needQuit) throws IOException {
		Writer writer = new OutputStreamWriter(outputFile);

		writer.write('\n');

        boolean haveQuit = (appText.indexOf("quit;") != -1);
		if (needQuit) {
			if (!haveQuit)
				appText = appText + "\nquit;";
		} else
			if (haveQuit)
				appText = appText.replaceAll("quit;", "");
        
        /*
         * Stupid PAUP cannot handle long filenames...
         * Therefore, we MUST be changed directory to where results need to be stored.
         *
         * appText
         *   = appText.replaceAll(PAUP_LOG, getTempFile(PAUP_LOG).getAbsolutePath())
         *      .replaceAll(PAUP_TREE, getTempFile(PAUP_TREE).getAbsolutePath())
         *      .replaceAll(PAUP_ALIGNMENT, getTempFile(PAUP_ALIGNMENT).getAbsolutePath())
         */
        
		writer.write(appText);
		writer.write('\n');
		writer.flush();
	}
	
    private static Map<String, Float> retrieveClusterSupports(int analysisMethod, File inputFile)
        throws ApplicationException {

        final class Match {
            String p;
            Float v;
            
            public Match(String p, Float v) {
                this.p = p;
                this.v = v;
            }
        };
        
        List<Match> finds1 = new ArrayList<Match>();
        List<Match> finds2 = new ArrayList<Match>();

        // FIXME: grep on undefined distance(s) and discard analysis if that occurs.
        
        try {
            LineNumberReader reader
               = new LineNumberReader(new InputStreamReader(new FileInputStream(inputFile)));

            String numberPattern = null;
            switch (analysisMethod) {
            case MRBAYES_ANALYSIS:
                numberPattern = "(?:\\s+)(?:\\d+)(?:\\s+)((\\d|\\.)+)";
                break;
            case PAUP_ANALYSIS:
                numberPattern = "(?:(?:\\s+)(\\d+))?";
            }

            Pattern p = Pattern.compile("^((?:\\*|\\.){3,})" + numberPattern);
            
            for (;;) {
                String s = reader.readLine();
                if (s == null)
                    break;
                Matcher m = p.matcher(s);
                
                if (m.find()) {
                    String value = m.group(2);
                    if (value == null)
                        finds1.add(new Match(m.group(1), null));
                    else
                        finds2.add(new Match(m.group(1), Float.valueOf(m.group(2))));
                }
            }

            Map<String,Float> result = new HashMap<String, Float>();

            if (!finds1.isEmpty()) {
                int r = finds1.size() / finds2.size();
                
                for (int i = 0; i < finds2.size(); ++i) {
                    String p1 = "";
                    for (int j = 0; j < r; ++j)
                        p1 += finds1.get((finds2.size() * j) + i).p;
            
                    result.put(p1 + finds2.get(i).p, finds2.get(i).v);
                }
            } else {
                for (int i = 0; i < finds2.size(); ++i) {            
                    result.put(finds2.get(i).p, finds2.get(i).v);
                } 
            }

            return result;
        } catch (FileNotFoundException e) {
            throw new ApplicationException(e.getMessage());
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage());
        }
    }
    
	private float retrieveResultValues(int[] taxaIndexes, int numTaxa)
        throws ApplicationException {
        
        StringBuffer posMaskBuf = new StringBuffer();
		StringBuffer negMaskBuf = new StringBuffer();

		if (taxaIndexes.length < 1) {
			throw new ApplicationException("Select 1 or more taxa");
		}

		Arrays.sort(taxaIndexes);
		int tax = 0;
		for (int i = 0; i < numTaxa; ++i) {
			if (i != taxaIndexes[tax]) {
				posMaskBuf.append('.');
				negMaskBuf.append('*');
			} else {
				posMaskBuf.append('*');
				negMaskBuf.append('.');

				// skip this, and all other duplicates in the taxaIndexes
				// list                
				while (i == taxaIndexes[tax] && tax < taxaIndexes.length - 1)
					++tax;
			}
		}

		String posMask = posMaskBuf.toString();
		String negMask = negMaskBuf.toString();
		
        if (patterns.containsKey(posMask)) {
            return patterns.get(posMask).floatValue();
        }

        if (patterns.containsKey(negMask)) {
            return patterns.get(negMask).floatValue();
        }

        return 0;
	}

	protected List<Double> retrieveClustersValues(SequenceAlignment alignment,
                                                  List<String> queryTaxa)
			throws ApplicationException {
		List<Double> results = new ArrayList<Double>();
        
		for (Iterator<Cluster> i = clusters.iterator(); i.hasNext();) {
			Cluster c = i.next();
            List<String> taxa = c.getTaxaIds();
            taxa.addAll(queryTaxa);

			results.add(new Double(retrieveResultValues(alignment, taxa)));
		}

		return results;
	}

    private float retrieveResultValues(SequenceAlignment alignment, List<String> queryTaxa)    
        throws ApplicationException {

        int[] allTaxaIndexes = new int[queryTaxa.size()];

        for (int j = 0; j < queryTaxa.size(); ++j)
            allTaxaIndexes[j] = alignment.getIndex(alignment.findSequence(queryTaxa.get(j)));

        return retrieveResultValues(allTaxaIndexes, alignment.getSequences().size());
    }

	protected Result compute(SequenceAlignment alignment, AbstractSequence sequence,
                             List<String> queryTaxa, int analysisMethod, String backboneClusters)
			throws IOException, ApplicationException {
		File bootstrapFile = null;

		switch (analysisMethod) {
		case MRBAYES_ANALYSIS:
			if (!runMrBayes(alignment, workingDir, commandBlock))
				throw new ApplicationException("internal error: weirdness running mrbayes");
			bootstrapFile = new File("analysis.parts");
			break;
		case PAUP_ANALYSIS:
			if (!runPaup(alignment, workingDir, commandBlock.replace(PAUP_BACKBONE, backboneClusters)))
				throw new ApplicationException("internal error: weirdness running mrbayes");
			bootstrapFile = getTempFile(PAUP_LOG);
		}

        patterns = retrieveClusterSupports(analysisMethod, bootstrapFile);        

        List<Double> results
            = retrieveClustersValues(alignment, queryTaxa);

        Cluster bestCluster = null;
        float bestSupport = 0;
		for (int i = 0; i < clusters.size(); ++i) {
			Cluster cluster = clusters.get(i);
			float f = results.get(i).floatValue();
			
			if (bestCluster == null || f > bestSupport) {
                bestCluster = cluster;
                bestSupport = f;
			}
		}

        float inner = 0;
        float outer = 0;
        if (haveOption("outer"))
            outer = getClusterOuter(alignment, bestCluster, bootstrapFile,
                                    analysisMethod);

        if (haveOption("inner"))
            inner = getClusterInner(alignment, queryTaxa, bestCluster, bootstrapFile,
                                    analysisMethod);

        Double signal = null;
        
        if (haveOption("signal")) {
            signal = runPuzzle(alignment);
        }

        File paupLog = getTempFile(PAUP_LOG);
        File paupTree = getTempFile(PAUP_TREE);
        File paupAlignment = getTempFile(PAUP_ALIGNMENT);
        File lmaFile = getTempFile(PUZZLE_LMA);
        
        String paupLogResource = makeResource(paupLog, "log", haveOption("log"));
        String paupAlignmentResource = makeResource(paupAlignment, "nex", haveOption("alignment"));
        String paupTreeResource = makeResource(paupTree, "tre", haveOption("tree"));
        String puzzleLMAResource = makeResource(lmaFile, "svg",
                                                haveOption("signal") && haveOption("puzzle"));
        
		Result r = new Result(sequence, bestCluster, bestSupport, outer, inner, signal, results,
                              paupLogResource, paupAlignmentResource, paupTreeResource, puzzleLMAResource);
        
        return r;
	}

    private double runPuzzle(SequenceAlignment alignment)
            throws ApplicationException, FileNotFoundException, IOException {
        File infile = getTempFile("infile");
        FileOutputStream fout = new FileOutputStream(infile);
        FileDescriptor fd = fout.getFD();
        alignment.writePhylipOutput(fout);
        fout.flush();
        fd.sync();
        fout.close();
        Runtime runtime = Runtime.getRuntime();
        Process puzzle = null;

        try {
            /*
             * Run puzzle
             */
            String cmd = puzzleCommand + " " + infile.getAbsolutePath() +" -svg";
            puzzle = runtime.exec(cmd, null, workingDir);

            InputStream puzzleOut = puzzle.getInputStream();
            OutputStream puzzleIn = puzzle.getOutputStream();
            PrintStream ps = new PrintStream(puzzleIn);

            ps.println("b\ny");
            ps.flush();
            
            InputStreamReader isr = new InputStreamReader(puzzleOut);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ( (line = br.readLine()) != null)
                System.err.println(line);
            br.close();
            
            int result = puzzle.waitFor();

            if (result != 0) {
                throw new ApplicationException("Puzzle exited with error: " + result);
            }

            File report = getTempFile(PUZZLE_REPORT);            
            
            infile.delete();
            getTempFile(PUZZLE_DIST).delete();
            
            LineNumberReader reader
               = new LineNumberReader(new InputStreamReader(new FileInputStream(report)));

            Pattern pattern = Pattern.compile("^Number of quartets in region (?:1|2|3): (\\d+).*:");
            Pattern allpattern = Pattern.compile("^Number of quartets: (\\d+)");           
            float signal = 0;
            float total = 0;
            for (;;) {
                String s = reader.readLine();
                System.err.println(s);
                if (s == null)
                    break;
                Matcher m = pattern.matcher(s);
                
                if (m.find()) {
                    signal += Float.valueOf(m.group(1));
                }

                m = allpattern.matcher(s);
                
                if (m.find()) {
                    total = Float.valueOf(m.group(1));
                }

            }
            
            report.delete();
            return (float) (signal / total);
        } catch (InterruptedIOException e) {
            puzzle.destroy();
            return -1;
        } catch (IOException e) {
            if (puzzle != null)
                puzzle.destroy();
            throw new ApplicationException("Error: I/O Error while invoking puzzle: "
                + e.getMessage());
        } catch (InterruptedException e) {
            puzzle.destroy();
            return -1;
        }

    }

    private String makeResource(File file, String suffix, boolean keep) throws IOException {
        if (keep) {
            return makeResource(file, suffix);
        } else {
            file.delete();
            return null;
        }
    }

    float getClusterOuter(SequenceAlignment alignment, Cluster cluster,
                          File inputFile, int analysisMethod)
            throws ApplicationException {
         if (cluster.getTaxa().size() >= 2) {
            return retrieveResultValues(alignment, cluster.getTaxaIds());
        } else {
            return 0;
        }
    }
    
	float getClusterInner(SequenceAlignment alignment, List<String> queryTaxa, Cluster cluster,
                          File inputFile, int analysisMethod)
            throws ApplicationException {

		int numSubClusters = 1;
        List<String> clusterTaxa = cluster.getTaxaIds();
		for (int i = 0; i < clusterTaxa.size(); ++i)
			numSubClusters *= 2;

        if (numSubClusters > 10000) {
            System.err.println("Inner support for " + numSubClusters + " possible subclusters will take for ages." +
                    " Sure you want this ?");
        }
        
		float result = 0;
		for (int i = 1; i < numSubClusters - 1; ++i) { // excluding the empty and full cluster
			List<Integer> idxesList = new ArrayList<Integer>();

			for (int j = 0; j < clusterTaxa.size(); ++j)
				if ((i & (1 << j)) != 0)
					idxesList.add(new Integer(j));

			List<String> taxa = new ArrayList<String>(queryTaxa);

            for (int j = 0; j < idxesList.size(); ++j) {
				taxa.add(clusterTaxa.get(((Integer) idxesList.get(j)).intValue()));
			}

			result += retrieveResultValues(alignment, taxa);
		}		
        
        return result;
	}
    
    public Result run(SequenceAlignment alignment, AbstractSequence sequence)
            throws AnalysisException {
        
        try {
            /*
             * Collect clusters, and build automatically the paup backbone constraint
             */
            StringBuffer backboneClusters = new StringBuffer("(");
            Set<String> clusterSequences = new LinkedHashSet<String>();
            for (int i = 0; i < clusters.size(); ++i) {
                List<String> taxa = clusters.get(i).getTaxaIds();
                if (taxa.size() > 1) {
                	if (backboneClusters.length() > 1)
                		backboneClusters.append(',');
                	int index = clusterSequences.size() + 2;
                	backboneClusters.append('(').append(index++);
                	for (int k = 1; k < taxa.size(); ++k)
                		backboneClusters.append(',').append(index++);
                	backboneClusters.append(')');
                }

       			clusterSequences.addAll(taxa);
            }
            backboneClusters.append(')');

        	/*
        	 * Collect list of sequences;
        	 * modify name of query sequence if it collides with a reference sequence.
        	 */
            List<String> sequences = new ArrayList<String>();
            if (sequence != null) {
            	if (clusterSequences.contains(sequence.getName()))
            		sequence.setName(sequence.getName() + "_Query");
                sequences.add(sequence.getName());
            }
            sequences.addAll(clusterSequences);
        	
            SequenceAlignment aligned = profileAlign(alignment, sequence, workingDir);
            SequenceAlignment analysisAlignment = aligned.selectSequences(sequences);
            
            List<String> queryTaxa = new ArrayList<String>();
            if (sequence != null)
                queryTaxa.add(sequence.getName());
            return compute(analysisAlignment, sequence, queryTaxa, PAUP_ANALYSIS, backboneClusters.toString());
        } catch (AlignmentException e) {
            throw new AnalysisException(getId(), sequence, e);
        } catch (IOException e) {
            throw new AnalysisException(getId(), sequence, e);
        } catch (ApplicationException e) {
            throw new AnalysisException(getId(), sequence, e);
        }
    }
    
    /**
     * @return Returns the clusters.
     */
    public List<Cluster> getClusters() {
        return clusters;
    }

	@Override
	public Result run(AbstractSequence sequence) throws AnalysisException {
		return (rega.genotype.PhyloClusterAnalysis.Result) super.run(sequence);
	}
}
