package rega.genotype.ui.admin.file_editor.blast;

import rega.genotype.ui.framework.widgets.StandardDialog;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WText;

public class TaxonomyButton extends WText {
	// arg = SelectedTaxonomyId
	private Signal1<String> finished = new Signal1<String>();

	public TaxonomyButton() {

		setStyleClass("hoverable");

		clicked().addListener(this, new Signal.Listener() {
			public void trigger() {
				final StandardDialog d = new StandardDialog("Choose taxonomy id");
				final TaxonomyWidget taxonomyWidget = new TaxonomyWidget(null);
				d.getContents().addWidget(taxonomyWidget);
				d.setWidth(new WLength(600));
				d.setHeight(new WLength(500));
				d.finished().addListener(d, new Signal1.Listener<WDialog.DialogCode>() {
					public void trigger(WDialog.DialogCode arg) {
						if(arg == WDialog.DialogCode.Accepted) {
							finished.trigger(taxonomyWidget.getSelectedTaxonomyId());
						}
					}
				});

			}
		});
	}

	public void setTaxonomyIdText(String taxonomyId) {
		if (taxonomyId == null || taxonomyId.isEmpty())
			setText("(Empty)");
		else
			setText(taxonomyId);
	}

	public String getValue() {
		if (getText().toString().equals("(Empty)"))
			return null;
		else
			return getText().toString();
	}

	public Signal1<String> finished() {
		return finished;
	}
}
