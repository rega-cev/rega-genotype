package rega.genotype.ui.admin.file_editor.blast;

import java.util.HashSet;
import java.util.Set;

import rega.genotype.AlignmentAnalyses;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.taxonomy.TaxonomyModel;
import rega.genotype.ui.admin.file_editor.xml.ConfigXmlReader;
import rega.genotype.ui.admin.file_editor.xml.ConfigXmlWriter.ToolMetadata;
import rega.genotype.ui.framework.widgets.StandardItemModelSearchProxy;
import eu.webtoolkit.jwt.SelectionBehavior;
import eu.webtoolkit.jwt.SelectionMode;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WStandardItemModel;
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

		WStandardItemModel taxonomyModel = TaxonomyModel.getInstance().createModel();
		searchProxy = new StandardItemModelSearchProxy(taxonomyModel);
		// Note: Optimization: getTaxonomyIds is slow because it has to read blast.xml, not sure if it is worth to add cache for that.
		searchProxy.setVisibleLeafs(getTaxonomyIds(toolConfig));
		searchProxy.setFilterRole(TaxonomyModel.TAXONOMY_ID_ROLE);

		tree.setModel(searchProxy);
		tree.setSelectionBehavior(SelectionBehavior.SelectRows);
		tree.setSelectionMode(SelectionMode.SingleSelection);
		tree.setHeight(new WLength(400));
		tree.setSortingEnabled(true);
		tree.setMargin(10, Side.Top);

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
			info.setText("The taxonmy file is empty. Use the update taxonomy button (in Global config) to download it from uniprot.");
	}

	private Set<String> getTaxonomyIds(ToolConfig toolConfig) {
		if (toolConfig == null)
			return null;

		ToolMetadata metadata = ConfigXmlReader.readMetadata(toolConfig.getConfigurationFile());
		if (metadata.taxonomyIds == null) { // support old tools
			AlignmentAnalyses alignmentAnalyses = BlastFileEditor.readBlastXml(
					toolConfig.getConfigurationFile());
			Set<String> taxonomyIds = new HashSet<String>();
			for (Cluster c:alignmentAnalyses.getAllClusters()) 
				taxonomyIds.add(c.getTaxonomyId());					 
			return taxonomyIds;
		} else
			return metadata.taxonomyIds;
	}

	private void printWrongTaxons(Set<String> taxonomyIds) {
		// debug: some taxonomy ids are not found in uniprot file. 
		for (String s: taxonomyIds) 
			if (!TaxonomyModel.getInstance().getTaxons().containsKey(s))
				System.err.println("Hirarchy for taxon " + s + " not found");
	}
	public String getSelectedTaxonomyId() {
		return selectedTaxonomyId;
	}
}
