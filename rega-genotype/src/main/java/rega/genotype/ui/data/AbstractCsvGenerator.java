package rega.genotype.ui.data;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public abstract class AbstractCsvGenerator extends SaxParser {
	private Writer w;

	public AbstractCsvGenerator(Writer w) {
		this.w = w;
	}

	@Override
	public void endSequence() {
		try {
			writeLine(w);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public abstract void writeLine(Writer l) throws IOException;
	

    protected String addCsvValue(String name) {
    	return addCsvValue(name, false);
    }

    protected String addCsvValue(String name, boolean isFirst) {
    	String val = getValue(name);
    	if (val==null)
    		val="";
    	val = "\"" + val + "\"";
    	
    	if (isFirst)
    		return val;
    	else
    		return "," + val;
    }

	protected void addPhyloResults(StringBuilder csvLine, String analysisId) {
		csvLine.append(addCsvValue("genotype_result.sequence.result['" + analysisId + "'].best.id"));
		csvLine.append(addCsvValue("genotype_result.sequence.result['" + analysisId + "'].best.support"));
		csvLine.append(addCsvValue("genotype_result.sequence.result['" + analysisId + "'].best.inner"));
		csvLine.append(addCsvValue("genotype_result.sequence.result['" + analysisId + "'].best.outer"));
	}

	protected void addPhyloScanResults(StringBuilder csvLine, String analysisId) {
		csvLine.append(addCsvValue("genotype_result.sequence.result['" + analysisId + "'].support['assigned']"));
		csvLine.append(addCsvValue("genotype_result.sequence.result['" + analysisId + "'].support['best']"));
		csvLine.append(addCsvValue("genotype_result.sequence.result['" + analysisId + "'].nosupport['best']"));
		csvLine.append(addCsvValue("genotype_result.sequence.result['" + analysisId + "'].profile['assigned']"));
		csvLine.append(addCsvValue("genotype_result.sequence.result['" + analysisId + "'].profile['best']"));
	}
}
