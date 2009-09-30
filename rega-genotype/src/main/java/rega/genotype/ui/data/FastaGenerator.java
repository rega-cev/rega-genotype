package rega.genotype.ui.data;

import java.io.IOException;
import java.io.OutputStream;

import rega.genotype.ui.forms.AbstractJobOverview;

/**
 * FastaGenerator is an implementation of GenotypeResultParser capable of writing in fasta format.
 */
public class FastaGenerator extends GenotypeResultParser {
	private AbstractJobOverview jobOverview;
	private OutputStream outputStream;
	
	public FastaGenerator(AbstractJobOverview jobOverview, OutputStream outputStream) {
		this.jobOverview = jobOverview;
		this.outputStream = outputStream;
	}

	@Override
	public void endSequence() {
		try {
			outputStream.write(getEscapedValue("/genotype_result/sequence[@id='name']").getBytes());
			outputStream.write("\n".getBytes());
			outputStream.write(getEscapedValue("/genotype_result/sequence/nucleotides").getBytes());
			outputStream.write("\n".getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public boolean skipSequence() {
    	return jobOverview.isExcludedByFilter(this);
    }
}