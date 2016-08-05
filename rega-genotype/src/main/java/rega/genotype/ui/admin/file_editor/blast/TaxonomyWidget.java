package rega.genotype.ui.admin.file_editor.blast;

import rega.genotype.taxonomy.TaxonomyModel;
import rega.genotype.ui.framework.widgets.StandardDialog;
import rega.genotype.ui.framework.widgets.StandardItemModelSearchProxy;
import eu.webtoolkit.jwt.SelectionBehavior;
import eu.webtoolkit.jwt.SelectionMode;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTreeView;

public class TaxonomyWidget extends WText {
	// arg = SelectedTaxonomyId
	private Signal1<String> finished = new Signal1<String>();
	private String selectedTaxonomyId = new String();

	public TaxonomyWidget() {

		setStyleClass("hoverable");

		clicked().addListener(this, new Signal.Listener() {
			public void trigger() {
				final StandardDialog d = new StandardDialog("Choose taxonomy id");
				d.getContents().addWidget(createCentralWidget());
				d.setWidth(new WLength(600));
				d.setHeight(new WLength(500));
				d.finished().addListener(d, new Signal1.Listener<WDialog.DialogCode>() {
					public void trigger(WDialog.DialogCode arg) {
						if(arg == WDialog.DialogCode.Accepted) {
							finished.trigger(selectedTaxonomyId);
						}
					}
				});

			}
		});
	}

	private WContainerWidget createCentralWidget() {
		WContainerWidget centralWidget = new WContainerWidget();
		final WLineEdit searchLE;
		final WPushButton expandAllB;
		final WTreeView tree;
		final WText info;
		final StandardItemModelSearchProxy searchProxy;

		new WText("Search", centralWidget);
		searchLE = new WLineEdit(centralWidget);
		expandAllB = new WPushButton("Expand all", centralWidget);
		tree = new WTreeView(centralWidget);
		info = new WText("", centralWidget);


		searchLE.addStyleClass("standard-margin");
		expandAllB.addStyleClass("standard-margin");
		searchLE.addStyleClass("standard-margin");

		TaxonomyModel taxonomyModel = TaxonomyModel.getInstance();
		searchProxy = new StandardItemModelSearchProxy(taxonomyModel);
		tree.setModel(searchProxy);
		tree.setSelectionBehavior(SelectionBehavior.SelectRows);
		tree.setSelectionMode(SelectionMode.SingleSelection);
		tree.setHeight(new WLength(400));
		tree.setSortingEnabled(true);

		searchLE.textInput().addListener(searchLE, new Signal.Listener() {
			public void trigger() {
				searchProxy.setSearchText(searchLE.getText());
			}
		});
		searchLE.setInline(true);
		searchLE.setText("");
		searchLE.setPlaceholderText("Search");
		searchProxy.setSearchText("");

		tree.selectionChanged().addListener(info, new Signal.Listener() {
			public void trigger() {
				if (tree.getSelectedIndexes().size() == 1) { 
					selectedTaxonomyId = (String) searchProxy.getData(tree.getSelectedIndexes().first(), TaxonomyModel.TAXONOMY_ID_ROLE);
					info.setText(selectedTaxonomyId);
				}
			}
		});

		expandAllB.clicked().addListener(expandAllB, new Signal.Listener() {
			public void trigger() {
				tree.expandToDepth(10);
			}
		});

		if (taxonomyModel.getRowCount() == 0)
			info.setText("The taxonmy file is empty. Use the update taxonomy button (under the tools table) to download it from uniprot.");

		return centralWidget;
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
