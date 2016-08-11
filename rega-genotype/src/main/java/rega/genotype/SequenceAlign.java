/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rega.genotype.singletons.Settings;

/**
 * Class that computes pairwise or profile sequence alignments based on clustalw.
 * 
 * Computed alignments are stored in a cache to avoid recomputing the same alignment
 * multiple times.
 * 
 * To speed up profile alignments, there is an option to first trim the query sequence
 * based on a pairwise alignment.
 * 
 * @author koen
 */
public class SequenceAlign {
    static Map<AbstractSequence, Map<SequenceAlignment, SequenceAlignment>>
        alignmentCache = new HashMap<AbstractSequence, Map<SequenceAlignment, SequenceAlignment>>();

    static public SequenceAlignment profileAlign(AbstractSequence sequence,
                                                 SequenceAlignment alignment,
                                                 boolean trimAlignment,
                                                 File workingDir)
            throws AlignmentException {
        SequenceAlignment result = findInCache(sequence, alignment);
        if (result != null)
            return result;

        if (trimAlignment) {
            SequenceAlignment trimmedAlignment = trimAlignment(sequence, alignment, workingDir);
            result = clustalProfileAlign(sequence, trimmedAlignment, workingDir);
        } else {
            result = clustalProfileAlign(sequence, alignment, workingDir);
        }

        addToCache(sequence, alignment, result);
        
        return result;
    }

    private static SequenceAlignment trimAlignment(AbstractSequence sequence,
                                                   SequenceAlignment alignment, File workingDir)
            throws AlignmentException {
        SequenceAlignment example = pairAlign(alignment.getSequences().get(0), sequence, workingDir);
        
        int diff = example.getLength() - alignment.getLength();

        Sequence query = (Sequence) example.getSequences().get(1);
        
        int start = query.firstNonGapPosition();
        int end = query.lastNonGapPosition();
        
        int MARGIN = 0;

        start = Math.max(0, start - MARGIN - diff);
        end = Math.min(alignment.getLength(), end + MARGIN + diff);
        
        return alignment.getSubSequence(start, end);
    }

    static public SequenceAlignment pairAlign(AbstractSequence s1, AbstractSequence s2, File workingDir)
            throws AlignmentException {
        return pairAlign(s1, s2, workingDir, 15.0, 6.66);
    }

    static public SequenceAlignment pairAlign(AbstractSequence s1, AbstractSequence s2, File workingDir, double gapOpenPenalty, double gapExtensionPenalty)
    		throws AlignmentException {
    	return clustalPairAlign(s1, s2, workingDir, gapOpenPenalty, gapExtensionPenalty);
    }
    
    private static SequenceAlignment clustalPairAlign(AbstractSequence s1, AbstractSequence s2, File workingDir, double gapOpenPenalty, double gapExtensionPenalty)
            throws AlignmentException {
		String clustalWPath = Settings.getInstance().getClustalWCmd();

        try {
            File f = File.createTempFile("pair", ".fasta");
            File f3 = File.createTempFile("aligned", ".fasta");

            FileOutputStream fout = new FileOutputStream(f);
            s1.writeFastaOutput(fout);
            s2.writeFastaOutput(fout);
            //fout.flush();
            //fout.getFD().sync();
            fout.close();

            String cmd = clustalWPath + " -quicktree -infile=" + f.getAbsolutePath()
                + " -output=fasta"
                + " -gapopen=" + gapOpenPenalty
                + " -gapext=" + gapExtensionPenalty
                + " -outfile=" + f3.getAbsolutePath();
            System.err.println(cmd);
            Runtime r = Runtime.getRuntime();
            Process p = r.exec(cmd, null, workingDir);
            InputStream inputStream = p.getInputStream();

            LineNumberReader reader
                = new LineNumberReader(new InputStreamReader(inputStream));

            Pattern scorePattern = Pattern.compile("Alignment Score (-?\\d+)");

            int score = 0;
            for (;;) {
                String s = reader.readLine();
                if (s == null)
                    break;
                System.err.println(s);
                Matcher m = scorePattern.matcher(s);
                
                if (m.find()) {
                    score = Integer.valueOf(m.group(1)).intValue();
                }
            }
            p.waitFor();

            p.getErrorStream().close();
            p.getInputStream().close();
            p.getOutputStream().close();

            /*
            * Read the generated file.
            */
            SequenceAlignment result
                = new SequenceAlignment(new BufferedInputStream(new FileInputStream(f3)),
                                        SequenceAlignment.FILETYPE_FASTA,
                                        SequenceAlignment.SEQUENCE_ANY);
            result.setAlignmentScore(score);

            f.delete();
            f3.delete();
            File dnd = new File(f.getAbsolutePath().replace("fasta", "dnd"));
            dnd.delete();
            
            return result;
        } catch (NumberFormatException e) {
        	e.printStackTrace();
            throw new AlignmentException("Could not parse clustalw score!");
        } catch (FileNotFoundException e) {
        	e.printStackTrace();
            throw new AlignmentException("Clustalw did not write file!");
        } catch (IOException e) {
        	e.printStackTrace();
            throw new AlignmentException("I/O error while doing clustalw");
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new AlignmentException(e.getMessage());
        } catch (ParameterProblemException e) {
            e.printStackTrace();
            throw new AlignmentException(e.getMessage());            
        } catch (FileFormatException e) {
        	e.printStackTrace();
            throw new AlignmentException("Clustalw did not write a proper FASTA file!");
        }
    }

    private static SequenceAlignment clustalProfileAlign(AbstractSequence sequence,
                                                         SequenceAlignment alignment,
                                                         File workingDir)
            throws AlignmentException {

		String clustalWPath = Settings.getInstance().getClustalWCmd();
        try {
            File f = File.createTempFile("profile", ".fasta");
            File f2 = File.createTempFile("query", ".fasta");
            File f3 = File.createTempFile("aligned", ".fasta");

            FileOutputStream fout = new FileOutputStream(f);
            alignment.writeFastaOutput(fout);
            //fout.flush();
            //fout.getFD().sync();
            fout.close();

            FileOutputStream fout2 = new FileOutputStream(f2);
            sequence.writeFastaOutput(fout2);
            //fout2.flush();
            //fout2.getFD().sync();
            fout2.close();

            System.err.println("Written file: " + f.getPath());
            System.err.println("Written file: " + f2.getPath());
            
            String cmd = clustalWPath + " -quicktree -profile1=" + f.getAbsolutePath()
                + " -profile2=" + f2.getAbsolutePath() + " -output=fasta"
                + " -outfile=" + f3.getAbsolutePath();
            System.err.println(cmd);
            Runtime r = Runtime.getRuntime();
            Process p = r.exec(cmd, null, workingDir);
            InputStream inputStream = p.getInputStream();

            LineNumberReader reader
                = new LineNumberReader(new InputStreamReader(inputStream));

            Pattern scorePattern = Pattern.compile("Alignment Score (-?\\d+)");

            /*
             * This is OK for profile alignments as well: two scores are given but
             * the last one is used.
             */
            //int score = 0;
            for (;;) {
                String s = reader.readLine();
                if (s == null)
                    break;
                System.err.println(s);
                Matcher m = scorePattern.matcher(s);
                
                if (m.find()) {
                    //score = Integer.valueOf(m.group(1)).intValue();
                }
            }

            p.waitFor();

            p.getErrorStream().close();
            p.getInputStream().close();
            p.getOutputStream().close();

            /*
            * Read the generated file.
            */
            SequenceAlignment result
                = new SequenceAlignment(new BufferedInputStream(new FileInputStream(f3)),
                                        SequenceAlignment.FILETYPE_FASTA,
                                        SequenceAlignment.SEQUENCE_ANY);
            
            f.delete();
            f2.delete();
            f3.delete();
            File dnd = new File(f.getAbsolutePath().replace("fasta", "dnd"));
            dnd.delete();
            
            return result;
        } catch (NumberFormatException e) {
            throw new AlignmentException("Could not parse clustalw score!");
        } catch (FileNotFoundException e) {
            throw new AlignmentException("Clustalw did not write file!");
        } catch (IOException e) {
            throw new AlignmentException("I/O error while doing clustalw");
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new AlignmentException(e.getMessage());
        } catch (ParameterProblemException e) {
            e.printStackTrace();
            throw new AlignmentException(e.getMessage());            
        } catch (FileFormatException e) {
            throw new AlignmentException("Clustalw did not write a proper FASTA file!");
        }
    }

    private static SequenceAlignment findInCache(AbstractSequence sequence,
                                                 SequenceAlignment alignment) {
        Map<SequenceAlignment,SequenceAlignment> m = alignmentCache.get(sequence);
        
        if (m != null) {
            return m.get(alignment);
        } else
            return null;
    }
    
    private static void addToCache(AbstractSequence sequence, SequenceAlignment alignment,
                                   SequenceAlignment result) {
        Map<SequenceAlignment,SequenceAlignment> m = alignmentCache.get(sequence);
        if (m == null) {
            m = new HashMap<SequenceAlignment,SequenceAlignment>();
            alignmentCache.put(sequence, m);
        }
        
        m.put(alignment, result);
    }

    static public void forget(AbstractSequence sequence) {
        alignmentCache.remove(sequence);
    }

	public static void forgetAll() {
		alignmentCache.clear();
	}
}
