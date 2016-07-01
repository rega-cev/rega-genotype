/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import rega.genotype.AlignmentAnalyses.Cluster;

/**
 * Represents an alignment of multiple sequences, and provides I/O methods to read and
 * the alignment from FASTA and to various file formats.
 * 
 * Note: It can also simply hold a set of unaligned sequences.
 */
public class SequenceAlignment
{
    private List<AbstractSequence> sequences;
    int sequenceType = SEQUENCE_DNA;
    int alignmentScore = -1;

    public final static int FILETYPE_FASTA = 0;
    public final static int FILETYPE_CLUSTAL = 1;
    public final static int FILETYPE_NEXUS = 2;
    public final static int FILETYPE_PHYLIP = 3;

	public final static int SEQUENCE_ANY = 0;
	public final static int SEQUENCE_DNA = 1;
	public final static int SEQUENCE_AA = 2;
    
    public final static int MAX_NEXUS_TAXUS_LENGTH = 20;
    public final static int MAX_PHYLIP_TAXUS_LENGTH = 8;

    public SequenceAlignment() {
    	this.sequenceType = FILETYPE_FASTA;
        this.sequences = new ArrayList<AbstractSequence>();
    }
    
    public SequenceAlignment(InputStream inputFile,
                             int fileType, int sequenceType)
        throws ParameterProblemException, IOException, FileFormatException
    {
    	this.sequenceType = sequenceType;
        this.sequences = new ArrayList<AbstractSequence>();

        switch (fileType) {
            case FILETYPE_FASTA:
                readFastaFile(inputFile, sequenceType);
                break;
            case FILETYPE_CLUSTAL:
                //readClustalFile(inputFile);
                throw new ParameterProblemException("Reading clustal not yet supported");
            case FILETYPE_NEXUS:
                readNexusFile(inputFile, sequenceType);
                break;
            default:
                throw new ParameterProblemException("Illegal value for fileType");
        }
        
        inputFile.close();
    }

	/**
	 * Removes all-gap columns from the alignment.
	 */
	public void degap() {
		for (int i = 0; i < getLength();) {
			boolean hasGap = false;

			for (int j = 0; j < sequences.size(); ++j) {
				AbstractSequence s = sequences.get(j);
				
				if (s.getSequence().charAt(i) == '-') {
					hasGap = true;
					break;
				}
			}

			if (hasGap) {
				for (int j = 0; j < sequences.size(); ++j) {
					AbstractSequence s = sequences.get(j);

					s.removeChar(i);
				}
			} else
				++i;
		}
	}

	/**
	 * Checks whether the alignment is sane: all sequences must have equal length.
	 * Optionally, it may enforce this by removing sequences that have a length different
	 * from the first sequence length.
	 */
    public boolean areAllEqualLength(boolean force) {
        Iterator<AbstractSequence> i = sequences.iterator();
        int length = -1;

        while (i.hasNext()) {
            AbstractSequence s = i.next();

            if (length == -1)
                length = s.getLength();
            else
                if (s.getLength() != length) {
                    if (force)
                        i.remove();
                    else
                        return false;
                }
        }

        return true;
    }

    SequenceAlignment(List<AbstractSequence> sequences, int sequenceType) {
        this.sequences = sequences;
        this.sequenceType = sequenceType;
    }

    private void readFastaFile(InputStream inputFile, int sequenceType)
        throws IOException, FileFormatException
    {
        /*
         * The FASTA format (for multiple sequences) as described in
         * http://www.molbiol.ox.ac.uk/help/formatexamples.htm#fasta
         * and
         * http://www.ncbi.nlm.nih.gov/BLAST/fasta.html
         */

        LineNumberReader reader
            = new LineNumberReader(new InputStreamReader(inputFile));

        for (;;) {
            Sequence s = readFastaFileSequence(reader, sequenceType);

            if (s != null) {
                sequences.add(s);
            } else
                return;
        }
    }
    
	private void readFastqFile(InputStream inputFile, int sequenceType)
			throws IOException, FileFormatException {
		LineNumberReader reader = new LineNumberReader(new InputStreamReader(inputFile));
		for (;;) {
			Sequence s = readFastqFileSequence(reader, sequenceType);
			if (s != null) {
				sequences.add(s);
			} else {
				return;
			}
		}
	}

    public static Sequence readFastaFileSequence(LineNumberReader reader, int sequenceType)
        throws IOException, FileFormatException
    {
        /*
         * first read the header
         */
        String header;
        do {
            header = reader.readLine();            
            if (header == null)
                return null; // no new sequence to be found
        } while (header.length() == 0);

        if (header.charAt(0) != '>')
            throw new FileFormatException("Expecting a '>'",
                                          reader.getLineNumber());
        
        if (!header.substring(1).matches("[^!@#$%^&\\*()+=]*"))
            throw new FileFormatException("Illegal character (one of '!@#$%^&\\*()+=')",
                    reader.getLineNumber());

        // eat '>'
        header = header.substring(1);
        while (header.charAt(0) == ' ')
			header = header.substring(1);
        // separate name from description
        int spacePos = header.indexOf(' ');
        String name;
        boolean nameCapped;
        String description;
        if (spacePos != -1) {   // only a name	
           name = makeLegalName(header.substring(0, spacePos));
           nameCapped = name.length() < header.substring(0, spacePos).length();
           description = header.substring(spacePos);
        } else {
           name = makeLegalName(header);
           nameCapped = name.length() < header.length();
           description = "";
        }

        /*
         * next read the sequence
         */
        StringBuffer sequence = new StringBuffer();
        
        String s;
        do {
            reader.mark(1000);
            s = reader.readLine();
            if (s != null) {
                 if (s.length() > 0 && s.charAt(0) == '>') {
                    // this is the start of a next sequence
                    reader.reset(); 
                    s = null;
                 } else
                    sequence.append(checkLegal(s, reader.getLineNumber(), sequenceType));
            }
        } while (s != null);

        return new Sequence(name, nameCapped, description, sequence.toString());
    }
    
	public static Sequence readFastqFileSequence(LineNumberReader reader,
			int sequenceType) throws IOException, FileFormatException {

		String line;
		int control = 0;
		int lengthSequence = 0;
		int lengthQuality = 0;
		StringBuffer sequence = new StringBuffer();
		String name = "";
        String quality = "";
		do{
			reader.mark(1000);
			line=reader.readLine();
			if (line!=null){
				if ((control == 0) || (control > 1 && (control%4 == 0))){ //line 1
					//System.out.println(line.toString()+"->"+control+"%4=" + control%4);
					if (line.charAt(0) != '@'){
						throw new FileFormatException("Expecting a '@'", reader.getLineNumber());
					}
					if (!line.substring(1).matches("[^!#$%^&\\*()+]*")){
						throw new FileFormatException("Illegal character (one of '!@#$%^&\\*()+=')",reader.getLineNumber());
					}
					name = line.substring(1).toString();
				}
				if (control > 0 && (control-1)%4 == 0){ //line 2
					sequence.append(checkLegal(line, reader.getLineNumber(),sequenceType));
					lengthSequence = line.length();
				}
				if (control > 0 && ((control-3)%4 == 0)){ //line 4
					lengthQuality = line.length();
					if (lengthSequence != lengthQuality){
						throw new FileFormatException("Quality must contain the same number of symbols as letters in the sequence",reader.getLineNumber());
					}
					quality = line.toString();
					lengthSequence = 0;
					lengthQuality = 0;
					line = null;
				}
				control++;
			}else{
				return null; // no new sequence to be found
			}
		}while(line!=null);
		reader.reset();
		reader.readLine();
		return new Sequence(name, false, quality, sequence.toString());
	}

    private static String checkLegal(String s, int line, int sequenceType) throws FileFormatException {
    	s = s.replaceAll("[ \\t\\n\\r]", "");
    	
    	switch (sequenceType) {
    	case SEQUENCE_ANY:
    		if (!s.toUpperCase().matches("[A-Z\\-.*]*"))
    			throw new FileFormatException("Illegal character in input", line);
    		break;
    	case SEQUENCE_DNA:
    		// bionumerics gives an occasional I?
    		if (!s.toUpperCase().matches("[ACGITRYSWKMBDHVN\\-.*]*")) {
    			String illegal = "";
    			for (char c : s.toUpperCase().toCharArray()) {
    				if (!"ACGITRYSWKMBDHVN-".contains(c+"")) {
    					illegal += c;
    					break;
    				}
    			}
    			throw new FileFormatException("Illegal nucleotide character (" + illegal + ") in input", line);
    		}
    		break;
    	case SEQUENCE_AA:
    		if (!s.toUpperCase().matches("[ACDEFGHIKLMNPQRSTUVWXY\\-.*]*"))
    			throw new FileFormatException("Illegal amino acid character in input", line);
    	}
    		
    	return s;
	}

	/**
     * Sanitize the sequence name to not confuse phylogenetic software packages with
     * symbols that they cannot handle or too long sequence names.
     */
    private static String makeLegalName(String name) {
    	String sane = name.replaceAll("/|\\+|\\(|\\)", "");
		try {
			sane = new String(sane.getBytes("ASCII"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    	if (sane.length() > 30)
    		return sane.substring(0, 30);
    	else
    		return sane;
	}

	private void readNexusFile(InputStream inputFile, int sequenceType) throws IOException, FileFormatException {
        /*
         * The NEXUS format (for multiple sequences) as used by PAUP export
         * function (i.e. the non-interleaved format).
         */

        LineNumberReader reader
            = new LineNumberReader(new InputStreamReader(inputFile));

        String signature = reader.readLine();
        
        if (signature == null || !signature.trim().equals("#NEXUS"))
        	throw new FileFormatException("NEXUS file should start with '#NEXUS'", reader.getLineNumber());

        for (;;) {
        	String line = reader.readLine();
        	if (line == null)
        		throw new FileFormatException("NEXUS file did not contain sequence data", reader.getLineNumber());
        	if (line.toUpperCase().indexOf("MATRIX") != -1)
        		break;
        }
        
        for (;;) {
            Sequence s = readNexusFileSequence(reader, sequenceType);

            if (s != null) {
                sequences.add(s);
            } else
                return;
        }
	}
    
	private Sequence readNexusFileSequence(LineNumberReader reader, int sequenceType) throws IOException, FileFormatException {
		/*
		 * Format: name white-space sequence data.
		 */
		String line = reader.readLine();
		if (line == null)
			throw new FileFormatException("Unexpected EOF while reading sequence data", reader.getLineNumber());

		if (line.trim().equals(";"))
			return null;
		
		String a[] = line.split("\\s+");
		if (a.length != 2)
			throw new FileFormatException("Unexpected sequence line format", reader.getLineNumber());
		
		return new Sequence(a[0], false, "", checkLegal(a[1], reader.getLineNumber(), sequenceType));
	}

	public AbstractSequence getSequence(String sequenceName) {
		for (AbstractSequence s: sequences)
			if (s.getName().equals(sequenceName))
				return s;

		return null;
	}

	public List<AbstractSequence> getSequences() {
        return sequences;
    }

    public int getLength() {
        return sequences.get(0).getLength();
    }

    /**
     * Create a sequence alignment that is a window on the current alignment.
     * 
     * This uses the SubSequence class to efficiently create a new alignment without
     * copying the sequences.
     */
    public SequenceAlignment getSubSequence(int startIndex, int endIndex)
    {
        List<AbstractSequence> subSequences = new ArrayList<AbstractSequence>();
        
        Iterator<AbstractSequence> i = sequences.iterator();
        while (i.hasNext()) {
            AbstractSequence sequence = i.next();
            
            String name = sequence.getName();
            String description = sequence.getDescription() + " [" + startIndex + ", " + endIndex + "]";
            
            subSequences.add(new SubSequence(name, description, sequence, startIndex, endIndex));
        }

        return new SequenceAlignment(subSequences, sequenceType);
    }

    /**
     * Write the alignment to a file.
     */
    public void writeOutput(OutputStream outputFile, int fileType)
        throws IOException, ParameterProblemException
    {
        switch (fileType) {
            case FILETYPE_FASTA:
                writeFastaOutput(outputFile);
                break;
            case FILETYPE_CLUSTAL:
                //writeClustalOutput(outputFile);
                throw new ParameterProblemException("Writing clustal not yet supported");
            case FILETYPE_NEXUS:
                writeNexusOutput(outputFile);
                break;
            case FILETYPE_PHYLIP:
                writePhylipOutput(outputFile);
                break;
             default:
                throw new ParameterProblemException("Illegal value for fileType");
        }
    }
    
    void writeFastaOutput(OutputStream outputFile) throws IOException
    {
        /*
         * The fasta format (for multiple sequences) as described in
         * http://www.molbiol.ox.ac.uk/help/formatexamples.htm#fasta
         * and
         * http://www.ncbi.nlm.nih.gov/BLAST/fasta.html
         */

        Writer writer = new OutputStreamWriter(outputFile);
        final char endl = '\n';
         
        Iterator<AbstractSequence> i = sequences.iterator();
        
        while (i.hasNext()) {
            AbstractSequence seq = i.next();
            
            writer.write('>' + seq.getName() + " " + seq.getDescription() + endl);
            
            final int lineLength = 50;
            
            final int seqLength = seq.getLength();
            for (int j = 0; j < seqLength; j += lineLength) {
                int end = Math.min(j + lineLength, seqLength);
                writer.write(seq.getSequence().substring(j, end));
                writer.write(endl);
            }
        }
        
        writer.flush();
    }

    void writePhylipOutput(OutputStream outputFile) throws IOException
    {
        /*
         * The phylip format (for multiple sequences)
         */

        Writer writer = new OutputStreamWriter(outputFile);
        final char endl = '\n';

        writer.write(sequences.size() + " " + getLength() + endl);
        Iterator<AbstractSequence> i = sequences.iterator();
        
        Set<String> nameSet = new HashSet<String>();
        while (i.hasNext()) {
            AbstractSequence seq = (AbstractSequence) i.next();

            String name = nexusName(seq, nameSet, MAX_PHYLIP_TAXUS_LENGTH);
            nameSet.add(name);

            writer.write(padBack(new StringBuffer(name), MAX_PHYLIP_TAXUS_LENGTH + 2)
            		     + seq.getSequence() + endl);
        }
        
        writer.flush();
    }

    void writeNexusOutput(OutputStream outputFile) throws IOException
    {
        /*
         * The Nexus file format, as taken from an example file
         */
        Writer writer = new OutputStreamWriter(outputFile);
        final char endl = '\n';
        
        writer.write("#NEXUS" + endl);
        writer.write(endl);
        
        Set<String> nameSet = new HashSet<String>();
        List<String> nameList = new ArrayList<String>();

        for (AbstractSequence seq:sequences) {
            String name = nexusName(seq, nameSet, MAX_NEXUS_TAXUS_LENGTH);
            nameList.add(name);
            nameSet.add(name);
            writer.write("[Name: " + padBack(new StringBuffer(name), MAX_NEXUS_TAXUS_LENGTH + 2)
                        + "Len: " +  padBack(new StringBuffer().append(seq.getLength()), 10)
                        + "Check: 0]" + endl);
            
        }
        
        writer.write(endl);
        
        String dataType[] = { "DNA", "protein" };
        
        writer.write("begin data;" + endl);
        writer.write(" dimensions ntax=" + sequences.size() + " nchar=" + getLength() + ";" + endl);
        writer.write(" format datatype=" + dataType[sequenceType] + " interleave missing=? gap=-;" + endl);
        writer.write("  matrix" + endl);

        final int blockUnit = 20;
        final int blockUnitsPerBlock = 5;
        for (int j = 0; j < getLength(); j += (blockUnit * blockUnitsPerBlock)) {
            for (int i = 0; i < sequences.size(); ++i) {
                AbstractSequence seq = sequences.get(i);
                
                writer.write(new String(padFront(new StringBuffer((String) nameList.get(i)), MAX_NEXUS_TAXUS_LENGTH + 2)));
                for (int k = 0; k < blockUnitsPerBlock; ++k) {
                    int start = j + (k * blockUnit);
                    int end = Math.min(start + blockUnit, seq.getSequence().length());
                    String s = seq.getSequence().substring(start, end);
                    
                    writer.write(" " + s);
                    if (s.length() < blockUnit)
                        break;
                }
                writer.write(endl);
            }
            
            writer.write(endl);
        }
        
        writer.write("  ;" + endl);
        writer.write("end;" + endl);
        writer.flush();
    }

    String nexusName(AbstractSequence seq, Set<String> names, int maxlength) {
        String name = seq.getName();
        name = name.substring(0, Math.min(maxlength, name.length()));
        name = name.replace('-', '_');
        name = name.replace(',', '_');
        name = name.replace('/', '_');
        name = name.replace('=', '_');
        if (names.contains(name)) {
  
            String base = name.substring(0, name.length() - 3) + '_';

            int c = 0;
            do {
                if (c < 10)    
                    name = base + '0' + c;
                else
                    name = base + c;
                ++c;
            } while (names.contains(name));
        }

        try {
        	Integer.parseInt(name);
        	name = "_" + name;
        } catch (NumberFormatException e) {        	
        }
        
        return name;
    }

    private static StringBuffer padBack(StringBuffer s, int total) {
        StringBuffer result = s;
        int numAdded = total - s.length();
        for (int i = 0; i < numAdded; ++i)
          result.append(' ');
        
        return result;
    }

    private static StringBuffer padFront(StringBuffer s, int total) {
        StringBuffer result = new StringBuffer();
        int numAdded = total - s.length();
        for (int i = 0; i < numAdded; ++i)
          result.append(' ');

        result.append(s);

        return result;
    }

	public void reverseTaxa() {
		Collections.reverse(sequences);
	}

	public int getSequenceType() {
		return sequenceType;
	}

	public static String sequenceTypeName(int sequenceType) {
		switch (sequenceType) {
		case SEQUENCE_ANY:
			return "Any";
		case SEQUENCE_DNA:
			return "DNA";
		case SEQUENCE_AA:
			return "Amino acid";
		}
		return "Unknown sequence type " + sequenceType;
	}
	
	public void setSequenceType(int i) {
		sequenceType = i;
	}
    
    public void removeAllGapSequences() {
        for (int i = 0; i < sequences.size(); ++i) {
            Sequence s = (Sequence) sequences.get(i);
            if (s.firstNonGapPosition() == s.getLength()) {
                sequences.remove(i);
                --i;
            }
        }
    }

    public int firstNonGapPosition() {
    	int firstNonGap = 0;

    	for (int i = 0; i < sequences.size(); ++i) {
            Sequence s = (Sequence) sequences.get(i);
            firstNonGap = Math.max(firstNonGap, s.firstNonGapPosition());
    	}
    	
    	return firstNonGap;
	}

    public int lastNonGapPosition() {
    	int lastNonGap = getLength();

    	for (int i = 0; i < sequences.size(); ++i) {
            Sequence s = (Sequence) sequences.get(i);
            lastNonGap = Math.min(lastNonGap, s.lastNonGapPosition());
    	}
    	
    	return lastNonGap;
	}

    public AbstractSequence findSequence(String name) {
    	name = name.replaceAll(":", "_").replaceAll(",", "_"); // Apparently, clustal will do that!
        for (int i = 0; i < sequences.size(); ++i)
            if (sequences.get(i).getName().equals(name))
                return sequences.get(i);

        return null;
    }

    public int getIndex(AbstractSequence sequence) {
        return sequences.indexOf(sequence);
    }

    public void addSequence(AbstractSequence sequence) {
    	sequences.add(sequence);
    }

    public boolean removeSequence(AbstractSequence sequence) {
    	return sequences.remove(sequence);
    }

    public boolean removeSequence(String sequenceName) {
    	for(AbstractSequence s: sequences)
    		if(s.getName().equals(sequenceName))
    			return sequences.remove(s);

    	return false;
    }
    
    public SequenceAlignment selectSequencesFromClusters(List<Cluster> clusters) {
		List<String> sequences = new ArrayList<String>();
		Set<String> clusterSequences = new LinkedHashSet<String>();

		for (int i = 0; i < clusters.size(); ++i) {
			List<String> taxa = clusters.get(i).getTaxaIds();
			clusterSequences.addAll(taxa);
		}
		sequences.addAll(clusterSequences);

		return selectSequences(sequences);
	}
    
    public SequenceAlignment selectSequences(List<String> selection) {
        List<AbstractSequence> selected = new ArrayList<AbstractSequence>();
        for (int i = 0; i < selection.size(); ++i) {
            AbstractSequence seq = findSequence(selection.get(i));
            if (seq == null) {
                System.err.println("Could not find sequence: \""
                        + selection.get(i) + "\"");
            } else
            	selected.add(seq);
        }
        
        return new SequenceAlignment(selected, sequenceType);
    }

	public int getAlignmentScore() {
		return alignmentScore;
	}

	public void setAlignmentScore(int alignmentScore) {
		this.alignmentScore = alignmentScore;
	}

}
