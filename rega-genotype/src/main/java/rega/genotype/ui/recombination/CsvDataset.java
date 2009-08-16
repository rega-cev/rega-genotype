package rega.genotype.ui.recombination;

import org.jfree.data.DomainOrder;
import org.jfree.data.xy.AbstractXYDataset;

public class CsvDataset extends AbstractXYDataset {
	private Table t_;

	public CsvDataset(Table t) {
		t_ = t;
	}

	@Override
	public DomainOrder getDomainOrder() {
		return DomainOrder.ASCENDING;
	}

	public int getItemCount(int arg0) {
		return t_.numRows() - 1;
	}

	public Number getX(int arg0, int arg1) {
		return Double.parseDouble(t_.valueAt(0, arg1 + 1));
	}

	@Override
	public double getXValue(int arg0, int arg1) {
		return Double.parseDouble(t_.valueAt(0, arg1 + 1));
	}

	public Number getY(int arg0, int arg1) {
		return Double.parseDouble(t_.valueAt(arg0 + 1, arg1 + 1));
	}

	@Override
	public double getYValue(int arg0, int arg1) {
		return Double.parseDouble(t_.valueAt(arg0 + 1, arg1 + 1));
	}

	@Override
	public int getSeriesCount() {
		return t_.numColumns() - 3;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Comparable getSeriesKey(int arg0) {
		return t_.valueAt(arg0 + 1, 0);
	}
}
