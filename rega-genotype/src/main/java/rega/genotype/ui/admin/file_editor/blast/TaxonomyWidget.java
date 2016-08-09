package rega.genotype.ui.admin.file_editor.blast;

import java.util.HashSet;
import java.util.Set;

import rega.genotype.AlignmentAnalyses;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.config.Config.ToolConfig;
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

/**
 * Show taxonomy tree.
 * 
 * @author michael
 */
public class TaxonomyWidget extends WContainerWidget {
	private String selectedTaxonomyId = new String();

	public TaxonomyWidget(ToolConfig toolConfig) {
		final WLineEdit searchLE;
		final WPushButton expandAllB;
		final WTreeView tree;
		final WText info;
		final StandardItemModelSearchProxy searchProxy;

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
		searchProxy.setVisibleLeafs(getTaxonomyIds(toolConfig));
		searchProxy.setFilterRole(TaxonomyModel.TAXONOMY_ID_ROLE);

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
				if (tree.getSelectedIndexes().size() == 1)
					selectedTaxonomyId = (String) searchProxy.getData(
							tree.getSelectedIndexes().first(), 
							TaxonomyModel.TAXONOMY_ID_ROLE);
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

	private Set<String> getTaxonomyIds(ToolConfig toolConfig) {
		if (toolConfig == null)
			return null;

		AlignmentAnalyses alignmentAnalyses = BlastFileEditor.readBlastXml(
				toolConfig.getConfigurationFile());
		Set<String> taxonomyIds = new HashSet<String>();
		for (Cluster c:alignmentAnalyses.getAllClusters()) 
				taxonomyIds.add(c.getTaxonomyId());

		return taxonomyIds;
	}

	private void printWrongTaxons(Set<String> taxonomyIds) {
		// debug: some taxonomy ids are not found in uniprot file. 
		TaxonomyModel taxonomyModel = TaxonomyModel.getInstance();
		for (String s: taxonomyIds) 
			if (!taxonomyModel.getTaxons().containsKey(s))
				System.err.println("Hirarchy for taxon " + s + " not found");
	}
	public String getSelectedTaxonomyId() {
		return selectedTaxonomyId;
	}
}
