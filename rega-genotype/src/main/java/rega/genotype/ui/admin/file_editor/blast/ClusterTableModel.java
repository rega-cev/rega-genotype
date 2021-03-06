package rega.genotype.ui.admin.file_editor.blast;

import java.util.List;

import rega.genotype.AlignmentAnalyses.Cluster;

import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.WAbstractTableModel;
import eu.webtoolkit.jwt.WModelIndex;

public class ClusterTableModel extends WAbstractTableModel{
	public static final int CLUSTER_ID_COLUMN = 0;
	public static final int NAME_COLUMN = 1;
	public static final int TOOL_ID_COLUMN = 2;
	public static final int SRC_ID_COLUMN = 3;

	String[] headers = { "Cluster ID", "Name", "Taxonomy ID", "Source"};

	private List<Cluster> clusters;

	public ClusterTableModel(List<Cluster> clusters) {
		this.clusters = clusters;		
	}

	public Cluster getCluster(int row) {
		return clusters.get(row);
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
		return clusters.size();
	}

	@Override
	public Object getData(WModelIndex index, int role) {
		Cluster cluster = getCluster(index.getRow());
		if (role == ItemDataRole.DisplayRole) {
			switch (index.getColumn()) {
			case CLUSTER_ID_COLUMN:
				return cluster.getId();
			case NAME_COLUMN:
				return cluster.getName();
			case TOOL_ID_COLUMN:					
				return cluster.getTaxonomyId();
			case SRC_ID_COLUMN:
				return cluster.getSource();
			default:
				break;
			}
		}
		return null;
	}

	public void refresh(List<Cluster> clusters) {
		this.clusters = clusters;
		refresh();
	}

	public void refresh() {
		layoutAboutToBeChanged().trigger();
		layoutChanged().trigger();
	}
}
