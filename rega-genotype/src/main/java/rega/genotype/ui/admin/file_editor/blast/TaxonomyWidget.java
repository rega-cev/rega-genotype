package rega.genotype.ui.admin.file_editor.blast;

import java.util.HashMap;
import java.util.Map;

import rega.genotype.taxonomy.TaxonomyModel;
import rega.genotype.ui.framework.widgets.StandardItemModelSearchProxy;
import eu.webtoolkit.jwt.SelectionBehavior;
import eu.webtoolkit.jwt.SelectionMode;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTreeView;

public class TaxonomyWidget extends WContainerWidget {
	private WLineEdit searchLE;
	private WPushButton expandAllB;
	private WTreeView tree;
	private WText info;
	private StandardItemModelSearchProxy searchProxy;
	private Map<Integer, String> taxa = new HashMap<Integer, String>();
	private String selectedTaxonomyId = new String();
	
	public TaxonomyWidget() {
		new WText("Search", this);
		searchLE = new WLineEdit(this);
		expandAllB = new WPushButton("Expand all", this);
		tree = new WTreeView(this);
		info = new WText("", this);
		
		
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
					selectedTaxonomyId = (String) searchProxy.getData(tree.getSelectedIndexes().first());
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
	}

	public String getSelectedTaxonomyId() {
		return selectedTaxonomyId;
	}
}
