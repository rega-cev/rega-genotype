package rega.genotype.ui.framework.widgets;

import java.util.EnumSet;

import eu.webtoolkit.jwt.Icon;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.StandardButton;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WMessageBox;

/**
 * Standardize dialogs.
 * 
 * @author michael
 */
public class Dialogs{
	public static WMessageBox infoDialog(String title, String text){
		return infoDialog(title, text, EnumSet.of(StandardButton.Cancel));
	}

	public static WMessageBox infoDialog(String title, String text,
			EnumSet<StandardButton> buttons){
		final WMessageBox m = new WMessageBox(title, text, Icon.NoIcon, buttons);
		m.show();
		m.setWidth(new WLength(300));

		m.buttonClicked().addListener(m,
				new Signal1.Listener<StandardButton>() {
			public void trigger(StandardButton e1) {
				if(e1 == StandardButton.Ok)
					m.accept();
				else if(e1 == StandardButton.Cancel)
					m.reject();
			}
		});
		return m;
	}

	public static WMessageBox removeDialog(String itemsText){
		return infoDialog("Remove", "Are you sure that you want to remove " + itemsText,
				EnumSet.of(StandardButton.Ok, StandardButton.Cancel));
	}

}
