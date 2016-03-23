package rega.genotype.ui.framework;

import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WText;

/**
 * WMessageBox does not work due to 
 * WDialog#exec() requires a Servlet 3.0 enabled servlet container and an application with async-supported enabled.
 * 
 * @author michael
 */
public class MsgDialog extends WDialog{
	public MsgDialog(String title, String text){
		super(title);
		show();
		getContents().addWidget(new WText(text));
		setWidth(new WLength(300));
		WPushButton cancelB = new WPushButton("OK", getFooter());
		cancelB.clicked().addListener(cancelB, new Signal.Listener() {
			public void trigger() {
				reject();
			}
		});
	}
}
