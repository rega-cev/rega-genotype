package rega.genotype.ui.admin.config;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.WAbstractTableModel;
import eu.webtoolkit.jwt.WModelIndex;

public class ToolConfigTableModel extends WAbstractTableModel {
	String[] headers = { "Id", "Version", "URL component" };
	Config config;
	public ToolConfigTableModel() {
		this.config = Settings.getInstance().getConfig();
	}

	@Override
	public Object getHeaderData(int section, Orientation orientation, int role) {
		if (role == ItemDataRole.DisplayRole 
				&& orientation == Orientation.Horizontal
				&& section < headers.length) {
			return headers[section];
		} else
			return super.getHeaderData(section, orientation, role);
	}

	@Override
	public int getColumnCount(WModelIndex parent) {
		return headers.length;
	}

	@Override
	public int getRowCount(WModelIndex parent) {
		return config.getTools().size();
	}

	@Override
	public Object getData(WModelIndex index, int role) {
		if (role == ItemDataRole.DisplayRole) {
			ToolConfig toolConfig = getToolConfig(index.getRow());
			switch (index.getColumn()) {
			case 0:
				return toolConfig.getId();
			case 1:
				return toolConfig.getVersion();
			case 2:
				return toolConfig.getPath();
			default:
				break;
			}
		}
		return null;
	}

	public void refresh() {
		layoutAboutToBeChanged().trigger();
		layoutChanged().trigger();
	}

	public ToolConfig getToolConfig(int row) {
		 return config.getTools().get(row);
	}
}
