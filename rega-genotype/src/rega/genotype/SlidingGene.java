package rega.genotype;
import jargs.gnu.CmdLineParser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*
 * Created on Apr 7, 2003
 */

/**
 * Sliding Gene main program
 */
public class SlidingGene {

    private static void printUsage() {
        System.err.println("usage: sg [ {-i, --informat} {FASTA} ]\n" +                           "          [ {-o, --outformat} {NEXUS, FASTA, PHYLIP} ]\n" +                           "          [ {-w, --windowsize} number ]\n" +                           "          [ {-s, --stepsize} number ]\n" +
                           "          infile outfileprefix\n");
    }

    public static void main(String[] args) throws Exception {
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option inFormatOption = parser.addStringOption('i', "--informat");
        CmdLineParser.Option outFormatOption = parser.addStringOption('o', "--outformat");
        CmdLineParser.Option windowSizeOption = parser.addIntegerOption('w', "--windowsize");
        CmdLineParser.Option stepSizeOption = parser.addIntegerOption('s', "--stepsize");

        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
        }

        String[] otherArgs = parser.getRemainingArgs();
        if (otherArgs.length != 2) {
            printUsage();
            System.exit(2);
        }

        String inFileName = otherArgs[0];
        String outPrefix = otherArgs[1];

        Integer windowSizeInt = (Integer) parser.getOptionValue(windowSizeOption);
        Integer stepSizeInt = (Integer) parser.getOptionValue(stepSizeOption);
        String inFormat = (String) parser.getOptionValue(inFormatOption);
        String outFormat = (String) parser.getOptionValue(outFormatOption);

        int windowSize = windowSizeInt != null ? windowSizeInt.intValue() : 100;
        int stepSize = stepSizeInt != null ? stepSizeInt.intValue() : 10;
        int inFileFormat = inFormat != null ? parseFormat(inFormat)
                : SequenceAlignment.FILETYPE_FASTA;
        int outFileFormat = outFormat != null ? parseFormat(outFormat)
                : SequenceAlignment.FILETYPE_FASTA;

        List windowed = null;
        try {
            windowed = slidingGene(inFileName, windowSize, stepSize, false, false);
        } catch (IOException e) {
            System.err.println("Error: I/O Error while reading " + inFileName);
            System.exit(1);
        } catch (FileFormatException e) {
            System.err.println("Error: File format exception while reading "
                               + inFileName + " " + e.getMessage());
            System.exit(1);
        } catch (AlignmentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }

        Iterator i = windowed.iterator();
        int k = 0;
        String suffix = ".txt";
        switch (outFileFormat) {
        case SequenceAlignment.FILETYPE_FASTA:
        	suffix = ".fasta"; break;
        case SequenceAlignment.FILETYPE_NEXUS:
        	suffix = ".nex"; break;
        case SequenceAlignment.FILETYPE_PHYLIP:
        	suffix = ".phy";
        }

        while (i.hasNext()) {
            SequenceAlignment a = (SequenceAlignment) i.next();
            
            String outFileName = outPrefix + k + suffix;
            try {
                OutputStream outFile = new FileOutputStream(outFileName);
                a.writeOutput(outFile, outFileFormat);
                outFile.close();
            } catch (FileNotFoundException e) {
                System.err.println("Error: Cannot open " + outFileName + " for writing");
                System.exit(1);
            } catch (IOException e) {
                System.err.println("Error: I/O Error while writing " + outFileName);
                System.exit(1);
            } catch (ParameterProblemException e) {
                System.err.println("Error: " + e.getMessage());
                System.exit(1);
            }

            ++k;
        }
    }

    private static int parseFormat(String s) {
        if (s.equalsIgnoreCase("FASTA"))
            return SequenceAlignment.FILETYPE_FASTA;
        else if (s.equalsIgnoreCase("NEXUS"))
            return SequenceAlignment.FILETYPE_NEXUS;
        else if (s.equalsIgnoreCase("CLUSTAL"))
            return SequenceAlignment.FILETYPE_CLUSTAL;
        else if (s.equalsIgnoreCase("PHYLIP"))
            return SequenceAlignment.FILETYPE_PHYLIP;
        else {
            System.err.println("Error: Unknown file format: " + s);
            System.exit(2);
            return 0;
        }
    }

    public static List slidingGene(String inFileName, int windowSize, int stepSize, boolean isAminoAcid,
    							   boolean degap)
        throws FileNotFoundException, ParameterProblemException,
               IOException, FileFormatException, AlignmentException {
        InputStream inFile = new FileInputStream(inFileName);
        
        SequenceAlignment alignment =
                new SequenceAlignment(inFile, SequenceAlignment.FILETYPE_FASTA);
        if (degap)
        	alignment.degap();

        alignment.setSequenceType(isAminoAcid ? SequenceAlignment.SEQUENCE_AA : SequenceAlignment.SEQUENCE_DNA);

        // our demo alignment is not properly aligned
        if (!alignment.areAllEqualLength(true)) {
            throw new AlignmentException("not a proper alignment: every sequence must be"
                + " of equal length");
        }
        
        List windowed = generateSlidingWindow(alignment, windowSize, stepSize);
        return windowed;
    }
    
    /**
     * Create a list of SequenceAlignment objects that each represent
     * a single window in the sliding window run with given parameters
     * 
     * @return List of {@link SequenceAlignment}
     */
    static List<SequenceAlignment> generateSlidingWindow(SequenceAlignment alignment, int windowSize, int step) {
        int alignmentLength = alignment.getLength();
        List<SequenceAlignment> result = new ArrayList<SequenceAlignment>();
        
        int i = 0;
        for (i = 0; i < alignmentLength - windowSize; i += step) {
            result.add(alignment.getSubSequence(i, i + windowSize));
        }

        result.add(alignment.getSubSequence(i, alignmentLength));
        
        return result;
    }
}
