package rega.genotype.ui.admin.config;

import rega.genotype.config.Config.ToolConfig;
import rega.genotype.ui.framework.widgets.Template;
import eu.webtoolkit.jwt.SelectionBehavior;
import eu.webtoolkit.jwt.SelectionMode;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WTableView;

/**
 * Show list of tools that can be edited with ToolConfigDialog.
 * 
 * @author michael
 */
public class ToolConfigTable extends Template{
	ToolConfigTableModel model;

	public ToolConfigTable(WContainerWidget parent) {
		super(tr("admin.config.tool-config-table"), parent);

		model = new ToolConfigTableModel();
		final WTableView table = new WTableView();
		table.setSelectionMode(SelectionMode.SingleSelection);
		table.setSelectionBehavior(SelectionBehavior.SelectRows);
		table.setModel(model);
		
		WPushButton addB = new WPushButton("Add");
		WPushButton editB = new WPushButton("Edit");
		
		addB.clicked().addListener(addB, new Signal.Listener() {
			public void trigger() {
				edit(null);
			}
		});

		table.doubleClicked().addListener(table, new Signal2.Listener<WModelIndex, WMouseEvent>() {

			public void trigger(WModelIndex index, WMouseEvent arg2) {
				if (index == null)
					return;
				edit(model.getToolConfig(index.getRow()));
			}
		});

		editB.clicked().addListener(editB, new Signal.Listener() {
			public void trigger() {
				if (table.getSelectedIndexes().size() == 1) {
				edit(model.getToolConfig(
						table.getSelectedIndexes().first().getRow()));
				}
			}
		});
		bindWidget("table", table);
		bindWidget("add", addB);
		bindWidget("edit", editB);
	}

	private void edit(ToolConfig config) {
		ToolConfigDialog d = new ToolConfigDialog(config);
		d.finished().addListener(d, new Signal1.Listener<WDialog.DialogCode>() {
			public void trigger(WDialog.DialogCode arg) {
				if (arg == WDialog.DialogCode.Accepted) {
					model.refresh();
				}
			}
		});
	}
}
