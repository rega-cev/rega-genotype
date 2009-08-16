package rega.genotype.ui.util;

import net.sf.witty.wt.SignalListener;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WEmptyEvent;
import net.sf.witty.wt.WFileUpload;
import net.sf.witty.wt.WMouseEvent;
import net.sf.witty.wt.WPushButton;

public class FileUpload extends WContainerWidget {
	private WFileUpload uploadFile;
	private WPushButton uploadButton;

	public FileUpload() {
        uploadFile = new WFileUpload(this);
        uploadFile.uploaded.addListener(new SignalListener<WEmptyEvent>()  {
            public void notify(WEmptyEvent a) {
                uploadButton.setEnabled(true);
                uploadButton.setText(tr("sequenceInput.uploadFile"));
            }
        });
        
        uploadButton = new WPushButton(tr("sequenceInput.uploadFile"), this);
        uploadButton.clicked.addListener(new SignalListener<WMouseEvent>() {
            public void notify(WMouseEvent a) {
                uploadButton.setText(tr("sequenceInput.uploadingFile"));
            	uploadFile.upload();
            }
        });
	}
	
	public WFileUpload getUploadFile() {
		return uploadFile;
	}
}
