package rega.genotype.ui.framework.widgets;

import java.io.File;

import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;

/**
 * WResourde for save as files.
 * 
 * Note: The browsers allow save as only from an anchor.
 * 
 * @author michael
 */
public abstract class DownloadResource extends WFileResource{
	/**
	 * executed before the anchor clicked is triggered.
	 */
	public abstract File downlodFile();

	public DownloadResource(String mimeType, String fileName) {
		super(mimeType, fileName);
	}

	@Override
	public void handleRequest(WebRequest request, WebResponse response) {
		File downlodFile = downlodFile();
		if (downlodFile != null) {
			super.handleRequest(request, response);
		}
	}
}
