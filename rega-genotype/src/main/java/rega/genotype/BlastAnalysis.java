/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.utils.StreamReaderRuntime;

/**
 * Implements similarity based analyses using NCBI blast.
 * 
 * @author koen
 */
public class BlastAnalysis extends AbstractAnalysis {
	public static String blastPath = "";
    public static String formatDbCommand = "formatdb";
    public static String blastCommand = "blastall";

    // blastall results using the -m8 flag.
    // http://www.compbio.ox.ac.uk/analysis_tools/BLAST/BLAST_blastall/blastall_examples.shtml
    public static final int BLAST_RESULT_QUERY_ID_IDX = 0;
    public static final int BLAST_RESULT_SUBJECT_ID_IDX = 1;
    public static final int BLAST_RESULT_PERCENT_IDENTITY_IDX = 2;
    public static final int BLAST_RESULT_ALINGMENT_LENGTH_IDX = 3;
    public static final int BLAST_RESULT_MISMATCHES_IDX = 4;
    public static final int BLAST_RESULT_GAP_IDX = 5;
    public static final int BLAST_RESULT_Q_START_IDX = 6;
    public static final int BLAST_RESULT_Q_END_IDX = 7;
    public static final int BLAST_RESULT_S_START_IDX = 8;
    public static final int BLAST_RESULT_S_END_IDX = 9;
    public static final int BLAST_RESULT_E_VALUE_IDX = 10;
    public static final int BLAST_RESULT_BIT_SCORE_IDX = 11;

    /**
     * Defines a region in a reference sequence.
     */
    public static class Region {
    	private String name;
    	private int begin, end;
    	
    	public Region(String name, int begin, int end) {
    		this.name = name;
    		this.begin = begin;
    		this.end = end;
    	}

    	public void setBegin(int begin) {
    		this.begin = begin;
    	}
  
		public int getBegin() {
			return begin;
		}

		public void setEnd(int end) {
			this.end = end;
		}

		public int getEnd() {
			return end;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		/**
		 * @param queryBegin
		 * @param queryEnd
		 * @param minimumOverlap
		 * @return whether the region overlaps with a region defined by queryBegin
		 *   and queryEnd, with a minimum overlap.
		 */
		public boolean overlaps(int queryBegin, int queryEnd, int minimumOverlap) {
			int overlapBegin = Math.max(queryBegin, begin);
			int overlapEnd = Math.min(queryEnd, end);
			
			return (overlapEnd - overlapBegin) > minimumOverlap;
		}
    }

    public static class ReferenceTaxus {
    	private String taxus;
    	private List<Region> regions;
		private String reportOther;
		private int reportOtherOffset;
		private int priority; // lower value means higher priority

    	public ReferenceTaxus(String taxus, int priority) {
    		this.taxus = taxus;
    		this.priority = priority;
    		this.regions = new ArrayList<Region>();
    	}
    	
    	void setReportAsOther(String taxus, int offset) {
    		this.reportOther = taxus;
    		this.reportOtherOffset = offset;
    	}

    	public void addRegion(Region r) {
    		if (this.regions == null)
    			this.regions = new ArrayList<Region>();
    		
    		regions.add(r);
    	}
    
    	public boolean removeRegion(Region region) {
    		return regions.remove(region);
    	}
   
    	public void clearRegions() {
    		regions.clear();
    	}

    	public List<Region> getRegions() {
    		return regions;
    	}

    	public void setTaxus(String taxus) {
			this.taxus = taxus;
		}
 
		public String getTaxus() {
			return taxus;
		}
		
		public int getPriority() {
			return priority;
		}

		public String reportAsOther() {
			return reportOther;
		}
		
		public int reportAsOtherOffset() {
			return reportOtherOffset;
		}
    }
    
    private List<Cluster> clusters;
    private Double absCutoff;
    private Double absMaxEValue;
    private Double relativeCutoff;
    private Double relativeMaxEValue;
    private String blastOptions;
    private Map<String, ReferenceTaxus> referenceTaxa;
    private String detailsOptions;

	/**
	 * A result from a blast analysis.
	 * 
	 * It contains information on the location of the sequence with respect to a
	 * reference genome (which may be the best match or a predefined referenceTaxus).
	 * 
	 * It also tells you whether the query sequence is reference complimented with
	 * respect to the reference sequences.
	 */
    public class Result extends AbstractAnalysis.Result implements Concludable {
        private Set<Cluster> clusters;
        private float absScore;
        private float relativeScore;
        private int start;
        private int end;
        private int matchDiffs;
        private int matchLength;
        private ReferenceTaxus refseq;
		private boolean reverseCompliment;
		private String detailsFile;

        public Result(AbstractSequence sequence, Set<Cluster> bestClusters, float absScore, float relativeScore,
        		int length, int diffs, int start, int end, ReferenceTaxus refseq, boolean reverseCompliment) {
            super(sequence);
            this.clusters = bestClusters;
            this.absScore = absScore;
            this.relativeScore = relativeScore;
            this.matchLength = length;
            this.matchDiffs = diffs;
            this.start = start;
            this.end = end;
            this.refseq = refseq;
            this.reverseCompliment = reverseCompliment;
        }

        public boolean haveSupport() {
            if (getAbsCutoff() == null && getRelativeCutoff() == null)
                return false;
            else
                return (getAbsCutoff() != null && absScore >= getAbsCutoff())
                || (getRelativeCutoff() != null && relativeScore >= getRelativeCutoff());
        }
        
        public void writeXML(ResultTracer tracer) {
            writeXMLBegin(tracer);
            
            if (end >= 0) {
            	tracer.add("start", String.valueOf(start));           
            	tracer.add("end", String.valueOf(end));
            }

            tracer.add("identity", String.valueOf((float)(matchLength - matchDiffs)/matchLength));

            if (!supportsMultiple()) {
                writeCluster(tracer, getCluster());
            } else {
                tracer.printlnOpen("<clusters>");
                addMultiple(tracer);                
                for (Cluster c : clusters)
                	writeCluster(tracer, c);
                if (detailsFile != null)
                	tracer.add("details", detailsFile);
                tracer.printlnClose("</clusters>");
            }

            if (refseq != null)
            	tracer.add("refseq", refseq.getTaxus());
            
            writeXMLEnd(tracer);
        }

		private float calcSimilarity() {
			return (float)(matchLength - matchDiffs)/matchLength * 100;
		}

		private void writeCluster(ResultTracer tracer, Cluster cluster) {
			tracer.printlnOpen("<cluster>");
			tracer.add("id", cluster != null ? cluster.getId() : "none");
			tracer.add("name", cluster != null ? cluster.getName() : "none");
			tracer.add("absolute-score", absScore);
			tracer.add("relative-score", relativeScore);
			if (cluster != null && cluster.getDescription() != null) {
			    tracer.add("description", cluster.getDescription());
			}
			tracer.add("reverse-compliment", String.valueOf(reverseCompliment));
			tracer.add("concluded-id", haveSupport() ? cluster.getId() : "Unassigned");
			tracer.add("concluded-name", haveSupport() ? cluster.getName() : "Unassigned");
			tracer.add("tool-id", cluster != null ? cluster.getToolId() : "none");

			tracer.printlnClose("</cluster>");
		}

		private String getClusterIds() {
			String result = "";
			for (Cluster c : clusters) {
				if (!result.isEmpty())
					result += ", ";
				result += c.getId();
			}
			return result;
		}

		private String getClusterNames() {
			String result = "";
			for (Cluster c : clusters) {
				if (!result.isEmpty())
					result += ", ";
				result += c.getName();
			}
			return result;
		}

		private String getDescription() {
			TreeSet<String> d = new TreeSet<String>();
			for (Cluster c : clusters)
				if (c.getDescription() != null)
					d.add(c.getDescription());
			String result = "";
			for (String s : d) {
				if (!result.isEmpty())
					result += ", ";
				result += s;
			}
			return result;
		}

        /**
         * @return Returns the cluster.
         */
        public Cluster getCluster() {
            return clusters.isEmpty() ? null : clusters.iterator().next();
        }
        
        /**
        * @return Returns the start.
        */
       public int getStart() {
           return start;
       }

       /**
        * @return Returns the end.
        */
       public int getEnd() {
           return end;
       }

        public void writeConclusion(ResultTracer tracer) {
            tracer.printlnOpen("<assigned>");
            if (!supportsMultiple()) {
            	Cluster cluster = getCluster();
                tracer.add("absolute-score", String.valueOf(absScore));
                tracer.add("relative-score", String.valueOf(relativeScore));
                tracer.add("id", cluster.getId());
                tracer.add("name", cluster.getName());
                if (cluster.getDescription() != null) {
                    tracer.add("description", cluster.getDescription());
                }
            } else {
                addMultiple(tracer);
            }
            tracer.printlnClose("</assigned>");
        }

		private void addMultiple(ResultTracer tracer) {
			float similarity = calcSimilarity();
			tracer.add("score", String.valueOf(similarity));
			if (clusters.size() == 1 || similarity == 100) {
				tracer.add("id", getClusterIds());
				tracer.add("description", getDescription());
				tracer.add("name", similarity == 100 ? getClusterNames() : "NT (non-typeable)");
			} else {
				tracer.add("id", getClusterIds());
				tracer.add("description", getDescription());
				tracer.add("name", "NT (non-typeable)");
			}
		}

		public Cluster getConcludedCluster() {
			return getCluster();
		}

		public float getConcludedSupport() {
			return 0;
		}

		public boolean isReverseCompliment() {
			return reverseCompliment;
		}
		
		public ReferenceTaxus getReference() {
			return refseq;
		}

		public void setDetailsFile(String detailsFile) {
			this.detailsFile = detailsFile;
		}

		public float getAbsScore() {
			return absScore;
		}

		public void setAbsScore(float absScore) {
			this.absScore = absScore;
		}

		public float getRelativeScore() {
			return relativeScore;
		}

		public void setRelativeScore(float relativeScore) {
			this.relativeScore = relativeScore;
		}
    }

    boolean supportsMultiple() {
    	return detailsOptions != null;
    }
    
    @Override
	public Result run(AbstractSequence sequence) throws AnalysisException {
		return (rega.genotype.BlastAnalysis.Result) super.run(sequence);
	}

	public BlastAnalysis(AlignmentAnalyses owner, String id,
                         List<Cluster> clusters, Double absCutoff, Double absMaxEValue,
                         Double relativeCutoff, Double relativeMaxEValue,
                         String blastOptions,
                         String detailsOptions, File workingDir) {
        super(owner, id);
        this.workingDir = workingDir;
        this.clusters = clusters;
        this.absCutoff = absCutoff;
        this.absMaxEValue = absMaxEValue;
        this.relativeCutoff = relativeCutoff;
        this.relativeMaxEValue = relativeMaxEValue;
        this.blastOptions = blastOptions != null ? blastOptions : "";
        this.detailsOptions = detailsOptions;
        this.referenceTaxa = new HashMap<String, ReferenceTaxus>();
    }

    private Result compute(SequenceAlignment analysisDb, AbstractSequence sequence)
            throws ApplicationException {
        Process formatdb = null;
        Process blast = null;
        try {
            if (sequence.getLength() != 0) {
                File db = getTempFile("db.fasta");
                FileOutputStream dbFile = new FileOutputStream(db);
                //FileDescriptor fd = dbFile.getFD();
                analysisDb.writeFastaOutput(dbFile);
                //dbFile.flush();
                //fd.sync();
                dbFile.close();

                File query = getTempFile("query.fasta");
                FileOutputStream queryFile = new FileOutputStream(query);
                //FileDescriptor fd2 = dbFile.getFD();
                sequence.writeFastaOutput(queryFile);
                //queryFile.flush();
                //fd2.sync();
                queryFile.close();
                        
                String cmd = blastPath + formatDbCommand + " " + formatDbOptions() + " -o T -i " + db.getAbsolutePath();
                System.err.println(cmd);
                
                formatdb = StreamReaderRuntime.exec(cmd, null, workingDir);
                int exitResult = formatdb.waitFor();

                if (exitResult != 0) {
                    throw new ApplicationException("formatdb exited with error: " + exitResult);
                }
                
                String blastOptions = "";
        		if (owner.getAlignment() != null && 
                		owner.getAlignment().getSequenceType() == SequenceAlignment.SEQUENCE_AA) {
        			blastOptions = "-p blastx " + this.blastOptions;
                	if (detailsOptions != null)
                		blastOptions = "-p blastx " + this.detailsOptions;
                } else {
                	blastOptions = "-p blastn " + this.blastOptions;
                	if (detailsOptions != null)
                		blastOptions = "-p blastn " + this.detailsOptions;
                }
                
                cmd = blastPath + blastCommand + " " + blastOptions
                	+ " -i " + query.getAbsolutePath()
                    + " -m 8 -d " + db.getAbsolutePath();
                System.err.println(cmd);

                blast = Runtime.getRuntime().exec(cmd, null, workingDir);
                InputStream inputStream = blast.getInputStream();

                final LineNumberReader reader
                    = new LineNumberReader(new InputStreamReader(inputStream));

                BlastResults br = new BlastResults() {
					public String[] next() throws ApplicationException {
						String s = null;
						try {
							s = reader.readLine();
						} catch (IOException ioe) {
							throw new ApplicationException("Error: I/O Error while invoking blast: " + ioe.getMessage());
						}
		    			if (s == null)
		    				return null;
		    			System.err.println(s);
		
		    			String[] values = s.split("\t");
		    			if (values.length != 12)
		    				throw new ApplicationException("blast result format error");
		    			return values;
					}
                };
                
                boolean aa = owner.getAlignment().getSequenceType() == SequenceAlignment.SEQUENCE_AA;
                Result result = parseBlastResults(br, this, aa, sequence);
                
                exitResult = blast.waitFor();

                blast.getErrorStream().close();
                blast.getInputStream().close();
                blast.getOutputStream().close();

                if (exitResult != 0) {
                    throw new ApplicationException("blast exited with error: " + exitResult);
                }

                String detailsFile = null;
                if (detailsOptions != null)
                	detailsFile = collectDetails(query, db);                

                db.delete();                
                query.delete();

                if (owner.getAlignment().getSequenceType() == SequenceAlignment.SEQUENCE_DNA) {
                    getTempFile("db.fasta.nhr").delete();
                    getTempFile("db.fasta.nin").delete();
                    getTempFile("db.fasta.nsd").delete();
                    getTempFile("db.fasta.nsi").delete();
                    getTempFile("db.fasta.nsq").delete();
                } else {
                    getTempFile("db.fasta.phr").delete();
                    getTempFile("db.fasta.pin").delete();
                    getTempFile("db.fasta.psd").delete();
                    getTempFile("db.fasta.psi").delete();
                    getTempFile("db.fasta.psq").delete();
                }

				if (result != null) {
					if (detailsFile != null) {
						result.setDetailsFile(detailsFile);
					}
					return result;
				} else
					return createResult(sequence, new HashSet<AlignmentAnalyses.Cluster>(), null, 0, 0, 0, 0, 0, 0, false);
            } else
                return createResult(sequence, new HashSet<AlignmentAnalyses.Cluster>(), null, 0, 0, 0, 0, 0, 0, false);
        } catch (IOException e) {
            if (formatdb != null)
                formatdb.destroy();
            if (blast != null)
                blast.destroy();
            throw new ApplicationException("Error: I/O Error while invoking blast: "
                + e.getMessage());
        } catch (InterruptedException e) {
            if (formatdb != null)
                formatdb.destroy();
            if (blast != null)
                blast.destroy();
            throw new ApplicationException("Error: I/O Error while invoking blast: "
                + e.getMessage());
        }
    }
    
    public interface BlastResults {
    	String [] next() throws ApplicationException;
    }
	public static Result parseBlastResults(BlastResults results, BlastAnalysis ba, boolean aa, AbstractSequence sequence) throws ApplicationException {
		int seqLength = sequence.getLength();
		int queryFactor = aa ? 3 : 1;

		String[] best = null, secondBest = null;
		int start = Integer.MAX_VALUE;
		int end = -1;

		boolean reverseCompliment = false;

		ReferenceTaxus refseq = null;
		Set<Cluster> bestClusters = new HashSet<Cluster>(), secondBestClusters = new HashSet<Cluster>();

		for (;;) {
			String [] values = results.next();
			if (values == null)
				break;

			if (best == null)
				best = values;

			if (values[BLAST_RESULT_BIT_SCORE_IDX].equals(best[BLAST_RESULT_BIT_SCORE_IDX]))
				bestClusters.add(ba.findCluster(values[BLAST_RESULT_SUBJECT_ID_IDX]));

			ReferenceTaxus referenceTaxus = ba.referenceTaxa
					.get(values[BLAST_RESULT_SUBJECT_ID_IDX]);

			/*
			 * First condition: there are no explicit reference taxa configured
			 * -- use best match
			 * 
			 * Second condition: - the referenceTaxus is the first - or has a
			 * higher priority than the current refseq and belongs to the same
			 * cluster (note priority is smaller number means higher priority)
			 */
			if ((ba.referenceTaxa.isEmpty() && values == best)
					|| (referenceTaxus != null
							&& (ba.findCluster(values[BLAST_RESULT_SUBJECT_ID_IDX]) == bestClusters
									.iterator().next()) && (refseq == null || referenceTaxus
							.getPriority() < refseq.getPriority()))) {
				refseq = referenceTaxus;
				boolean queryReverseCompliment = Integer.parseInt(values[BLAST_RESULT_Q_END_IDX])
						- Integer.parseInt(values[BLAST_RESULT_Q_START_IDX]) < 0;
				boolean refReverseCompliment = Integer.parseInt(values[BLAST_RESULT_S_END_IDX])
						- Integer.parseInt(values[BLAST_RESULT_S_START_IDX]) < 0;
				int offsetBegin = Integer.parseInt(values[BLAST_RESULT_Q_START_IDX]);
				int offsetEnd = seqLength
						- Integer.parseInt(values[BLAST_RESULT_Q_END_IDX]);
				if (queryReverseCompliment) {
					offsetBegin = seqLength - offsetBegin;
					offsetEnd = seqLength - offsetEnd;
					reverseCompliment = true;
				}
				if (refReverseCompliment) {
					String tmp = values[BLAST_RESULT_S_START_IDX];
					values[BLAST_RESULT_S_START_IDX] = values[BLAST_RESULT_S_END_IDX];
					values[9] = tmp;
					reverseCompliment = true;
				}
				start = Integer.parseInt(values[BLAST_RESULT_S_START_IDX]) * queryFactor - offsetBegin;
				end = Integer.parseInt(values[BLAST_RESULT_S_END_IDX]) * queryFactor + offsetEnd;

				if (refseq != null && refseq.reportAsOther() != null) {
					refseq = ba.referenceTaxa.get(refseq.reportAsOther());
					start += refseq.reportAsOtherOffset();
					end += refseq.reportAsOtherOffset();
				}
			}

			if ((ba.relativeCutoff != null || ba.relativeMaxEValue != null)
					&& !bestClusters.isEmpty()) {
				Cluster c = ba.findCluster(values[BLAST_RESULT_SUBJECT_ID_IDX]);
				if (!bestClusters.contains(c)) {
					if (secondBest == null)
						secondBest = values;
					if (secondBest[BLAST_RESULT_BIT_SCORE_IDX].equals(values[BLAST_RESULT_BIT_SCORE_IDX]))
						secondBestClusters.add(c);
				}
			}
		}

		if (best != null) {
			int length = Integer.valueOf(best[BLAST_RESULT_ALINGMENT_LENGTH_IDX]);
			int diffs = Integer.valueOf(best[BLAST_RESULT_MISMATCHES_IDX]) 
					+ Integer.valueOf(best[BLAST_RESULT_GAP_IDX]); // #diffs + #gaps
			float pValue = Float.valueOf(best[BLAST_RESULT_E_VALUE_IDX]);
			float absScore = Float.valueOf(best[BLAST_RESULT_BIT_SCORE_IDX]);
			float relativeScore = absScore;

			if ((ba.absCutoff == null && ba.absMaxEValue == null)|| 
					(ba.absMaxEValue != null && pValue > ba.absMaxEValue))
				absScore = -1;
			if ((ba.relativeCutoff == null && ba.relativeMaxEValue == null)|| 
					(ba.relativeMaxEValue != null && pValue > ba.relativeMaxEValue))
				relativeScore = -1;

			if (ba.relativeCutoff != null || ba.relativeMaxEValue != null) {
				if (secondBest != null)
					relativeScore = relativeScore / Float.valueOf(secondBest[BLAST_RESULT_BIT_SCORE_IDX]);
			}

			if (start == Integer.MAX_VALUE)
				start = -1;

			Result result = ba.createResult(sequence, bestClusters, refseq,
					absScore, relativeScore, length, diffs, start, end, reverseCompliment);

			return result;
		} else {
			return null;
		}
	}

    private String collectDetails(File query, File db) throws IOException, InterruptedException, ApplicationException {
        String cmd = blastPath + blastCommand + " " + detailsOptions
            	+ " -i " + query.getAbsolutePath()
                + " -T -d " + db.getAbsolutePath();
        System.err.println(cmd);
        Process blast = Runtime.getRuntime().exec(cmd, null, workingDir);

        File outputFile = getTempFile("paup.html");
        copyToFile(outputFile, blast.getInputStream());        
        int result = blast.waitFor();

        String detailsResource = makeResource(outputFile, "html");

        blast.getErrorStream().close();
        blast.getInputStream().close();
        blast.getOutputStream().close();

		if (result != 0) { 
			throw new ApplicationException("Blast exited with error: " + result);
		}
		
		return detailsResource;
	}

	private void copyToFile(File outputFile, InputStream stdout)
			throws IOException {
		InputStreamReader isr = new InputStreamReader(stdout);
        BufferedReader br = new BufferedReader(isr);
        FileWriter osw = new FileWriter(outputFile);
        
        String line = null;
        while ( (line = br.readLine()) != null)
        	osw.write(line + '\n');
        br.close();
        osw.close();
	}

	private Cluster findCluster(String taxus) {
        for (int i = 0; i < clusters.size(); ++i)
            if (clusters.get(i).containsTaxus(taxus))
            	return clusters.get(i);
        
        return null;
    }
    
    private Result createResult(AbstractSequence sequence, Set<Cluster> bestClusters, ReferenceTaxus refseq,
                                float absScore, float relativeScore, int length, int diffs, int start, int end, boolean reverseCompliment) {
    	if (!bestClusters.isEmpty())
    		return new Result(sequence, bestClusters, absScore, relativeScore, length, diffs, start, end, refseq, reverseCompliment);
    	else
    		return new Result(sequence, bestClusters, 0, 0, 0, 0, 0, 0, null, reverseCompliment);
    }

    Result run(SequenceAlignment alignment, AbstractSequence sequence) throws AnalysisException {
        try {
            List<String> sequences = new ArrayList<String>();
            Set<String> clusterSequences = new LinkedHashSet<String>();

            for (int i = 0; i < clusters.size(); ++i) {
                List<String> taxa = clusters.get(i).getTaxaIds();
                clusterSequences.addAll(taxa);
            }
            sequences.addAll(clusterSequences);

            SequenceAlignment analysisDb = alignment.selectSequences(sequences);
            
            return compute(analysisDb, sequence);
        } catch (ApplicationException e) {
            throw new AnalysisException(getId(), sequence, e);
        }
    }

	public void addReferenceTaxus(ReferenceTaxus t) {
		referenceTaxa.put(t.getTaxus(), t);
	}

	public void clearReferenceTaxus() {
		referenceTaxa.clear();
	}

	public void removeReferenceTaxus(ReferenceTaxus t) {
		referenceTaxa.remove(t.getTaxus());
	}

	public Collection<ReferenceTaxus> getReferenceTaxus() {
		return referenceTaxa.values();
	}

	public ReferenceTaxus findReferenceTaxus(String taxusId) {
		for (ReferenceTaxus taxus: getReferenceTaxus()) {
			if (taxus.getTaxus().equals(taxusId))
				return taxus;
		}

		return null;
	}

	public Set<String> getRegions() {
		Set<String> result = new HashSet<String>();
		for (ReferenceTaxus t : referenceTaxa.values())
			for (Region r : t.getRegions())
				result.add(r.getName());
		
		return result;
	}

	public Double getAbsCutoff() {
		return absCutoff;
	}

	public void setAbsCutoff(Double absCutoff) {
		this.absCutoff = absCutoff;
	}

	public Double getAbsMaxEValue() {
		return absMaxEValue;
	}

	public void setAbsMaxEValue(Double absMaxEValue) {
		this.absMaxEValue = absMaxEValue;
	}

	public Double getRelativeCutoff() {
		return relativeCutoff;
	}

	public void setRelativeCutoff(Double relativeCutoff) {
		this.relativeCutoff = relativeCutoff;
	}

	public Double getRelativeMaxEValue() {
		return relativeMaxEValue;
	}

	public void setRelativeMaxEValue(Double relativeMaxEValue) {
		this.relativeMaxEValue = relativeMaxEValue;
	}

	public String getBlastOptions() {
		return blastOptions;
	}

	public void setBlastOptions(String blastOptions) {
		this.blastOptions = blastOptions;
	}

	private String formatDbOptions() {
		if (owner.getAlignment() != null && 
        		owner.getAlignment().getSequenceType() == SequenceAlignment.SEQUENCE_AA) 
			return "";
		else
			return "-p F";
	}
}
