/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.util;

import java.io.File;

import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileUpload;

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
        
        uploadFile.uploaded().addListener(this, new Signal.Listener() {
			public void trigger() {
				uploadedFile.trigger(new File(uploadFile.getSpoolFileName()));
			}
		});
        
        //TODO allow changing of file, something like http://jasny.github.com/bootstrap/javascript.html#fileupload
        //TODO add support for browser with no auto submit for file upload
	}
	
	public Signal1<File> uploadedFile() {
		return uploadedFile;
	}
}
