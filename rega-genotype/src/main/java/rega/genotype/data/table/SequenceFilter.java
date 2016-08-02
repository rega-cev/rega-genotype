package rega.genotype.data.table;

import rega.genotype.data.GenotypeResultParser;

public interface SequenceFilter {
	public boolean excludeSequence(GenotypeResultParser parser);
}
