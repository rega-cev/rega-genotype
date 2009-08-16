package rega.genotype.ui.forms;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import net.sf.witty.wt.SignalListener;
import net.sf.witty.wt.WAnchor;
import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WEmptyEvent;
import net.sf.witty.wt.WFileResource;
import net.sf.witty.wt.WResource;
import net.sf.witty.wt.WTable;
import net.sf.witty.wt.WText;
import net.sf.witty.wt.WTimer;
import net.sf.witty.wt.WWidget;
import net.sf.witty.wt.i8n.WMessage;
import rega.genotype.ui.data.AbstractCsvGenerator;
import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.framework.GenotypeWindow;

public abstract class AbstractJobOverview extends IForm {
	protected File jobDir;

	private WText analysisInProgress;
	private WTable jobTable;
	
	private WTimer updater;
	
	private WContainerWidget downloadContainer;
	
	private boolean fillingTable = false;
	

	public AbstractJobOverview(GenotypeWindow main) {
		super(main, "monitor-form");

		analysisInProgress = new WText(tr("monitorForm.analysisInProgress"), this);
		
		new WBreak(this);
		
		jobTable = new WTable(this);
		jobTable.setStyleClass("jobTable");
		
		downloadContainer = new WContainerWidget(this);
		
		if(updater!=null) {
			updater.start();
		}
	}
	
	public void init(File jobDir) {
		this.jobDir = jobDir;
		
		jobTable.clear();
		downloadContainer.clear();

		if(updater!=null) {
			updater.stop();
		}
		
		analysisInProgress.setHidden(true);
		
		File jobDone = new File(jobDir.getAbsolutePath() + File.separatorChar + "DONE");
		if(!jobDone.exists()) {
			analysisInProgress.setHidden(false);
			
			updater = new WTimer();
			updater.setInterval(5*1000);
			updater.timeout.addListener(new SignalListener<WEmptyEvent>() {
				public void notify(WEmptyEvent a) {
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
			List<WMessage> headers = getHeaders();
			for(int i = 0; i<headers.size(); i++) {
				jobTable.putElementAt(0, i, new WText(headers.get(i)));
			}
		}
		
		p.parseFile(jobDir);
		
		File jobDone = new File(jobDir.getAbsolutePath() + File.separatorChar + "DONE");
		if(jobDone.exists()) {
			updater.stop();
			analysisInProgress.setHidden(true);

			//TODO
			//header('Content-type: application/ms-excell');
			//requires WAnchor fix
			
			//download section
			WText downloadResult = new WText(tr("monitorForm.downloadResults"), downloadContainer);
			WAnchor xmlFileDownload = new WAnchor((String)null, tr("monitorForm.xmlFile"), downloadContainer);
			xmlFileDownload.setStyleClass("link");
			xmlFileDownload.setRef(new WFileResource("application/xml", jobDir.getAbsolutePath() + File.separatorChar + "result.xml").generateUrl());
			
			new WText(lt(" , "), downloadContainer);
			
			WAnchor csvTableDownload = new WAnchor((String)null, tr("monitorForm.csvTable"), downloadContainer);
			csvTableDownload.setStyleClass("link");
			csvTableDownload.setRef(new WResource() {
				@Override
				public String resourceMimeType() {
					return "application/excell";
				}

				@Override
				protected void streamResourceData(OutputStream stream) {
					AbstractCsvGenerator acsvgen = AbstractJobOverview.this.getMain().getOrganismDefinition().getCsvGenerator(new PrintStream(stream));
					acsvgen.parseFile(new File(jobDir.getAbsolutePath()));
				}
				
			}.generateUrl());
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
					jobTable.putElementAt(getSequenceIndex()+1, i, data.get(i));
				}
			}
		}
	};
	
	public abstract List<WMessage> getHeaders();
	
	public abstract List<WWidget> getData(SaxParser p);
}
