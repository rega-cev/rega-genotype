package rega.genotype.data.table;

import java.io.IOException;

import org.xml.sax.SAXException;

import rega.genotype.ngs.NgsResultsParser;
import rega.genotype.ngs.model.ConsensusBucket;
import rega.genotype.ngs.model.Contig;
import rega.genotype.ngs.model.NgsResultsModel;
import rega.genotype.util.DataTable;

public class NgsTableGenerator extends NgsResultsParser {
	private DataTable table;

	public NgsTableGenerator(DataTable table) throws IOException {
		super();
		this.table = table;

		table.addLabel("Rega Assignment");
		table.addLabel("Contigs number");
		table.addLabel("Contigs start");
		table.addLabel("Contigs end");
		table.addLabel("# Reads");
		table.addLabel("Coverage (%)");
		table.addLabel("Depth of coverage");
		table.addLabel("Source");
		table.addLabel("ref");
		table.addLabel("ref name");

		table.newRow();
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();

		for (ConsensusBucket bucket: model.getConsensusBuckets()) {
			int contigNumber = 1;
			for (Contig contig: bucket.getContigs()) {
				try {
					table.addLabel(bucket.getConcludedName());
					table.addNumber(contigNumber);
					table.addNumber(contig.getStartPosition());
					table.addNumber(contig.getEndPosition());
					table.addNumber(contig.getReadCount(model.getReadLength()));
					table.addNumber(contig.getCovPercentage(bucket.getRefLen()));
					table.addNumber(contig.getDeepCov(model.getReadLength()));
					table.addLabel(bucket.getSrcDatabase());
					table.addLabel(bucket.getRefName());
					table.addLabel(bucket.getRefDescription());
					table.newRow();
				} catch (IOException e) {
					e.printStackTrace();
				}
				contigNumber++;
			}
		}
		try {
			table.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
