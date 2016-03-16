package rega.genotype.ui.admin.repository;

import java.util.List;

import rega.genotype.config.ToolManifest;
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.WAbstractTableModel;
import eu.webtoolkit.jwt.WModelIndex;

public class ToolRepoTableModel  extends WAbstractTableModel {
	String[] headers = {"name", "Id", "Version", "Creation date"};
	List<ToolManifest> manifests;
	public ToolRepoTableModel(List<ToolManifest> manifests) {
		this.manifests = manifests;
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
		return manifests.size();
	}

	@Override
	public Object getData(WModelIndex index, int role) {
		if (role == ItemDataRole.DisplayRole) {
			ToolManifest toolManifest = getToolManifestint(index.getRow());
			switch (index.getColumn()) {
			case 0:
				return toolManifest.getName();
			case 1:
				return toolManifest.getId();
			case 2:
				return toolManifest.getVersion();
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

	public ToolManifest getToolManifestint(int row) {
		 return manifests.get(row);
	}
}
