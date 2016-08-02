/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.util;

import java.io.File;

import rega.genotype.ui.framework.widgets.Dialogs;

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
	private Signal1<File> uploadedFile = new Signal1<File>();

	public FileUpload() {
		setStyleClass("fileUpload");
        uploadFile = new WFileUpload(this);
        
        uploadFile.changed().addListener(this, new Signal.Listener() {
			public void trigger() {
				uploadFile.upload();
			}
		});
        
        uploadFile.uploaded().addListener(this, new Signal.Listener() {
			public void trigger() {
				uploadedFile.trigger(new File(uploadFile.getSpoolFileName()));
			}
		});

		uploadFile.fileTooLarge().addListener(this,
				new Signal1.Listener<Long>() {
					public void trigger(Long arg) {
						Dialogs.infoDialog("Info", "File too lagre: " + arg);
					}
				});
        
        if (!uploadFile.canUpload()) {
        	//TODO show an upload button when the upload is not performed 
        	//automatically, this is necessary when there is not javascript available?
        	//seems not to work properly?
        	
        	WPushButton uploadButton = new WPushButton("fileupload.upload-button");
        	uploadButton.clicked().addListener(this, new Signal1.Listener<WMouseEvent>() {
				public void trigger(WMouseEvent arg) {
					uploadFile.upload();
				}
			});
        }
	}
	
	public WFileUpload getWFileUpload() {
		return uploadFile;
	}

	public Signal1<File> uploadedFile() {
		return uploadedFile;
	}

	public void setMultiple(boolean multiple) {
		uploadFile.setMultiple(multiple);
	}
}
