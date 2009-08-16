/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * An abstract base class for sequences information.
 */
public abstract class AbstractSequence {
    /**
     * @return the sequence name
     */
    public abstract String getName();
    
    /**
     * @return the sequence description
     */
    public abstract String getDescription();
    
    /**
     * @return the sequence itself
     */
    public abstract String getSequence();

    /**
     * @return he sequence length (== getSequence().getLength())
     */
    public abstract int getLength();

    /**
     * Deletes a token from the sequence, reducing the length by 1.
     */
	public abstract void removeChar(int i);

    /**
     * Returns the original sequence that this sequence is derived from.
     * 
     * This may be itself or another sequence if the sequence was obtained
     * by taking a subsequence or as a reverse complement.
     */
	public abstract AbstractSequence sourceSequence();

	/**
	 * Write the sequence in FASTA format to the output stream.
	 */
    public void writeFastaOutput(OutputStream f) throws IOException {
        List<AbstractSequence> thisseq = new ArrayList<AbstractSequence>();
        thisseq.add(this);
        SequenceAlignment sa = new SequenceAlignment(thisseq, SequenceAlignment.SEQUENCE_DNA);
        sa.writeFastaOutput(f);
    }

    /**
     * Compute the reverse compliment of the sequence, and return this as
     * a new sequence.
     * 
     * @return a new sequence that is the reverse complement.
     */
	public AbstractSequence reverseCompliment() {
		String sequence = getSequence().toUpperCase();
		StringBuffer s = new StringBuffer();
		for (int i = 0; i < sequence.length(); ++i) {
			char ch = sequence.charAt(sequence.length() - i - 1);
			switch (ch) {
			case 'A':
				ch = 'T'; break;
			case 'C':
				ch = 'G'; break;
			case 'G':
				ch = 'C'; break;
			case 'T':
				ch = 'A'; break;
			case 'M':
				ch = 'K'; break;
			case 'R':
				ch = 'Y'; break;
			case 'W':
				ch = 'W'; break;
			case 'S':
				ch = 'S'; break;
			case 'Y':
				ch = 'R'; break;
			case 'K':
				ch = 'M'; break;
			case 'B':
				ch = 'V'; break;
			case 'D':
				ch = 'H'; break;
			case 'H':
				ch = 'D'; break;
			case 'V':
				ch = 'B'; break;
			case 'N':
				ch = 'N'; break;
			default:
			}
			s.append(ch);
		}
		
		Sequence result = new Sequence(getName(), getDescription(), s.toString());
		result.setSourceSequence(this);
		
		return result;
	}
}