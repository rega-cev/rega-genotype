/*
 * Created on Feb 8, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import rega.genotype.AlignmentAnalyses.Cluster;

public class BlastAnalysis extends AbstractAnalysis {
	public static String blastPath = "";
    public static String formatDbCommand = "formatdb";
    public static String blastCommand = "blastall";

    public static class Region {
    	private String name;
    	private int begin, end;
    	
    	public Region(String name, int begin, int end) {
    		this.name = name;
    		this.begin = begin;
    		this.end = end;
    	}

		public int getBegin() {
			return begin;
		}

		public int getEnd() {
			return end;
		}

		public String getName() {
			return name;
		}

		public boolean overlaps(int queryBegin, int queryEnd, int minimumOverlap) {
			int overlapBegin = Math.max(queryBegin, begin);
			int overlapEnd = Math.min(queryEnd, end);
			
			return (overlapEnd - overlapBegin) > minimumOverlap;
		}
    }
    
    private List<Cluster> clusters;
    private Double cutoff;
    private String blastOptions;
	private String formatDbOptions;
    private File workingDir;
   
	private List<Region> regions;
	private String referenceTaxus;

    public class Result extends AbstractAnalysis.Result implements Concludable {
        private Cluster cluster;
        private float   score;
        private int start;
        private int end;
        private String refseq;
		private boolean reverseCompliment;

        public Result(AbstractSequence sequence, Cluster cluster, float score, int start, int end, String refseq, boolean reverseCompliment) {
            super(sequence);
            this.cluster = cluster;
            this.score = score;
            this.start = start;
            this.end = end;
            this.refseq = refseq;
            this.reverseCompliment = reverseCompliment;
        }

        public boolean haveSupport() {
            if (cutoff == null)
                return false;
            else
                return score >= cutoff;
        }
        
        public void writeXML(ResultTracer tracer) {
            writeXMLBegin(tracer);
            tracer.add("start", String.valueOf(start));
            tracer.add("end", String.valueOf(end));
            tracer.printlnOpen("<cluster>");
            tracer.add("id", cluster != null ? cluster.getId() : "none");
            tracer.add("name", cluster != null ? cluster.getName() : "none");
            tracer.add("score", score);
            if (cluster != null && cluster.getDescription() != null) {
                tracer.add("description", cluster.getDescription());
            }
            tracer.add("reverse-compliment", String.valueOf(reverseCompliment));
            tracer.printlnClose("</cluster>");
            tracer.add("refseq", refseq);
            writeXMLEnd(tracer);
        }

        /**
         * @return Returns the cluster.
         */
        public Cluster getCluster() {
            return cluster;
        }

        /**
         * @return Returns the score.
         */
        public float getScore() {
            return score;
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
            tracer.add("id", cluster.getId());
            tracer.add("name", cluster.getName());
            tracer.add("score", String.valueOf(score));
            if (cluster.getDescription() != null) {
                tracer.add("description", cluster.getDescription());
            }
            tracer.printlnClose("</assigned>");
        }

		public Cluster getConcludedCluster() {
			return cluster;
		}

		public float getConcludedSupport() {
			return 0;
		}

		public boolean isReverseCompliment() {
			return reverseCompliment;
		}
    }
    
    @Override
	public Result run(AbstractSequence sequence) throws AnalysisException {
		return (rega.genotype.BlastAnalysis.Result) super.run(sequence);
	}

	public BlastAnalysis(AlignmentAnalyses owner, String id,
                         List<Cluster> clusters, Double cutoff,
                         String blastOptions,
                         File workingDir) {
        super(owner, id);
        this.workingDir = workingDir;
        this.clusters = clusters;
        this.cutoff = cutoff;
        this.blastOptions = blastOptions != null ? blastOptions : "";
        if (owner.getAlignment().getSequenceType() == SequenceAlignment.SEQUENCE_AA) {
        	this.blastOptions = "-p blastx";
        	this.formatDbOptions = "";
        } else {
        	this.blastOptions = "-p blastn";
        	this.formatDbOptions = "-p F";
        }
    }

	void addRegion(Region r) {
		if (this.regions == null)
			this.regions = new ArrayList<Region>();
		
		regions.add(r);
	}
	
	public List<Region> getRegions() {
		return regions;
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
                        
                Runtime runtime = Runtime.getRuntime();

                String cmd = blastPath + formatDbCommand + " " + formatDbOptions + " -o T -i " + db.getAbsolutePath();
                System.err.println(cmd);
                formatdb = runtime.exec(cmd, null, workingDir);
                int result = formatdb.waitFor();

                if (result != 0) {
                    throw new ApplicationException("formatdb exited with error: " + result);
                }
                
                db.delete();
                
                cmd = blastPath + blastCommand + " " + blastOptions
                	+ " -i " + query.getAbsolutePath()
                    + " -m 8 -d " + db.getAbsolutePath();
                System.err.println(cmd);
                blast = runtime.exec(cmd, null, workingDir);
                InputStream inputStream = blast.getInputStream();

                LineNumberReader reader
                    = new LineNumberReader(new InputStreamReader(inputStream));

                int queryFactor = (owner.getAlignment().getSequenceType() == SequenceAlignment.SEQUENCE_AA)
                	? 3 : 1;

                String[] best = null;
                int start = Integer.MAX_VALUE;
                int end = -1;
                
                boolean reverseCompliment = false;

                String refseq = "";

                for (;;) {
                    String s = reader.readLine();
                    if (s == null)
                        break;
                    System.err.println(s);

                    String[] values = s.split("\t");
                    if (values.length != 12)
                    	throw new ApplicationException("blast result format error");

                    if (best == null)
                    	best = values;

                    if ((end == -1)
                    	 && ((referenceTaxus == null && values == best) || values[1].equals(referenceTaxus))) {
                    	refseq = referenceTaxus;
                       	reverseCompliment = Integer.parseInt(values[7]) - Integer.parseInt(values[6]) < 0;
                       	int offsetBegin = Integer.parseInt(values[6]);
                       	int offsetEnd = sequence.getLength() - Integer.parseInt(values[7]);
                       	if (reverseCompliment) {
                       		offsetBegin = sequence.getLength() - offsetBegin;
                       		offsetEnd = sequence.getLength() - offsetEnd;
                       	}                       	
                       	start = Integer.parseInt(values[8])*queryFactor - offsetBegin;
                       	end = Integer.parseInt(values[9])*queryFactor + offsetEnd;
                    }
                }
                result = blast.waitFor();

                blast.getErrorStream().close();
                blast.getInputStream().close();
                blast.getOutputStream().close();

                if (result != 0) {
                    throw new ApplicationException("blast exited with error: " + result);
                }

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

                if (best == null)
                    throw new ApplicationException("blast error");

                return createResult(sequence, best[1], refseq, Float.valueOf(best[11]), start, end, reverseCompliment);
            } else
                return createResult(sequence, null, null, 0, 0, 0, false);
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
    
    private Result createResult(AbstractSequence sequence, String match, String refseq,
                                float score, int start, int end, boolean reverseCompliment) {
        for (int i = 0; i < clusters.size(); ++i) {
            if (clusters.get(i).containsTaxus(match))
                return new Result(sequence, clusters.get(i), score, start, end, refseq, reverseCompliment);
        }

        return new Result(sequence, null, 0, 0, 0, null, reverseCompliment);
    }

    Result run(SequenceAlignment alignment, AbstractSequence sequence)
            throws AnalysisException {

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

	public Double getCutoff() {
		return cutoff;
	}

	public String getReferenceTaxus() {
		return referenceTaxus;
	}

	public void setReferenceTaxus(String referenceTaxus) {
		this.referenceTaxus = referenceTaxus;
	}
}
