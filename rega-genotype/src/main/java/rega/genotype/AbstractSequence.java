package rega.genotype;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * An interface for accessing sequence information
 */
public abstract class AbstractSequence {
    /**
     * The sequence name
     */
    public abstract String getName();
    
    /**
     * The sequence description
     */
    public abstract String getDescription();
    
    /**
     * The sequence itself
     */
    public abstract String getSequence();

    /**
     * The sequence length (== getSequence().getLength())
     */
    public abstract int getLength();

	public abstract void removeChar(int i);

	public abstract AbstractSequence sourceSequence();
	
    public void writeFastaOutput(OutputStream f) throws IOException {
        List<AbstractSequence> thisseq = new ArrayList<AbstractSequence>();
        thisseq.add(this);
        SequenceAlignment sa = new SequenceAlignment(thisseq, SequenceAlignment.SEQUENCE_DNA);
        sa.writeFastaOutput(f);
    }

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