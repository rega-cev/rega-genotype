package rega.genotype.ui.forms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;

import rega.genotype.ui.data.AbstractCsvGenerator;
import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WBreak;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WResource;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTimer;
import eu.webtoolkit.jwt.WWidget;

public abstract class AbstractJobOverview extends IForm {
	protected File jobDir;

	private WText analysisInProgress;
	private WTable jobTable;
	
	private WTimer updater;
	
	private WContainerWidget downloadContainer;
	
	private boolean fillingTable = false;
	

	public AbstractJobOverview(GenotypeWindow main) {
		super(main, "monitor-form");
	
		WString aipm = tr("monitorForm.analysisInProgress");
		aipm.arg(getMain().getOrganismDefinition().getUpdateInterval()/1000);
		analysisInProgress = new WText(aipm, this);
		analysisInProgress.setStyleClass("analysisProgress");
		
		new WBreak(this);
		
		jobTable = new WTable(this);
		jobTable.setStyleClass("jobTable");
		
		downloadContainer = new WContainerWidget(this);
		downloadContainer.setStyleClass("downloadContainer");
		
		if(updater!=null) {
			updater.start();
		}
		
		GenotypeMain.getApp().internalPathChanged.addListener(this, new Signal1.Listener<String>() {

			public void trigger(String basePath) {
				if (basePath.equals(GenotypeWindow.jobPath(jobDir) + '/')) {
					try {
						String id = GenotypeMain.getApp().internalPathNextPart(basePath);
						if (!id.equals("")) {
							int sequenceIndex = Integer.valueOf(id);
							getMain().detailsForm(jobDir, sequenceIndex);
						}
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			} });

	}
	
	public void init(File jobDir) {
		boolean otherJob = !jobDir.equals(this.jobDir);
		
		this.jobDir = jobDir;

		// FIXME should be done when hiding the widget?
		if (updater != null) {
			updater.stop();
		}

		if (otherJob)
			jobTable.clear();

		downloadContainer.clear();

		analysisInProgress.setHidden(true);
		
		File jobDone = new File(jobDir.getAbsolutePath() + File.separatorChar + "DONE");
		if (!jobDone.exists()) {
			analysisInProgress.setHidden(false);

			updater = new WTimer();
			updater.setInterval(getMain().getOrganismDefinition().getUpdateInterval());
			updater.timeout.addListener(this, new Signal.Listener() {
				public void trigger() {
					if(!fillingTable)
						fillTable();
					System.err.println("lala---------------------");
				}
			});
			updater.start();
		}
		
		fillTable();
	}
	
	public void fillTable() {
		fillingTable = true;
		if(jobTable.numRows()==0) {
			List<WString> headers = getHeaders();
			for(int i = 0; i<headers.size(); i++) {
				jobTable.elementAt(0, i).addWidget(new WText(headers.get(i)));
				jobTable.elementAt(0, i).setStyleClass("jobTableHeader");
			}
		}
		
		p.parseFile(jobDir);
		
		File jobDone = new File(jobDir.getAbsolutePath() + File.separatorChar + "DONE");
		if(jobDone.exists()) {
			if(updater!=null)
				updater.stop();
			analysisInProgress.setHidden(true);

			WText downloadResult = new WText(tr("monitorForm.downloadResults"), downloadContainer);
			WAnchor xmlFileDownload = new WAnchor("", tr("monitorForm.xmlFile"), downloadContainer);
			// Wt2:
			//xmlFileDownload.etTarget(AnchorTarget.TargetNewWindow);
			xmlFileDownload.setAttributeValue("target", "_new");
			xmlFileDownload.setStyleClass("link");
			xmlFileDownload.setTarget(AnchorTarget.TargetNewWindow);
			WResource xmlResource = new WFileResource("application/xml", jobDir.getAbsolutePath() + File.separatorChar + "result.xml");
			xmlResource.suggestFileName("result.xml");
			xmlFileDownload.setRef(xmlResource.generateUrl());
			
			new WText(lt(" , "), downloadContainer);
			
			WAnchor csvTableDownload = new WAnchor("", tr("monitorForm.csvTable"), downloadContainer);
			csvTableDownload.setAttributeValue("target", "_new");
			csvTableDownload.setStyleClass("link");
			csvTableDownload.setTarget(AnchorTarget.TargetNewWindow);
			WResource csvResource = new WResource() {
				@Override
				public String resourceMimeType() {
					return "application/excell";
				}
				// TODO Auto-generated catch block

				@Override
				protected boolean streamResourceData(OutputStream stream, HashMap<String, String> arguments) throws IOException {
					Writer w = new OutputStreamWriter(stream, "UTF-8");
					AbstractCsvGenerator acsvgen = AbstractJobOverview.this.getMain().getOrganismDefinition().getCsvGenerator(w);
					acsvgen.parseFile(new File(jobDir.getAbsolutePath()));
					w.flush();
					return true;
				}
				
			};
			csvResource.suggestFileName("results.csv");
			csvTableDownload.setRef(csvResource.generateUrl());
			
			new WBreak(downloadContainer);
			new WText(tr("monitorForm.downloadJob"),downloadContainer);

			final File jobArchive = GenotypeLib.getZipArchiveFileName(jobDir);
			WAnchor jobFileDownload = new WAnchor("", tr("monitorForm.jobFile"), downloadContainer);
			jobFileDownload.setAttributeValue("target", "_new");
			jobFileDownload.setStyleClass("link");
			jobFileDownload.setTarget(AnchorTarget.TargetNewWindow);
			WResource jobResource = new WFileResource("application/zip", jobArchive.getAbsolutePath()) {
				@Override
				protected boolean streamResourceData(OutputStream stream, HashMap<String, String> arguments) {
					GenotypeLib.zip(jobDir, jobArchive);
					super.streamResourceData(stream, arguments);
					return true;
				}
					
			};
			jobResource.suggestFileName(jobArchive.getName());
			jobFileDownload.setRef(jobResource.generateUrl());
		}
		
		fillingTable = false;
	}

	private SaxParser p = new SaxParser(){
		@Override
		public void endSequence() {
			int numRows = jobTable.numRows()-1;
			if(getSequenceIndex()>=numRows) {
				List<WWidget> data = getData(p);
				for(int i = 0; i<data.size(); i++) {
					jobTable.elementAt(getSequenceIndex()+1, i).addWidget(data.get(i));
				}
			}
		}
	};
	
	public abstract List<WString> getHeaders();
	
	public abstract List<WWidget> getData(SaxParser p);

	protected WAnchor createReportLink(final SaxParser p) {
		WAnchor report = new WAnchor("", lt("Report"));
		report.setStyleClass("link");
		report.setRefInternalPath(GenotypeWindow.reportPath(jobDir, p.getSequenceIndex()));
		return report;
	}
}
