package rega.genotype.ui.forms;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WImage;
import net.sf.witty.wt.WResource;
import net.sf.witty.wt.WTable;
import net.sf.witty.wt.WText;
import net.sf.witty.wt.WWidget;

import org.apache.commons.io.IOUtils;

import rega.genotype.ui.data.SaxParser;

public abstract class AbstractJobOverview extends WContainerWidget {
	private WTable jobTable = new WTable(this);
	
	public AbstractJobOverview() {
		List<WText> headers = getHeaders();
		for(int i = 0; i<headers.size(); i++) {
			jobTable.putElementAt(0, i, headers.get(i));
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
	
	public abstract List<WText> getHeaders();
	
	public abstract List<WWidget> getData(SaxParser p);
}
