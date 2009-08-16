package rega.genotype.ui.forms;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import net.sf.witty.wt.SignalListener;
import net.sf.witty.wt.WAnchor;
import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WEmptyEvent;
import net.sf.witty.wt.WFileResource;
import net.sf.witty.wt.WImage;
import net.sf.witty.wt.WResource;
import net.sf.witty.wt.WTable;
import net.sf.witty.wt.WText;
import net.sf.witty.wt.WTimer;
import net.sf.witty.wt.WWidget;
import net.sf.witty.wt.i8n.WMessage;

import org.apache.commons.io.IOUtils;

import rega.genotype.ui.data.AbstractCsvGenerator;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.i18n.resources.GenotypeResourceManager;

public abstract class AbstractJobOverview extends WContainerWidget {
	protected File jobDir;
	
	protected OrganismDefinition od;
	
	private WText title;
	private WText analysisInProgress;
	private WTable jobTable;
	
	private WTimer updater;
	
	private boolean fillingTable = false;

	public AbstractJobOverview(File jobDir, GenotypeResourceManager rm, OrganismDefinition od) {
		this.jobDir = jobDir;
		
		this.od = od;
		
		this.title = new WText(rm.getOrganismValue("monitor-form", "title"), this);
		new WBreak(this);
		
		File jobDone = new File(jobDir.getAbsolutePath() + File.separatorChar + "DONE");
		if(!jobDone.exists()) {
			analysisInProgress = new WText(tr("monitorForm.analysisInProgress"), this);
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
		
		jobTable = new WTable(this);
		
		if(updater!=null) {
			updater.start();
		}
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

			//download section
			WContainerWidget downloadContainer = new WContainerWidget(this);
			WText downloadResult = new WText(tr("monitorForm.downloadResults"), downloadContainer);
			WAnchor xmlFileDownload = new WAnchor((String)null, tr("monitorForm.xmlFile"), downloadContainer);
			xmlFileDownload.setStyleClass("link");
			xmlFileDownload.setRef(new WFileResource("application/xml", jobDir.getAbsolutePath() + File.separatorChar + "result.xml").generateUrl());
			
			new WText(lt(" , "), this);
			
			WAnchor csvTableDownload = new WAnchor((String)null, tr("monitorForm.csvTable"), downloadContainer);
			csvTableDownload.setStyleClass("link");
			csvTableDownload.setRef(new WResource() {
				@Override
				public String resourceMimeType() {
					return "application/excell";
				}

				@Override
				protected void streamResourceData(OutputStream stream) {
					AbstractCsvGenerator acsvgen = od.getCsvGenerator(new PrintStream(stream));
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

	protected WImage getWImageFromFile(final File f) {
		WImage chartImage = new WImage(new WResource() {

            @Override
            public String resourceMimeType() {
                return "image/png";
            }

            @Override
            protected void streamResourceData(OutputStream stream) {
                try {
                	FileInputStream fis = new FileInputStream(f);
                    IOUtils.copy(fis, stream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
        }, (WContainerWidget)null);
		
		return chartImage;
	}
	
	public abstract List<WMessage> getHeaders();
	
	public abstract List<WWidget> getData(SaxParser p);
}
