package rega.genotype.ui.data;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import rega.genotype.data.GenotypeResultParser;
import rega.genotype.data.table.SequenceFilter;
import rega.genotype.ui.framework.Constants.Mode;
import rega.genotype.ui.util.GenotypeLib;

/**
 * FastaGenerator is an implementation of GenotypeResultParser capable of writing in fasta format.
 */
public class FastaGenerator extends GenotypeResultParser {
	private SequenceFilter filter;
	private OutputStreamWriter writer;
	private Mode mode;

	public FastaGenerator(SequenceFilter filter, OutputStream outputStream, Mode mode) {
		super(-1);

		this.filter = filter;
		this.mode = mode;
		this.writer = new OutputStreamWriter(outputStream);
	}

	@Override
	public void endSequence() {
		super.endSequence();

		try {
			writer.write(">");
			String seqPath =  mode == Mode.Classical ? "/genotype_result/sequence" 
					: "/genotype_result/assembly/bucket/sequence";
			
			String namePath = seqPath + "/@name";
			String descPath = seqPath + "/sequence/result[@id='blast']/cluster/concluded-description";
			String nuclPath = seqPath +  "/sequence/nucleotides";

			writer.write(GenotypeLib.getEscapedValue(this, namePath));
			String description = GenotypeLib.getEscapedValue(this, descPath);
			if (description != null) // support old result.xml files
				writer.write(description.isEmpty() ? "__unassigned" : description);

			writer.write("\n");
			writer.write(GenotypeLib.getEscapedValue(this, nuclPath));
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