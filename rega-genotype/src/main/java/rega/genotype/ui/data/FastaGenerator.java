package rega.genotype.ui.data;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import rega.genotype.data.GenotypeResultParser;
import rega.genotype.data.table.SequenceFilter;
import rega.genotype.ui.util.GenotypeLib;

/**
 * FastaGenerator is an implementation of GenotypeResultParser capable of writing in fasta format.
 */
public class FastaGenerator extends GenotypeResultParser {
	private SequenceFilter filter;
	private OutputStreamWriter writer;
	
	public FastaGenerator(SequenceFilter filter, OutputStream outputStream) {
		super(-1);

		this.filter = filter;
		this.writer = new OutputStreamWriter(outputStream);
	}

	@Override
	public void endSequence() {
		super.endSequence();

		try {
			writer.write(">");
			writer.write(GenotypeLib.getEscapedValue(this, "/genotype_result/sequence/@name"));
			writer.write("\n");
			writer.write(GenotypeLib.getEscapedValue(this, "/genotype_result/sequence/nucleotides"));
			writer.write("\n");
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public boolean skipSequence() {
    	return filter.excludeSequence(this);
    }
}