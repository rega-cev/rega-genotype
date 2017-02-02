package rega.genotype.ui.framework.widgets;

import java.io.File;
import java.io.IOException;

import rega.genotype.Constants;
import rega.genotype.Constants.Mode;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.data.table.NgsTableGenerator;
import rega.genotype.data.table.SequenceFilter;
import rega.genotype.ngs.NgsFileSystem;
import rega.genotype.ui.data.FastaGenerator;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.util.CsvDataTable;
import rega.genotype.util.DataTable;
import rega.genotype.util.XlsDataTable;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WResource;
import eu.webtoolkit.jwt.WResource.DispositionType;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTemplate;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;

public class DownloadsWidget extends WTemplate{

	private File jobDir;
	private OrganismDefinition organismDefinition;
	private SequenceFilter filter;
	private Mode mode;

	public DownloadsWidget(SequenceFilter filter, File jobDir, 
			OrganismDefinition organismDefinition, boolean hasXmlAnchor,
			Mode mode) {
		super(tr("job-overview-downloads"));
		this.filter = filter;
		this.jobDir = jobDir;
		this.organismDefinition = organismDefinition;
		this.mode = mode;

		WAnchor xmlAnchor = null;
		if (hasXmlAnchor)
			xmlAnchor = createXmlDownload();
		bindWidget("xml-file", xmlAnchor);
		bindWidget("csv-file", createTableDownload(tr("monitorForm.csvTable"), true));
		bindWidget("xls-file", createTableDownload(tr("monitorForm.xlsTable"), false));

		WContainerWidget fasta;
		if (mode == Mode.Classical)
			fasta = createFastaDownload(Constants.SEQUENCES_FILE_NAME, tr("monitorForm.fasta"));
		else {
			fasta = new WContainerWidget();
			fasta.setInline(true);
			fasta.addWidget(createFastaDownload(NgsFileSystem.CONTIGS_FILE, tr("monitorForm.contigs")));
			fasta.addWidget(new WText(" "));
			fasta.addWidget(createFastaDownload(NgsFileSystem.CONSENSUSES_FILE, tr("monitorForm.consensuses")));
		}
		bindWidget("fasta-file", fasta);
	}

	private WAnchor createXmlDownload() {
		WAnchor xmlFileDownload = new WAnchor("", tr("monitorForm.xmlFile"));
		xmlFileDownload.setObjectName("xml-download");
		xmlFileDownload.setStyleClass("link");
		String resultsFileName = mode == Mode.Ngs ? Constants.NGS_RESULT_FILE_NAME : Constants.RESULT_FILE_NAME;
		WResource xmlResource = new WFileResource("application/xml", jobDir.getAbsolutePath() + File.separatorChar + resultsFileName);
		xmlResource.suggestFileName(Constants.RESULT_FILE_NAME);
		xmlResource.setDispositionType(DispositionType.Attachment);
		xmlFileDownload.setLink(new WLink(xmlResource));
		return xmlFileDownload;
	}
	
	private WAnchor createFastaDownload(String fileName, WString anchorName) {
		WAnchor fastaDownload = new WAnchor("", anchorName);
		fastaDownload.setObjectName("fasta-download");
		fastaDownload.setStyleClass("link");

		WResource fastaResource;
		if (filter != null) {
			fastaResource = new WResource() {
				@Override
				protected void handleRequest(WebRequest request, WebResponse response) throws IOException {
					response.setContentType("text/plain");
					FastaGenerator generateFasta = new FastaGenerator(filter, response.getOutputStream(), mode);
					generateFasta.parseResultFile(new File(jobDir.getAbsolutePath()), mode);
				}
			};
		} else {
			fastaResource = new WFileResource("text/plain",
					jobDir.getAbsolutePath() + File.separatorChar + Constants.SEQUENCES_FILE_NAME);
		}

		fastaResource.suggestFileName(fileName);
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
				GenotypeResultParser tableGen;
				if (mode == Mode.Ngs)
					tableGen = new NgsTableGenerator(t);
				else
					tableGen = organismDefinition.getDataTableGenerator(filter, t);
				tableGen.parseResultFile(new File(jobDir.getAbsolutePath()), mode);
			}
			
		};
		csvResource.suggestFileName("results." + (csv ? "csv" : "xls"));
		csvResource.setDispositionType(DispositionType.Attachment);
		csvTableDownload.setLink(new WLink(csvResource));

		return csvTableDownload;
	}
}
