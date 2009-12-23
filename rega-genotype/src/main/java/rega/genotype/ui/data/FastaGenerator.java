package rega.genotype.ui.data;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import rega.genotype.ui.forms.AbstractJobOverview;

/**
 * FastaGenerator is an implementation of GenotypeResultParser capable of writing in fasta format.
 */
public class FastaGenerator extends GenotypeResultParser {
	private AbstractJobOverview jobOverview;
	private OutputStreamWriter writer;
	
	public FastaGenerator(AbstractJobOverview jobOverview, OutputStream outputStream) {
		this.jobOverview = jobOverview;
		this.writer = new OutputStreamWriter(outputStream);
	}

	@Override
	public void endSequence() {
		try {
			writer.write(">");
			writer.write(getEscapedValue("/genotype_result/sequence/@name"));
			writer.write("\n");
			writer.write(getEscapedValue("/genotype_result/sequence/nucleotides"));
			writer.write("\n");
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public boolean skipSequence() {
    	return jobOverview.isExcludedByFilter(this);
    }
}