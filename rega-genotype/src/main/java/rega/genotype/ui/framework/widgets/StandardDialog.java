package rega.genotype.ui.framework.widgets;

import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WPushButton;

/**
 * Standard dialog with OK button.
 * 
 * @author michael
 */
public class StandardDialog extends WDialog{
	public StandardDialog(String title){
		super(title);
		show();
		WPushButton okB = new WPushButton("OK", getFooter());
		WPushButton cancelB = new WPushButton("Cancel", getFooter());

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
}
