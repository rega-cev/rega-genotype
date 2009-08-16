/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.util;

import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileUpload;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WPushButton;

/**
 * A file upload widget.
 * 
 * @author simbre1
 *
 */
public class FileUpload extends WContainerWidget {
	private WFileUpload uploadFile;
	private WPushButton uploadButton;

	public FileUpload() {
		setStyleClass("fileUpload");
        uploadFile = new WFileUpload(this);
        uploadFile.uploaded().addListener(this, new Signal.Listener()  {
            public void trigger() {
                uploadButton.setEnabled(true);
                uploadButton.setText(tr("sequenceInput.uploadFile"));
            }
        });
        
        uploadButton = new WPushButton(tr("sequenceInput.uploadFile"), this);
        uploadButton.clicked().addListener(this, new Signal1.Listener<WMouseEvent>() {
            public void trigger(WMouseEvent a) {
                uploadButton.setEnabled(false);
                uploadButton.setText(tr("sequenceInput.uploadingFile"));
            	uploadFile.upload();
            }
        });
	}
	
	public WFileUpload getUploadFile() {
		return uploadFile;
	}
}
