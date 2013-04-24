package rega.genotype.ui.data;

import rega.genotype.data.GenotypeResultParser;

public interface SequenceFilter {
	public boolean excludeSequence(GenotypeResultParser parser);
}
