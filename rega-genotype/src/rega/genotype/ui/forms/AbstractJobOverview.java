package rega.genotype.ui.forms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import net.sf.witty.wt.SignalListener;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WEmptyEvent;
import net.sf.witty.wt.WImage;
import net.sf.witty.wt.WResource;
import net.sf.witty.wt.WTable;
import net.sf.witty.wt.WText;
import net.sf.witty.wt.WTimer;
import net.sf.witty.wt.WWidget;
import net.sf.witty.wt.i8n.WMessage;

import org.apache.commons.io.IOUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.i18n.resources.GenotypeResourceManager;

public abstract class AbstractJobOverview extends WContainerWidget {
	protected File jobDir;
	
	private WTable jobTable;
	
	private WTimer updater;
	
	public AbstractJobOverview(File jobDir, GenotypeResourceManager rm) {
		this.jobDir = jobDir;
		
		WText title = new WText(rm.getOrganismValue("monitor-form", "title"), this);
		
		File jobDone = new File(jobDir.getAbsolutePath() + File.separatorChar + "DONE");
		if(!jobDone.exists()) {
			WText analysisInProgress = new WText(tr("monitorForm.analysisInProgress"), this);
			updater = new WTimer();
			updater.setInterval(5*1000);
			updater.timeout.addListener(new SignalListener<WEmptyEvent>() {
				public void notify(WEmptyEvent a) {
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
		if(jobTable.numRows()==0) {
			List<WMessage> headers = getHeaders();
			for(int i = 0; i<headers.size(); i++) {
				jobTable.putElementAt(0, i, new WText(headers.get(i)));
			}
		}
		
		try {
			File resultFile = new File(jobDir.getAbsolutePath()+File.separatorChar+"result.xml");
			if(resultFile.exists()) {
				p.parse(new InputSource(new FileReader(resultFile)));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
