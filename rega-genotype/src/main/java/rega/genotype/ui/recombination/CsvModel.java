package rega.genotype.ui.recombination;

import rega.genotype.utils.Table;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.WAbstractTableModel;
import eu.webtoolkit.jwt.WModelIndex;

public class CsvModel extends WAbstractTableModel {
	private Table table;

	public CsvModel(Table table) {
		this.table = table;
	}

	public Object getHeaderData(int section, Orientation orientation, int role) {
		return table.valueAt(section, 0);
	}
	
	public int getColumnCount(WModelIndex parent) {
		if (parent == null)
			return table.numColumns() - 2;
		else 
			return 0;
	}

	public Object getData(WModelIndex index, int role) {
		return Double.parseDouble(table.valueAt(index.getColumn(), index.getRow() + 1));
	}

	public int getRowCount(WModelIndex parent) {
		if (parent == null)
			return table.numRows() - 1;
		else 
			return 0;
	}
}
