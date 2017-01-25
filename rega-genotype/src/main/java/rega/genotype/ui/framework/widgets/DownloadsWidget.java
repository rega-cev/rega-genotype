package rega.genotype.ui.framework.widgets;

import java.io.File;
import java.io.IOException;

import rega.genotype.data.table.AbstractDataTableGenerator;
import rega.genotype.data.table.SequenceFilter;
import rega.genotype.ui.data.FastaGenerator;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.util.CsvDataTable;
import rega.genotype.util.DataTable;
import rega.genotype.util.XlsDataTable;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WResource;
import eu.webtoolkit.jwt.WResource.DispositionType;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTemplate;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;

public class DownloadsWidget extends WTemplate{

	private File jobDir;
	private OrganismDefinition organismDefinition;
	private SequenceFilter filter;

	public DownloadsWidget(SequenceFilter filter, File jobDir, OrganismDefinition organismDefinition, boolean hasXmlAnchor) {
		super(tr("job-overview-downloads"));
		this.filter = filter;
		this.jobDir = jobDir;
		this.organismDefinition = organismDefinition;

		WAnchor xmlAnchor = null;
		if (hasXmlAnchor)
			xmlAnchor = createXmlDownload();
		bindWidget("xml-file", xmlAnchor);
		bindWidget("csv-file", createTableDownload(tr("monitorForm.csvTable"), true));
		bindWidget("xls-file", createTableDownload(tr("monitorForm.xlsTable"), false));

		WAnchor fastaAnchor = createFastaDownload();

		bindWidget("fasta-file", fastaAnchor);
	}

	private WAnchor createXmlDownload() {
		WAnchor xmlFileDownload = new WAnchor("", tr("monitorForm.xmlFile"));
		xmlFileDownload.setObjectName("xml-download");
		xmlFileDownload.setStyleClass("link");
		WResource xmlResource = new WFileResource("application/xml", jobDir.getAbsolutePath() + File.separatorChar + "result.xml");
		xmlResource.suggestFileName("result.xml");
		xmlResource.setDispositionType(DispositionType.Attachment);
		xmlFileDownload.setLink(new WLink(xmlResource));
		return xmlFileDownload;
	}
	
	private WAnchor createFastaDownload() {
		WAnchor fastaDownload = new WAnchor("", tr("monitorForm.fasta"));
		fastaDownload.setObjectName("fasta-download");
		fastaDownload.setStyleClass("link");

		WResource fastaResource;
		if (filter != null) {
			fastaResource = new WResource() {
				@Override
				protected void handleRequest(WebRequest request, WebResponse response) throws IOException {
					response.setContentType("text/plain");
					FastaGenerator generateFasta = new FastaGenerator(filter, response.getOutputStream());
					generateFasta.parseResultFile(new File(jobDir.getAbsolutePath()));
				}
			};
		} else {
			fastaResource = new WFileResource("text/plain", jobDir.getAbsolutePath() + File.separatorChar + "sequences.fasta");
		}

		fastaResource.suggestFileName("sequences.fasta");
		fastaResource.setDispositionType(DispositionType.Attachment);
		fastaDownload.setLink(new WLink(fastaResource));

		return fastaDownload;
	}

	private WAnchor createTableDownload(WString label, final boolean csv) {
		WAnchor csvTableDownload = new WAnchor("", label);
		csvTableDownload.setObjectName("csv-table-download");
		csvTableDownload.setStyleClass("link");

		WResource csvResource = new WResource() {
			@Override
			protected void handleRequest(WebRequest request, WebResponse response) throws IOException {
				response.setContentType("application/excel");
				DataTable t = csv ? new CsvDataTable(response.getOutputStream(), ',', '"') : new XlsDataTable(response.getOutputStream());
				AbstractDataTableGenerator acsvgen = 
					organismDefinition.getDataTableGenerator(filter, t);
				acsvgen.parseResultFile(new File(jobDir.getAbsolutePath()));
			}
			
		};
		csvResource.suggestFileName("results." + (csv ? "csv" : "xls"));
		csvResource.setDispositionType(DispositionType.Attachment);
		csvTableDownload.setLink(new WLink(csvResource));

		return csvTableDownload;
	}
}
