package rega.genotype.ui.framework.widgets;

import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WTableView;

/**
 * WTableView with some extra functions that can be useful for this project.
 * 
 * @author michael
 */
public class StandardTableView extends WTableView{
	private static final double COLUMN_PADDING = 7;
	private static final double SCROLL_SIZE = 14;

	public StandardTableView() {
		super();
	}

	public StandardTableView(WContainerWidget parent) {
		super(parent);
	}
	/**
	 * calculate and set table width, considering the existing columns.
	 */
	public void setTableWidth(int columnCount) {
		// set table widths
		double tableWidth = 2;
		for(int i=0; i < columnCount; i++) {
			tableWidth += getColumnWidth(i).getValue() + COLUMN_PADDING;
		}
		setWidth(new WLength(tableWidth + SCROLL_SIZE));
	}

	public void setTableWidth() {
		setTableWidth(getModel().getColumnCount());
	}

}
