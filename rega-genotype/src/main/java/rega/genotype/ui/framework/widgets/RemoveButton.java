package rega.genotype.ui.framework.widgets;

import java.util.EnumSet;

import eu.webtoolkit.jwt.Icon;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.StandardButton;
import eu.webtoolkit.jwt.WDialog.DialogCode;
import eu.webtoolkit.jwt.WMessageBox;
import eu.webtoolkit.jwt.WPushButton;

/**
 * Remove button with warning message.
 * 
 * @author michael
 */
public class RemoveButton extends WPushButton {
	private Signal accepted = new Signal();

	public RemoveButton(CharSequence text) {
		super(text);

		clicked().addListener(this, new Signal.Listener() {
			public void trigger() {
				WMessageBox d = new WMessageBox("Warning", dialogText(), Icon.NoIcon,
						EnumSet.of(StandardButton.Ok, StandardButton.Cancel));
				d.show();
				d.finished().addListener(d, new Signal1.Listener<DialogCode>() {
					public void trigger(DialogCode arg) {
						if (arg == DialogCode.Accepted) {
							accept();
							accepted.trigger();
						}
					}
				});
			}
		});
	}

	protected String dialogText() { 
		return "Are you sure that you want to remove ";
	}

	protected boolean accept() {
		return true;
	}

	public Signal accepted() {
		return accepted;
	}
}
