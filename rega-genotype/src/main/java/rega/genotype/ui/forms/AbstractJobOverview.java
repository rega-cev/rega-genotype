/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import org.jdom.Element;

import rega.genotype.data.GenotypeResultParser;
import rega.genotype.data.table.SequenceFilter;
import rega.genotype.ngs.NgsResultsParser;
import rega.genotype.ngs.NgsResultsTracer;
import rega.genotype.ngs.NgsWidget;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.framework.widgets.DownloadsWidget;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.util.CsvDataTable;
import rega.genotype.util.DataTable;
import rega.genotype.util.XlsDataTable;
import rega.genotype.viruses.recombination.RegionUtils;
import rega.genotype.viruses.recombination.RegionUtils.Region;
import eu.webtoolkit.jwt.Icon;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.StandardButton;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WApplication.UpdateLock;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WMessageBox;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WResource;
import eu.webtoolkit.jwt.WResource.DispositionType;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WTableCell;
import eu.webtoolkit.jwt.WTemplate;
import eu.webtoolkit.jwt.WText;
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

	/**
	 * Synchronize read write results.xml
	 *
	 * Tool analysis writes results.xml that can take long time. Parsing the results is done
	 * in the parserThread that adds the existing results to the view widget waits (5s the
	 * updater is responsible for that) and checks for new results in the results.xml file.
	 */
	private ParserRunnable parserRunnable;
	private GenotypeResultParser parser;
	private NgsResultsParser ngsParser;

	protected File jobDir;
	private WTable jobTable;
	private JobOverviewSummary summary;
	private SequenceFilter filter;
	private boolean scrollingEnabled = false;
	
	protected Template template;
	
	private boolean hasRecombinationResults;
	private NgsWidget ngsWidget;

	public AbstractJobOverview(GenotypeWindow main) {
		super(main);
		
		template = new Template(tr("job-overview-form"), this);

		template.bindString("app.base.url", GenotypeMain.getApp().getEnvironment().getDeploymentPath());
		template.bindString("app.context", GenotypeMain.getApp().getServletContext().getContextPath());
		template.bindEmpty("analysis-cancelled");
		
		jobTable = new WTable();
		template.bindEmpty("ngs-results");
		template.bindWidget("results", jobTable);
		jobTable.setHeaderCount(1, Orientation.Horizontal);
		jobTable.setHeaderCount(1, Orientation.Vertical);
		jobTable.setStyleClass("jobTable");
		jobTable.setObjectName("job-table");

		// on slow servers it can take some time till init is called
		template.bindEmpty("summary");
		template.bindEmpty("job-id");
		template.bindEmpty("downloads");
		template.bindEmpty("recombination-fragment-downloads");
		template.bindEmpty("analysis-in-progress");
		template.bindEmpty("scroll");
	}

	public void init(final String jobId, final String filter) {
		this.jobDir = getJobDir(jobId);

		this.hasRecombinationResults = false;

		this.summary = getSummary(filter);
		template.bindWidget("summary", this.summary);
		template.bindString("job-id", jobId);

		this.filter = new SequenceFilter() {
			public boolean excludeSequence(GenotypeResultParser p) {
				String assignment = GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/conclusion/assigned/name");
				return filter != null && !filter.equals(summary.encodeAssignment(assignment));
			}			
		};
		
		jobTable.clear();

		if (!isNgsJob())
			template.bindWidget("downloads", createDownloadsWidget(filter));
		template.bindWidget("scroll", createScrollButton());

		if (hasRecombinationResults)
			template.bindWidget("recombination-fragment-downloads", createRecombinationFragmentDownloadsWidget(filter));
		else
			template.bindWidget("recombination-fragment-downloads", null);

		template.bindWidget("analysis-in-progress", createInProgressWidget());

		if (parser != null)
			stop();
		parser = createParser();
		startParserThread();

		updateView();
	}

	protected void showDownloads() {
		showWidget("downloads", true);
		showWidget("recombination-fragment-downloads", true);
	}

	private void showWidget(String widgetName, boolean show) {
		WWidget w = template.resolveWidget(widgetName);
		if (w != null)
			w.setHidden(!show);
	}
	
	protected boolean jobDone() {
		File jobDone = new File(jobDir.getAbsolutePath() + File.separatorChar + "DONE");
		return jobDone.exists();
	}
	
	private boolean jobCancelled() {
		return new File(jobDir, ".CANCEL").exists();
	}
	
	protected void updateInfo() {
		showWidget("analysis-in-progress", !jobDone());
		if (jobCancelled())
			template.bindString("analysis-in-progress", 
					"The job was canceled.");
		
		if (jobDone() && jobCancelled())
			template.bindString("analysis-cancelled", tr("monitorForm.analysisCancelled"));
	}

	private WTemplate createInProgressWidget() {
		WTemplate analysisInProgress;
		analysisInProgress = new Template(tr("monitorForm.analysisInProgress"));
		WPushButton cancelButton = new WPushButton(tr("monitorForm.cancelButton"));
		analysisInProgress.bindWidget("cancel-button", cancelButton);
		analysisInProgress.bindInt("update-time-seconds", getMain().getOrganismDefinition().getUpdateInterval()/1000);
		cancelButton.clicked().addListener(analysisInProgress, new Signal1.Listener<WMouseEvent>(){
			public void trigger(WMouseEvent arg) {
				final WMessageBox messageBox = new WMessageBox(
						tr("monitorForm.cancelling"),
						tr("monitorForm.areYouSureToCancel"),
		                Icon.Information, EnumSet.of(StandardButton.Yes, StandardButton.No));
		        messageBox.setModal(false);
		        messageBox.setWidth(new WLength(600));
		        messageBox.buttonClicked().addListener(AbstractJobOverview.this, new Signal1.Listener<StandardButton>() {
					public void trigger(StandardButton sb) {
						if (messageBox.getButtonResult() == StandardButton.Yes) {
							try {
								new File(jobDir, ".CANCEL").createNewFile();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
		            	
		                if (messageBox != null)
		                    messageBox.remove();
					}
				});
		        
		        messageBox.show();
			}
		});
		analysisInProgress.hide();
		return analysisInProgress;
	}

	public void updateView() {
		if (isNgsJob())
			updateNgsView();
		fillResultsWidget();
		updateInfo();
		if (jobDone()) {
			showDownloads();
			template.bindEmpty("scroll");
		}
	}

	protected void updateNgsView() {
		if (ngsParser != null) {
			if (ngsWidget == null) {
				ngsWidget = new NgsWidget(jobDir);
				template.bindWidget("ngs-results", ngsWidget);
			}
			ngsWidget.refresh(ngsParser.getModel(), getMain().getOrganismDefinition());
		}
	}

	protected void fillResultsWidget() {
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
			jobTable.setHidden(true);
		}
	}
	
	public WWidget createRecombinationFragmentDownloadsWidget(final String filter) {
		WTemplate t = new WTemplate(tr("job-overview-recombination-fragment-downloads"));
		
		t.bindWidget("csv-file", createRecombinationFragmentTableDownload(tr("monitorForm.csvTable"), true));
		t.bindWidget("xls-file", createRecombinationFragmentTableDownload(tr("monitorForm.xlsTable"), false));

		t.hide();
		
		return t;
	}
	
	private WWidget createScrollButton() {
		final WPushButton scroll = new WPushButton();
		scroll.setFloatSide(Side.Right);
		scroll.setText("Enable auto scroll");
		scroll.clicked().addListener(scroll, new Signal.Listener() {
			public void trigger() {
				if (scrollingEnabled) {
					scroll.setText("Enable auto scroll");
					scrollingEnabled = false;
				} else {
					scroll.setText("Disable auto scroll");
					scrollingEnabled = true;
				}
			}
		});

		return scroll;
	}
	private WWidget createDownloadsWidget(final String filter) {
		DownloadsWidget downloadsWidget = new DownloadsWidget(this.filter, jobDir, getMain().getOrganismDefinition(), filter == null);
		downloadsWidget.hide();
		return downloadsWidget;
	}
	
	private WAnchor createRecombinationFragmentTableDownload(WString label, final boolean csv) {
		WAnchor tableDownload = new WAnchor("", label);
		tableDownload.setStyleClass("link");

		WResource csvResource = new WResource() {
			@Override
			protected void handleRequest(WebRequest request, WebResponse response) throws IOException {
				response.setContentType("application/excel");
				final DataTable t = csv ? new CsvDataTable(response.getOutputStream(), ',', '"') : new XlsDataTable(response.getOutputStream());
				t.addLabel("sequence-name");
				t.addLabel("recombination-scan-type");
				t.addLabel("fragment-start");
				t.addLabel("fragment-end");
				t.addLabel("fragment-assignment");
				t.addLabel("fragment-support");
				t.newRow();
				
				GenotypeResultParser grp = new GenotypeResultParser() {
					public void endSequence() {
						GenotypeResultParser p = null;
						
						//no result
						String startXPath = "/genotype_result/sequence/result[@id='blast']/start";
						if (getValue(startXPath) == null)
							return;
						final int start = Integer.parseInt(getValue(startXPath));
						
						OrganismDefinition od = AbstractJobOverview.this.getMain().getOrganismDefinition();
						if (od.getRecombinationResultXPaths() != null) {
							for (String path : od.getRecombinationResultXPaths()) {
								String recombinationPath = path + "/recombination";
								if (this.elementExists(recombinationPath)) {
									if (p == null) {
										p = new GenotypeResultParser(this.getSequenceIndex());
										p.parseResultFile(jobDir);
									}
									
									Element recombination = p.getElement(recombinationPath);
									
									String name = GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@name");
									String scanType = ((Element)recombination.getParent()).getAttributeValue("id");
							
									List<Region> regions = RegionUtils.getSupportedRegions(recombination, start);
									for (Region r : regions) {
										try {
											t.addLabel(name);
											t.addLabel(scanType);
											t.addLabel(r.start + "");
											t.addLabel(r.end + "");
											t.addLabel(r.assignment);
											t.addLabel(r.support + "");
											t.newRow();
										} catch (IOException ioe) {
											ioe.printStackTrace();
										}
									}
								}
							}
						}

					}
					public boolean skipSequence() {
				    	return filter.excludeSequence(this);
				    }
				};
				grp.parseResultFile(jobDir);

				try {
					t.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		csvResource.suggestFileName("results." + (csv ? "csv" : "xls"));
		csvResource.setDispositionType(DispositionType.Attachment);
		tableDownload.setLink(new WLink(csvResource));

		return tableDownload;
	}

	protected boolean isNgsJob() {
		return NgsResultsTracer.ngsRsultsFile(jobDir).exists();
	}

	public void stop() {
		parserRunnable.stop();
		parser.stopParsing();
		parser = null;
		parserRunnable = null;
	}

	private void startParserThread() {
		final int interval = getMain().getOrganismDefinition().getUpdateInterval();
		parserRunnable = new ParserRunnable(interval, WApplication.getInstance());
		Thread parserThread = new Thread(parserRunnable);
		parserThread.setName("parserThread");
		parserThread.start();
	}

	protected void startNgsParser() {
		
	}

	private class ParserRunnable implements Runnable {
		final Object lock = new Object();
		volatile boolean stop = false;
		private int interval;
		private WApplication app;
		ParserRunnable(int interval, WApplication app) {
			this.interval = interval;
			this.app = app;
		}
		public void run() {
			// prepare NGS data
			ngsParser = new NgsResultsParser();
			ngsParser.updateUiSignal().addListener(AbstractJobOverview.this,
					new Signal.Listener() {
				public void trigger() {
					UpdateLock updateLock = app.getUpdateLock();
					updateView();
					app.triggerUpdate();
					updateLock.release();
				}
			});
			final File ngsResultFile = new File(getJobdir(), NgsResultsTracer.NGS_RESULTS_FILL);
			if (ngsResultFile.exists())
				ngsParser.parseFile(ngsResultFile);
			
			final File resultFile = new File(getJobdir(), "result.xml");
			// wait till result.xml is ready
			while (!stop && !resultFile.exists()){
				synchronized (lock) {
					try {
						lock.wait(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
						assert (false);
					}
				}
			}

			if (!stop)
				parser.parseResultFile(getJobdir());
		}

		void stop(){
			stop = true;
		}
	}
	/**
	 * re implement if you need to use a different parser.
	 * 
	 * @return
	 */
	protected GenotypeResultParser createParser() {
		return new Parser();
	}

	private class Parser extends GenotypeResultParser {		
		private WApplication app;

		Parser() {
			this.app = WApplication.getInstance();
			setReaderBlocksOnEof(true);
		}
		@Override
		public void endSequence() {
			if (skipSequence())
				return;

			UpdateLock updateLock = app.getUpdateLock(); // parsing is called from parserThread.

			int numRows = AbstractJobOverview.this.jobTable.getRowCount()-1;
			if (getSequenceIndex() - getFilteredSequences() >= numRows) {
				jobTable.setHidden(false);

				if (numRows < 1000) {
					List<WWidget> data = getData(this);
	
					int row = jobTable.getRowCount();
					for (int i = 0; i < data.size(); i++) {
						OrganismDefinition od = AbstractJobOverview.this.getMain().getOrganismDefinition();
	
						if (od.getRecombinationResultXPaths() != null) {
							for (String path : od.getRecombinationResultXPaths()) {
								if (elementExists(path + "/recombination")) {
									hasRecombinationResults = true;
									break;
								}
							}
						}
						
						WTableCell cell = jobTable.getElementAt(row, i);
						cell.setId("");
						WWidget widget = data.get(i);
						if (widget != null) {
							cell.addWidget(widget);
							if (widget.getObjectName().length() == 0)
								widget.setId("");
						}
	
						if (WApplication.getInstance().getEnvironment().getUserAgent().indexOf("MSIE") != -1)
							cell.setStyleClass(jobTable.getColumnAt(i).getStyleClass());
					}

					jobTable.getRowAt(jobTable.getRowCount() - 1).setId("");

					if (scrollingEnabled)
						doJavaScript("window.scrollTo(0,document.body.scrollHeight - 500);");

				} else if (jobTable.getRowCount() == 1000) {
					WTableCell cell = jobTable.getElementAt(1000, 0);
					cell.setColumnSpan(jobTable.getColumnCount());
					cell.addWidget(new WText("Showing only first 1000 results... See downloads for complete results."));
				}

				if (summary != null) 
					summary.update(this, getMain().getOrganismDefinition());

			}

			app.triggerUpdate();
			updateLock.release();
		}

		@Override
		public void updateUi() {
			UpdateLock updateLock = app.getUpdateLock();
			updateView();
			app.triggerUpdate();
			updateLock.release();
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
		WAnchor anchor = new WAnchor(new WLink(WLink.Type.InternalPath, "/job/" + jobId(jobDir) + "/" + JobForm.SEQUENCE_PREFIX + p.getSequenceIndex()), tr("monitorForm.report"));
		anchor.setObjectName("report");
		return anchor;
	}
	
	public boolean existsJob(String jobId) {
		return getJobDir(jobId).exists();
	}

	public File getJobDir(String jobId) {
		return new File(getMain().getOrganismDefinition().getJobDir() + File.separatorChar + jobId);
	}

	public static String reportPath(File jobDir, int sequenceIndex) {
		return jobPath(jobDir) + '/' + JobForm.SEQUENCE_PREFIX + String.valueOf(sequenceIndex);
	}

	public static String jobPath(File jobDir) {
		return '/' + JobForm.JOB_URL + '/' + jobId(jobDir);
	}
	
	public String getJobPath() {
		return jobPath(jobDir);
	}
	
	protected WImage createGenomeImage(final GenotypeResultParser p, final String assignedId, final String myTypeGenome, boolean unassigned) {
		String startV = p.getValue("/genotype_result/sequence/result[@id='blast']/start");
		final int start = unassigned || startV == null ? -1 : Integer.parseInt(startV);
		String endV = p.getValue("/genotype_result/sequence/result[@id='blast']/end");
		final int end = unassigned || endV == null ? -1 : Integer.parseInt(endV);
		final int sequenceIndex = p.getSequenceIndex();
	
		if (start < 1 && end < 1)
			return null; // Do not show genome image if the sequence could not be aligned.

		return GenotypeLib.getWImageFromResource(new WFileResource("image/png", "") {
			@Override
			public void handleRequest(WebRequest request, WebResponse response) {
				String typeVirusImage = "0";
				File f = myTypeGenome == null ? null :
						new File(getMain().getOrganismDefinition().getXmlPath()+"/genome_"+myTypeGenome.replaceAll("\\d", "")+".png");
				try {
					if (myTypeGenome != null && f.exists()){
						typeVirusImage = myTypeGenome.replaceAll("\\d", "");
					}
					if (getFileName().isEmpty()) {
						File file = getMain().getOrganismDefinition().getGenome().getSmallGenomePNG(jobDir, sequenceIndex, assignedId, start, end, typeVirusImage, "", null);
						setFileName(file.getAbsolutePath());
					}
	
					super.handleRequest(request, response);
				} catch (IOException e) {
					e.printStackTrace(); // some times the images dont exist.
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

	public File getJobdir() {
		return jobDir;
	}

	protected void bindResults(WWidget resultsWidget) {
		template.bindWidget("results", resultsWidget);
	}
}
