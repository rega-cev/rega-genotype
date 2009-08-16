/*
 * Created on Feb 8, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype;

import java.io.File;
import java.io.FileDescriptor;
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
	
    static public String formatDbCommand = "formatdb";
    static public String blastCommand = "blastall";

    private List<Cluster> clusters;
    private Double cutoff;
    private String options;
    private File workingDir;

    public class Result extends AbstractAnalysis.Result implements Concludable {
        private Cluster cluster;
        private float   score;
        private int start;
        private int end;

        public Result(AbstractSequence sequence, Cluster cluster, float score, int start, int end) {
            super(sequence);
            this.cluster = cluster;
            this.score = score;
            this.start = start;
            this.end = end;
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
            tracer.printlnClose("</cluster>");
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
    }
    
    @Override
	public Result run(AbstractSequence sequence) throws AnalysisException {
		return (rega.genotype.BlastAnalysis.Result) super.run(sequence);
	}

	public BlastAnalysis(AlignmentAnalyses owner, String id,
                         List<Cluster> clusters, Double cutoff,
                         String options,
                         File workingDir) {
        super(owner, id);
        this.clusters = clusters;
        this.cutoff = cutoff;
        this.options = options;
        this.workingDir = workingDir;
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

                String cmd = blastPath + File.separatorChar + formatDbCommand + " -o T -p F -i " + db.getAbsolutePath();
                System.err.println(cmd);
                formatdb = runtime.exec(cmd, null, workingDir);
                int result = formatdb.waitFor();

                if (result != 0) {
                    throw new ApplicationException("formatdb exited with error: " + result);
                }
                
                db.delete();
                
                cmd = blastPath + File.separatorChar + blastCommand + " -p blastn -i " + query.getAbsolutePath() + " "
                    + (options != null ? options : "") + " -m 8 -d " + db.getAbsolutePath();
                System.err.println(cmd);
                blast = runtime.exec(cmd, null, workingDir);
                InputStream inputStream = blast.getInputStream();

                LineNumberReader reader
                    = new LineNumberReader(new InputStreamReader(inputStream));

                String[] best = null;
                int start = Integer.MAX_VALUE;
                int end = -1;
                for (;;) {
                    String s = reader.readLine();
                    if (s == null)
                        break;
                    System.err.println(s);
                    if (best == null) {
                        best = s.split("\t");
                        if (best.length != 12)
                        	throw new ApplicationException("blast result format error");
                    }
                    String[] values = s.split("\t");
                    
                    if (values[1].equals(best[1]) && Float.parseFloat(values[11]) > cutoff) {
                    	start = Math.min(start, Integer.parseInt(values[8]) - Integer.parseInt(values[6]));
                    	end = Math.max(end, Integer.parseInt(values[9]) + sequence.getLength() - Integer.parseInt(values[7]));
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
                getTempFile("db.fasta.nhr").delete();
                getTempFile("db.fasta.nin").delete();
                getTempFile("db.fasta.nsd").delete();
                getTempFile("db.fasta.nsi").delete();
                getTempFile("db.fasta.nsq").delete();

                if (best == null)
                    throw new ApplicationException("blast error");
                
                return createResult(sequence, best[1], Float.valueOf(best[11]), start, end);
            } else
                return createResult(sequence, null, 0, 0, 0);
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
    
    private Result createResult(AbstractSequence sequence, String match,
                                float score, int start, int end) {
        for (int i = 0; i < clusters.size(); ++i) {
            if (clusters.get(i).containsTaxus(match))
                return new Result(sequence, clusters.get(i), score, start, end);
        }

        return new Result(sequence, null, 0, 0, 0);
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
}
