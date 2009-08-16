/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import java.io.File;
import java.io.IOException;
import java.util.List;

import rega.genotype.ui.data.AbstractDataTableGenerator;
import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.CsvDataTable;
import rega.genotype.ui.util.DataTable;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.ui.util.XlsDataTable;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
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
	private boolean fillingTable = false;

	private WText explainText;	

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

		if (updater!=null) {
			updater.start();
		}
		
		GenotypeMain.getApp().internalPathChanged().addListener(this, new Signal1.Listener<String>() {

			public void trigger(String basePath) {
				if (basePath.equals(GenotypeWindow.jobPath(jobDir) + '/')) {
					try {
						String id = GenotypeMain.getApp().getInternalPathNextPart(basePath);
						if (!id.equals("")) {
							int sequenceIndex = Integer.valueOf(id);
							getMain().detailsForm(jobDir, sequenceIndex);
						} else {
							getMain().setForm(AbstractJobOverview.this);
						}
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			} });
	}

	protected boolean downloadResultsLink() {
		return true;
	}

	@Override
	public void setHidden(boolean hidden) {
		if (updater != null)
			if (hidden) 
				updater.stop();
			else
				updater.start();

		if (!hidden)
			fillTable();

		super.setHidden(hidden);
	}

	public void init(File jobDir, String jobId) {
		boolean otherJob = !jobDir.equals(this.jobDir);
		
		this.jobDir = jobDir;

		WString msg = tr("monitorForm.explain");
		msg.arg(jobId);
		explainText.setText(msg);

		if (updater != null) {
			updater.stop();
			updater = null;
		}

		if (otherJob)
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
					if(!fillingTable)
						fillTable();
				}
			});

			// the update timer will be started from within setHidden(false), together with
			// an initial fill
		}
	}
	
	public void fillTable() {
		fillingTable = true;
		if(jobTable.getRowCount()==0) {
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
			WAnchor xmlFileDownload = new WAnchor("", tr("monitorForm.xmlFile"), div);
			xmlFileDownload.setId("");
			xmlFileDownload.setTarget(AnchorTarget.TargetNewWindow);
			xmlFileDownload.setStyleClass("link");
			WResource xmlResource = new WFileResource("application/xml", jobDir.getAbsolutePath() + File.separatorChar + "result.xml");
			xmlResource.suggestFileName("result.xml");
			xmlFileDownload.setRef(xmlResource.generateUrl());
			
			l = new WText(", ", div);
			l.setId("");
			
			div.addWidget(createTableDownload(tr("monitorForm.csvTable"), true));

			l = new WText(", ", div);
			l.setId("");

			div.addWidget(createTableDownload(tr("monitorForm.xlsTable"), false));

			if (downloadResultsContainer != null) {
				div = new WContainerWidget(downloadResultsContainer);
				l = new WText(tr("monitorForm.downloadJob"), div);
				l.setId("");

				div.setObjectName("donwload-results-contents");

				final File jobArchive = GenotypeLib.getZipArchiveFileName(jobDir);
				WAnchor jobFileDownload = new WAnchor("", tr("monitorForm.jobFile"), div);
				jobFileDownload.setId("");
				jobFileDownload.setStyleClass("link");
				jobFileDownload.setTarget(AnchorTarget.TargetNewWindow);
				WResource jobResource = new WFileResource("application/zip", jobArchive.getAbsolutePath()) {
					@Override
					public void handleRequest(WebRequest request, WebResponse response) {
						GenotypeLib.zip(jobDir, jobArchive);
						super.handleRequest(request, response);
					}
						
				};
				jobResource.suggestFileName(jobArchive.getName());
				jobFileDownload.setRef(jobResource.generateUrl());
			}
		}
		
		fillingTable = false;
	}

	private WAnchor createTableDownload(WString label, final boolean csv) {
		WAnchor csvTableDownload = new WAnchor("", label);
		csvTableDownload.setId("");
		csvTableDownload.setStyleClass("link");
		csvTableDownload.setTarget(AnchorTarget.TargetNewWindow);

		WResource csvResource = new WResource() {
			@Override
			protected void handleRequest(WebRequest request, WebResponse response) throws IOException {
				response.setContentType("application/excell");
				DataTable t = csv ? new CsvDataTable(response.getOutputStream(), ';', '"') : new XlsDataTable(response.getOutputStream());
				AbstractDataTableGenerator acsvgen = AbstractJobOverview.this.getMain().getOrganismDefinition().getDataTableGenerator(t);
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
			if(getSequenceIndex()>=numRows) {
				List<WWidget> data = getData(tableFiller);
				for (int i = 0; i < data.size(); i++) {
					WTableCell cell = jobTable.getElementAt(getSequenceIndex()+1, i);
					cell.setId("");
					cell.addWidget(data.get(i));
					if (data.get(i).getObjectName().length() > 0)
						data.get(i).setId("");
					
					if (WApplication.getInstance().getEnvironment().agentIsIE())
						cell.setStyleClass(jobTable.getColumnAt(i).getStyleClass());
				}
				jobTable.getRowAt(jobTable.getRowCount() - 1).setId("");
			}
		}
	};
	
	public abstract List<Header> getHeaders();
	
	public abstract List<WWidget> getData(GenotypeResultParser p);

	protected WAnchor createReportLink(final GenotypeResultParser p) {
		WAnchor report = new WAnchor("", "Report");
		report.setObjectName("report-" + p.getSequenceIndex());
		report.setStyleClass("link");
		report.setRefInternalPath(GenotypeWindow.reportPath(jobDir, p.getSequenceIndex()));
		return report;
	}
}
