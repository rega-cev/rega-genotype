package rega.genotype.ui.framework.widgets;

import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WText;

/**
 * Standard dialog with OK button.
 * 
 * @author michael
 */
public class StandardDialog extends WDialog{
	private WPushButton okB;
	private WPushButton cancelB;

	public StandardDialog(String title){
		this(title, true);
	}

	public StandardDialog(String title, boolean okAccepts){
		super(title);
		show();
		okB = new WPushButton("OK", getFooter());
		cancelB = new WPushButton("Cancel", getFooter());
		if (okAccepts)
			okB.clicked().addListener(okB, new Signal.Listener() {
				public void trigger() {
					accept();
				}
			});

		cancelB.clicked().addListener(cancelB, new Signal.Listener() {
			public void trigger() {
				reject();
			}
		});
	}

	public WPushButton getOkB() {
		return okB;
	}

	public WPushButton getCancelB() {
		return cancelB;
	}

	public void addText(String text) {
		new WText(text, getContents());
	}
}
