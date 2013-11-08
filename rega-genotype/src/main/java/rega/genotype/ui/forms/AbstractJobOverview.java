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
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.CsvDataTable;
import rega.genotype.ui.util.DataTable;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.ui.util.XlsDataTable;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WResource;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WTableCell;
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
	private WText analysisInProgress;
	private WTable jobTable;
	private WTimer updater;
	private WContainerWidget downloadTableContainer, downloadResultsContainer;

	private WText explainText;
	
	private JobOverviewSummary summary;
	private String filter;

	public AbstractJobOverview(GenotypeWindow main) {
		super(main, "monitor-form");

		explainText = new WText(this);
		explainText.setObjectName("job-overview-explanation");

		WString msg = tr("monitorForm.analysisInProgress");
		msg.arg(getMain().getOrganismDefinition().getUpdateInterval()/1000);
		analysisInProgress = new WText(msg, this);
		analysisInProgress.setStyleClass("analysisProgress");
		analysisInProgress.setObjectName("analysis-in-progress");
		
		jobTable = new WTable(this);
		jobTable.setHeaderCount(1, Orientation.Horizontal);
		jobTable.setHeaderCount(1, Orientation.Vertical);
		jobTable.setStyleClass("jobTable");
		jobTable.setObjectName("job-table");

		downloadTableContainer = new WContainerWidget(this);
		downloadTableContainer.setStyleClass("downloadContainer");
		downloadTableContainer.setObjectName("download-table");

		if (downloadResultsLink()) {
			downloadResultsContainer = new WContainerWidget(this);
			downloadResultsContainer.setStyleClass("downloadContainer");
			downloadResultsContainer.setObjectName("download-results");
		} else
			downloadResultsContainer = null;
	}

	protected boolean downloadResultsLink() {
		return true;
	}
	
	public void init(String jobId, String filter) {
		this.filter = filter;

		if (filter != null)
			setTitle(getMain().getResourceManager().getOrganismValue("monitor-form", "title-filtered").arg(filter));
		else {
			setTitle(getMain().getResourceManager().getOrganismValue("monitor-form", "title"));
			WString msg = tr("monitorForm.explain");
			msg.arg(jobId);
			explainText.setText(msg);
		}

		File jobDir = getJobDir(jobId);
		
		this.jobDir = jobDir;

		if (summary != null)
			this.removeWidget(summary);
		
		summary = getSummary(filter);

		if (summary != null) {
			summary.reset();
			int index = getIndexOf(analysisInProgress);
			
			if (summary.getLocation() == Side.Top)
				this.insertWidget(++index, summary);
			else
				this.addWidget(summary);
		}

		if (updater != null) {
			updater.stop();
			updater = null;
		}

		jobTable.clear();

		downloadTableContainer.clear();
		if (downloadResultsContainer != null)
			downloadResultsContainer.clear();

		analysisInProgress.setHidden(true);
		
		File jobDone = new File(jobDir.getAbsolutePath() + File.separatorChar + "DONE");
		if (!jobDone.exists()) {
			analysisInProgress.setHidden(false);

			updater = new WTimer();
			updater.setInterval(getMain().getOrganismDefinition().getUpdateInterval());
			updater.timeout().addListener(this, new Signal.Listener() {
				public void trigger() {
					fillTable();
				}
			});
		}

		fillTable();		

		if (updater != null)
			updater.start();
	}

	public void fillTable() {
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
		
		tableFiller.parseFile(jobDir);
		
		File jobDone = new File(jobDir.getAbsolutePath() + File.separatorChar + "DONE");
		if (jobDone.exists()) {
			downloadTableContainer.clear();
			
			if (downloadResultsContainer != null)
				downloadResultsContainer.clear();

			if (updater!=null) {
				updater.stop();
				updater = null;
			}
			analysisInProgress.setHidden(true);

			WContainerWidget div = new WContainerWidget(downloadTableContainer);
			div.setObjectName("donwload-table-contents");
			
			WText l = new WText(tr("monitorForm.downloadResults"), div);
			l.setId("");
			
			if (filter == null) {
				WAnchor xmlFileDownload = new WAnchor("", tr("monitorForm.xmlFile"), div);
				xmlFileDownload.setObjectName("xml-download");
				xmlFileDownload.setTarget(AnchorTarget.TargetNewWindow);
				xmlFileDownload.setStyleClass("link");
				WResource xmlResource = new WFileResource("application/xml", jobDir.getAbsolutePath() + File.separatorChar + "result.xml");
				xmlResource.suggestFileName("result.xml");
				xmlFileDownload.setRef(xmlResource.generateUrl());
				l = new WText(", ", div);
				l.setId("");
			}
			
			div.addWidget(createTableDownload(tr("monitorForm.csvTable"), true));

			l = new WText(", ", div);
			l.setId("");

			div.addWidget(createTableDownload(tr("monitorForm.xlsTable"), false));
			
			l = new WText(", ", div);
			l.setId("");
			
			div.addWidget(createFastaDownload());

			if (downloadResultsContainer != null) {
				div = new WContainerWidget(downloadResultsContainer);
				l = new WText(tr("monitorForm.downloadJob"), div);
				l.setId("");

				div.setObjectName("donwload-results-contents");

				WAnchor jobFileDownload = new WAnchor("", tr("monitorForm.jobFile"), div);
				jobFileDownload.setObjectName("zip-download");
				jobFileDownload.setStyleClass("link");
				jobFileDownload.setTarget(AnchorTarget.TargetNewWindow);
				WResource jobResource = new WResource() {
					@Override
					protected void handleRequest(WebRequest request, WebResponse response) throws IOException {
						response.setContentType("application/zip");
						GenotypeLib.zip(jobDir, response.getOutputStream());
					}
				};
				jobResource.suggestFileName(jobDir.getName() + ".zip");
				jobFileDownload.setRef(jobResource.generateUrl());
				
				downloadResultsContainer.setHidden(filter!=null);
			}
			
		}
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
				
				FastaGenerator generateFasta = new FastaGenerator(AbstractJobOverview.this, response.getOutputStream());
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
				response.setContentType("application/excell");
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

	private GenotypeResultParser tableFiller = new GenotypeResultParser(){
		@Override
		public void endSequence() {
			int numRows = jobTable.getRowCount()-1;
			if(getSequenceIndex() - getFilteredSequences() >= numRows) {
				List<WWidget> data = getData(tableFiller);
				
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
					summary.update(tableFiller, getMain().getOrganismDefinition());
			}
		}

		@Override
		public boolean skipSequence() {
			return isExcludedByFilter(this);
		}
	};
	
	public abstract List<Header> getHeaders();
	
	public abstract List<WWidget> getData(GenotypeResultParser p);

	public abstract JobOverviewSummary getSummary(String filter);

	protected WAnchor createReportLink(final GenotypeResultParser p) {
		WAnchor report = new WAnchor("", "Report");
		report.setObjectName("report-" + p.getSequenceIndex());
		report.setStyleClass("link");
		report.setRefInternalPath(reportPath(jobDir, p.getSequenceIndex()));
		return report;
	}
	
	public boolean isExcludedByFilter(GenotypeResultParser p) {
		String assignment = GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/conclusion/assigned/name");
		return filter != null && !filter.equals(summary.encodeAssignment(assignment));
	}
	
	public boolean existsJob(String jobId) {
		return getJobDir(jobId).exists();
	}

	public File getJobDir(String jobId) {
		return new File(Settings.getInstance().getJobDir(getMain().getOrganismDefinition()).getAbsolutePath()
				+ File.separatorChar + jobId);
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
}
