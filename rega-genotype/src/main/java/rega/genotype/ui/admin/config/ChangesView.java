package rega.genotype.ui.admin.config;

import java.util.ArrayList;
import java.util.List;

import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.framework.widgets.StandardTableView;
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.WAbstractTableModel;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WText;

/**
 * View all logged changes of input tool.
 * 
 * Note: The log contains only the tools that are currently installed on this server (not all tool in public repo).
 * 
 * @author michael
 */
public class ChangesView extends WContainerWidget{

	public ChangesView(String toolId) {
		new WText("<div>View all logged changes of current tool.</div>" +
				"<div>Note: The log contains only the tools that are currently installed on this server (not all tool in public repo).</div>", this); 
		StandardTableView table = new StandardTableView(this);
		table.setModel(new ChagesModel(toolId));
		table.setColumnWidth(1, new WLength(550));
	}

	// classes
	
	public static class ChagesModel extends WAbstractTableModel {

		private String[] headers = {"Version", "Changed"};
		List<ToolManifest> tools = new ArrayList<ToolManifest>();
		
		public ChagesModel(String toolId) {
			List<ToolConfig> allTools = Settings.getInstance().getConfig().getTools();
			for (ToolConfig t: allTools)
				if (t.getId() != null && t.getId().equals(toolId))
					tools.add(t.getToolMenifest());
		}

		@Override
		public Object getHeaderData(int section, Orientation orientation,
				int role) {
			if (role == ItemDataRole.DisplayRole 
					&& orientation == Orientation.Horizontal)
				return headers[section];
			
			return super.getHeaderData(section, orientation, role);
		}
		
		@Override
		public int getColumnCount(WModelIndex parent) {
			return 2;
		}

		@Override
		public int getRowCount(WModelIndex parent) {
			return tools.size();
		}

		@Override
		public Object getData(WModelIndex index, int role) {
			if (role == ItemDataRole.DisplayRole) {
				switch (index.getColumn()) {
				case 0:
					return tools.get(index.getRow()).getVersion();
				case 1:
					return tools.get(index.getRow()).getCommitMessage();
				}
			}
			return null;
		}
		
	}
}
