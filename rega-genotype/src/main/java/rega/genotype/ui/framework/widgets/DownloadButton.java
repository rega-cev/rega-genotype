package rega.genotype.ui.framework.widgets;

import java.io.File;

import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WPushButton;

/**
 * Allow browser save as from a button.
 * 
 * * The browsers allow save as only from an anchor.
 * 
 * @author michael
 */
public abstract class DownloadButton extends WContainerWidget{
	private WPushButton button = new WPushButton("Download", this);
	private WAnchor anchor = new WAnchor(this);
	/**
	 * executed before the anchor clicked is triggered.
	 */
	public abstract File downlodFile();

	public DownloadButton(String text) {
		button.setText(text);

		anchor.setTarget(AnchorTarget.TargetDownload);
		anchor.setText("Download");
		anchor.hide();

		button.clicked().addListener(button, new Signal.Listener() {
			public void trigger() {
				File downlodFile = downlodFile();
				if (downlodFile != null) {
					WFileResource resource = new WFileResource(
							"", downlodFile.getAbsolutePath());
					resource.suggestFileName(downlodFile.getName());
					anchor.setLink(new WLink(resource));

					// js from: http://jsbin.com/valoxufaba/2/edit?html,output
					String js = "if (" + anchor.getJsRef() + ".click) { \n"
							+ anchor.getJsRef() + ".click() \n"
							+ " } ";
					//"else if(document.createEvent) { \n"
					//+ "if(event.target !== anchorObj) {"
					//+ "var evt = document.createEvent(\"MouseEvents\"); "
					//+ "evt.initMouseEvent(\"click\", true, true, window, 0, 0, 0, 0, 0, "
					//+                    "false, false, false, false, 0, null); "
					//+ "var allowDefault = " + downloadA.getJsRef() + ".dispatchEvent(evt);"
					anchor.doJavaScript(js);
				}
			}
		});
	}

	public WAnchor getAnchor() {
		return anchor;
	}

	public WPushButton getButton() {
		return button;
	}
}
