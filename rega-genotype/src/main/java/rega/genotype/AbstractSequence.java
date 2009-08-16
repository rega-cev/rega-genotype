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
}