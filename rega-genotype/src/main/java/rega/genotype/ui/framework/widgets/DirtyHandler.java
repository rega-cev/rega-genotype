package rega.genotype.ui.framework.widgets;

import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WFormModel;
import eu.webtoolkit.jwt.WFormWidget;
import eu.webtoolkit.jwt.WInteractWidget;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WTemplateFormView;
import eu.webtoolkit.jwt.WWidget;
/**
 * Used to check if UI is dirty (some thing was changed compared to the database).
 * If the model is dirty saving is allowed and "There is unsaved changes window 
 * will pop-up".
 */
public class DirtyHandler {	
	private Signal dirty = new Signal();
	private Signal clean = new Signal();

	private int dirtyCount = 0; // count how much children are dirty.

	// listeners //
	public Signal.Listener dirtySetter = new Signal.Listener() {
		public void trigger() {
			increaseDirty();
		}
	};

	public Signal.Listener cleanSetter = new Signal.Listener() {
		public void trigger() {
			setClean();
		}
	};

	public void increaseDirty() {
		dirtyCount++;
		if (dirtyCount == 1)
			dirty.trigger();
	}

	public void decreaseDirty() {
		if (dirtyCount > 0)
			dirtyCount--;
		if (dirtyCount == 0)
			clean.trigger();
	}

	public void setClean() {
		if (dirtyCount != 0) {
			dirtyCount = 0;
			clean.trigger();
		}
	}

	public boolean isDirty(){
		return dirtyCount > 0;
	}

	public Signal dirty(){
		return dirty;
	}

	public Signal clean(){
		return clean;
	}

	// connect //

	public void connect(final DirtyHandler dirtyHandler, WWidget owner) {
		dirtyHandler.dirty().addListener(owner, new Signal.Listener() {
			public void trigger() {
				increaseDirty();
			}
		});

		dirtyHandler.clean().addListener(owner, new Signal.Listener() {
			public void trigger() {
				decreaseDirty();
			}
		});
	}

	public void connect(final WFormWidget wFormWidget) {
		if(wFormWidget != null){
			if (wFormWidget instanceof WLineEdit)
				wFormWidget.keyWentUp().addListener(wFormWidget, dirtySetter);
			wFormWidget.changed().addListener(wFormWidget, dirtySetter);
		}
	}

	/**
	 * Connect DirtyHandler to all WFormWidget in the view.
	 * IMPORTANT: will not work for widget that do not inherit from WFormWidget.
	 * @param model
	 * @param dirtyHandler
	 */
	public void connect(final WFormModel model, final WTemplateFormView formView) {
		for (String field: model.getFields()) {
			WWidget w = formView.resolveWidget(field);
			if (w instanceof WFormWidget)
				this.connect((WFormWidget)w);
		}
	}

	public void connect(final WInteractWidget wInteractWidget) {
		for (WWidget w: wInteractWidget.getChildren()){
			if (w instanceof WFormWidget)
				this.connect((WFormWidget)w);
		}
	}

}
