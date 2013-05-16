/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import java.io.File;
import java.io.IOException;
import java.util.List;

import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.data.AbstractDataTableGenerator;
import rega.genotype.ui.data.FastaGenerator;
import rega.genotype.ui.data.SequenceFilter;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.CsvDataTable;
import rega.genotype.ui.util.DataTable;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.ui.util.XlsDataTable;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WResource;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WTableCell;
import eu.webtoolkit.jwt.WTemplate;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTimer;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;

/**
 * An abstract class for the form that shows the overview of a running or finished job.
 * 
 * It will check for itself if an analysis is still running (by looking for a DONE file), and until
 * the job is done, it will use a timer to append result rows to the table.
 * 
 * You should implement the getHeader() and getData() methods to customize the contents of
 * the table.
 */
public abstract class AbstractJobOverview extends AbstractForm {
	public class Header {
		public WString name;
		public int span;

		public Header(WString aName) {
			this(aName, 1);
		}

		public Header(WString aName, int aSpan) {
			name = aName;
			span = aSpan;
		}
	}
	
	protected File jobDir;
	private WTable jobTable;
	private JobOverviewSummary summary;
	private SequenceFilter filter;
	
	private WTimer updater;
	
	private WTemplate template;
	
	public AbstractJobOverview(GenotypeWindow main) {
		super(main);
		
		template = new WTemplate(tr("job-overview-form"), this);
		template.addFunction("tr", WTemplate.Functions.tr);

		template.bindString("app.base.url", GenotypeMain.getApp().getEnvironment().getDeploymentPath());
		template.bindString("app.context", GenotypeMain.getApp().getServletContext().getContextPath());
		
		jobTable = new WTable(this);
		template.bindWidget("results", jobTable);
		jobTable.setHeaderCount(1, Orientation.Horizontal);
		jobTable.setHeaderCount(1, Orientation.Vertical);
		jobTable.setStyleClass("jobTable");
		jobTable.setObjectName("job-table");
	}

	public void init(final String jobId, final String filter) {
		this.jobDir = getJobDir(jobId);

		this.summary = getSummary(filter);
		template.bindWidget("summary", this.summary);

		this.filter = new SequenceFilter() {
			public boolean excludeSequence(GenotypeResultParser p) {
				String assignment = GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/conclusion/assigned/name");
				return filter != null && !filter.equals(summary.encodeAssignment(assignment));
			}			
		};
		
		jobTable.clear();
		
		resetTimer();
		
		if (!jobDone()) {
			updater = new WTimer();
			updater.setInterval(getMain().getOrganismDefinition().getUpdateInterval());
			updater.timeout().addListener(this, new Signal.Listener() {
				public void trigger() {
					fillTable(filter);
					updateInfo();
					if (jobDone()) {
						resetTimer();
						template.bindWidget("downloads", createDownloadsWidget(filter));
					}
				}
			});
		}

		fillTable(filter);
		updateInfo();
		if (jobDone())
			template.bindWidget("downloads", createDownloadsWidget(filter));
		else
			template.bindWidget("downloads", null);

		if (updater != null)
			updater.start();
	}
	
	private void resetTimer() {
		if (updater != null) {
			updater.stop();
			updater = null;
		}
	}
	
	private boolean jobDone() {
		File jobDone = new File(jobDir.getAbsolutePath() + File.separatorChar + "DONE");
		return jobDone.exists();
	}
	
	private boolean jobCancelled() {
		return new File(jobDir, ".CANCEL").exists();
	}
	
	private void updateInfo() {
		WTemplate analysisInProgress = null;
		if (!jobDone()) {
			analysisInProgress = new WTemplate(tr("monitorForm.analysisInProgress"));
			WPushButton cancelButton = new WPushButton(tr("monitorForm.cancelButton"));
			analysisInProgress.bindWidget("cancel-button", cancelButton);
			cancelButton.clicked().addListener(analysisInProgress, new Signal1.Listener<WMouseEvent>(){
				public void trigger(WMouseEvent arg) {
					try {
						new File(jobDir, ".CANCEL").createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		}
		
		WString analysisCancelled = new WString();
		if (jobDone() && jobCancelled())
			analysisCancelled = tr("monitorForm.analysisCancelled");
		template.bindString("analysis-cancelled", analysisCancelled);
		template.bindWidget("analysis-in-progress", analysisInProgress);
	}
	
	private void fillTable(String filter) {
		if (jobTable.getRowCount()==0) {
			List<Header> headers = getHeaders();

			int col = 0;
			for(int i = 0; i<headers.size(); i++) {
				Header h = headers.get(i);
				WText hh = new WText(h.name);
				hh.setId("");
				jobTable.getElementAt(0, col).addWidget(hh);
				jobTable.getElementAt(0, col).setId("");
				jobTable.getElementAt(0, col).setStyleClass("jobTableHeader");				
				jobTable.getElementAt(0, col).setColumnSpan(h.span);

				for (int j = 0; j < h.span; ++j) {
					jobTable.getColumnAt(col + j).setStyleClass((j > 0 ? "nlb " : "") + (j < h.span - 1 ? "nrb" : ""));
					jobTable.getColumnAt(col + j).setId("");
				}

				col += h.span;
			}
			
			jobTable.getRowAt(0).setId("");
		}
		
		new Parser().parseFile(jobDir);
	}
	
	private WWidget createDownloadsWidget(final String filter) {
		WTemplate t = new WTemplate(tr("job-overview-downloads"));
		
		WAnchor xmlAnchor = null;
		if (filter == null)
			xmlAnchor = createXmlDownload();
		t.bindWidget("xml-file", xmlAnchor);
		
		t.bindWidget("csv-file", createTableDownload(tr("monitorForm.csvTable"), true));

		t.bindWidget("xls-file", createTableDownload(tr("monitorForm.xlsTable"), false));
		
		WAnchor fastaAnchor = null;
		if (filter != null)
			fastaAnchor = createFastaDownload();
		t.bindWidget("fasta-file", fastaAnchor);
		
		return t;
	}

	private WAnchor createXmlDownload() {
		WAnchor xmlFileDownload = new WAnchor("", tr("monitorForm.xmlFile"));
		xmlFileDownload.setObjectName("xml-download");
		xmlFileDownload.setTarget(AnchorTarget.TargetNewWindow);
		xmlFileDownload.setStyleClass("link");
		WResource xmlResource = new WFileResource("application/xml", jobDir.getAbsolutePath() + File.separatorChar + "result.xml");
		xmlResource.suggestFileName("result.xml");
		xmlFileDownload.setRef(xmlResource.generateUrl());
		return xmlFileDownload;
	}
	
	private WAnchor createFastaDownload() {
		WAnchor fastaDownload = new WAnchor("", tr("monitorForm.fasta"));
		fastaDownload.setObjectName("fasta-download");
		fastaDownload.setStyleClass("link");
		fastaDownload.setTarget(AnchorTarget.TargetNewWindow);

		WResource fastaResource = new WResource() {
			@Override
			protected void handleRequest(WebRequest request, WebResponse response) throws IOException {
				response.setContentType("text/plain");
				
				FastaGenerator generateFasta = new FastaGenerator(filter, response.getOutputStream());
				generateFasta.parseFile(new File(jobDir.getAbsolutePath()));
			}
			
		};
		fastaResource.suggestFileName("sequences.fasta");
		fastaDownload.setResource(fastaResource);

		return fastaDownload;
	}
	
	private WAnchor createTableDownload(WString label, final boolean csv) {
		WAnchor csvTableDownload = new WAnchor("", label);
		csvTableDownload.setObjectName("csv-table-download");
		csvTableDownload.setStyleClass("link");
		csvTableDownload.setTarget(AnchorTarget.TargetNewWindow);

		WResource csvResource = new WResource() {
			@Override
			protected void handleRequest(WebRequest request, WebResponse response) throws IOException {
				response.setContentType("application/excel");
				DataTable t = csv ? new CsvDataTable(response.getOutputStream(), ';', '"') : new XlsDataTable(response.getOutputStream());
				AbstractDataTableGenerator acsvgen = 
					AbstractJobOverview.this.getMain().getOrganismDefinition().getDataTableGenerator(AbstractJobOverview.this, t);
				acsvgen.parseFile(new File(jobDir.getAbsolutePath()));
			}
			
		};
		csvResource.suggestFileName("results." + (csv ? "csv" : "xls"));
		csvTableDownload.setResource(csvResource);

		return csvTableDownload;
	}

	private class Parser extends GenotypeResultParser {		
		@Override
		public void endSequence() {
			int numRows = AbstractJobOverview.this.jobTable.getRowCount()-1;
			if(getSequenceIndex() - getFilteredSequences() >= numRows) {
				List<WWidget> data = getData(this);
				
				int row = jobTable.getRowCount();
				for (int i = 0; i < data.size(); i++) {
					WTableCell cell = jobTable.getElementAt(row, i);
					cell.setId("");
					cell.addWidget(data.get(i));
					if (data.get(i).getObjectName().length() == 0)
						data.get(i).setId("");
					
					if (WApplication.getInstance().getEnvironment().getUserAgent().indexOf("MSIE") != -1)
						cell.setStyleClass(jobTable.getColumnAt(i).getStyleClass());
				}
				jobTable.getRowAt(jobTable.getRowCount() - 1).setId("");
				
				if (summary != null) 
					summary.update(this, getMain().getOrganismDefinition());
			}
		}

		@Override
		public boolean skipSequence() {
			return filter.excludeSequence(this);
		}
	};
	
	public abstract List<Header> getHeaders();
	
	public abstract List<WWidget> getData(GenotypeResultParser p);

	public abstract JobOverviewSummary getSummary(String filter);

	protected WAnchor createReportLink(final GenotypeResultParser p) {
		return new WAnchor(new WLink(WLink.Type.InternalPath, "/job/" + jobId(jobDir) + "/" + JobForm.SEQUENCE_PREFIX + p.getSequenceIndex()), tr("monitorForm.report"));
	}
	
	public boolean existsJob(String jobId) {
		return getJobDir(jobId).exists();
	}

	public File getJobDir(String jobId) {
		return new File(Settings.getInstance().getJobDir(getMain().getOrganismDefinition()).getAbsolutePath()+File.separatorChar+jobId);
	}

	public static String reportPath(File jobDir, int sequenceIndex) {
		return jobPath(jobDir) + '/' + String.valueOf(sequenceIndex);
	}

	public static String jobPath(File jobDir) {
		return '/' + JobForm.JOB_URL + '/' + jobId(jobDir);
	}
	
	public String getJobPath() {
		return jobPath(jobDir);
	}
	
	protected WImage createGenomeImage(final GenotypeResultParser p, final String assignedId, boolean unassigned) {
		String startV = p.getValue("/genotype_result/sequence/result[@id='blast']/start");
		final int start = unassigned || startV == null ? -1 : Integer.parseInt(startV);
		String endV = p.getValue("/genotype_result/sequence/result[@id='blast']/end");
		final int end = unassigned || endV == null ? -1 : Integer.parseInt(endV);
		final int sequenceIndex = p.getSequenceIndex();
	
		return GenotypeLib.getWImageFromResource(new WFileResource("image/png", "") {
			@Override
			public void handleRequest(WebRequest request, WebResponse response) {
				try {
					if (getFileName().isEmpty()) {
						File file = getMain().getOrganismDefinition().getGenome().getSmallGenomePNG(jobDir, sequenceIndex, assignedId, start, end, 0, "", null);
						setFileName(file.getAbsolutePath());
					}
	
					super.handleRequest(request, response);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}				
		});
	}

	public static String jobId(File jobDir) {
		return jobDir.getAbsolutePath().substring(jobDir.getAbsolutePath().lastIndexOf(File.separatorChar)+1);
	}	
	
	public SequenceFilter getFilter() {
		return filter;
	}
}
