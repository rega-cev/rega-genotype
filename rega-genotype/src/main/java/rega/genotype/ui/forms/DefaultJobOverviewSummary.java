package rega.genotype.ui.forms;

import rega.genotype.ui.data.GenotypeResultParser;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.WAbstractItemModel;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WStandardItemModel;
import eu.webtoolkit.jwt.WTableView;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.chart.LabelOption;
import eu.webtoolkit.jwt.chart.WPieChart;

public class DefaultJobOverviewSummary extends WContainerWidget implements JobOverviewSummary {
	private WStandardItemModel model;
	
	private WPieChart pieChart;
	private SummaryTableView table;
	
	private double total = 0;
	
	private final String CHECK_THE_BOOTSCAN = "Check the bootscan";
	private final String NOT_ASSIGNED = "Not assigned";
	
	private final static class SummaryTableView extends WTableView {
		public SummaryTableView(WAbstractItemModel model,
				WContainerWidget parent) {
			super(model, parent);
			initTotalRow();
		}

		private void initTotalRow() {
			int rowCount = table_.getRowCount();
			table_.insertRow(rowCount);
			table_.getElementAt(rowCount, 0).addWidget(
					new WText(tr("detailsForm.summary.total")));
			table_.getElementAt(rowCount, 2).addWidget(new WText("100%"));
		}

		public void updateTotalRow(double total) {
			int rowCount = table_.getRowCount()-1;
			table_.getElementAt(rowCount, 1).clear();
			table_.getElementAt(rowCount, 1).addWidget(new WText((int)total + ""));
		}
	}
	
	public DefaultJobOverviewSummary() {
		model = new WStandardItemModel();
		
		pieChart = new WPieChart(this);
		
		this.setHidden(true);
	}
	
	private void init() {
		model = new WStandardItemModel();
		
		model.insertColumns(0, 3);
		model.insertRow(0);
		model.setData(0, 0, CHECK_THE_BOOTSCAN);
		model.setData(0, 1, 0);
		model.setData(0, 2, 0);
		model.insertRow(1);
		model.setData(1, 0, NOT_ASSIGNED);
		model.setData(1, 1, 0);
		model.setData(1, 2, 0);

		
		model.setHeaderData(0, tr("detailsForm.summary.assignment"));
		model.setHeaderData(1, tr("detailsForm.summary.numberSeqs"));
		model.setHeaderData(2, tr("detailsForm.summary.percentage"));
		
		table = new SummaryTableView(model, this);
		model.dataChanged().addListener(this, new Signal2.Listener<WModelIndex, WModelIndex>() {
			public void trigger(WModelIndex arg1, WModelIndex arg2) {
				table.updateTotalRow(total);
			}
		});
		
		pieChart.setModel(model);
		pieChart.setLabelsColumn(0);
		pieChart.setDataColumn(1);
        pieChart.setDisplayLabels(LabelOption.Outside, LabelOption.TextLabel);
        pieChart.resize(200, 150);
        pieChart.setMargin(20, Side.Top, Side.Bottom);
        pieChart.setMargin(WLength.Auto, Side.Left, Side.Right);
        
		this.setHidden(false);
	}
	
	public void update(GenotypeResultParser parser) {
		String assignment = formatAssignment(parser.getEscapedValue("genotype_result.sequence.conclusion.assigned.name"));
		
		if (table == null) {
			init();
		}
		
		Integer insertPosition = 0;
		String label;
		int cmp;
		for (int i = 0; i < model.getRowCount(); i++) {
			label = (String)model.getData(i, 0);
			cmp = assignment.compareTo(label);
			if (cmp == 0) {
				model.setData(i, 1, (Integer)model.getData(i, 1) + 1);
				insertPosition = null;
				break;
			} else if (cmp > 0 && i < model.getRowCount() - 2) {
				insertPosition = i + 1;
			}
		}
		
		if (insertPosition != null) {
			model.insertRow(insertPosition);
			model.setData(insertPosition, 0, assignment);
			model.setData(insertPosition, 1, 1);
			model.setData(insertPosition, 2, 0.0);
		}
		
		total++;
		
		for (int i = 0; i < model.getRowCount(); i++) {
			int v = ((Integer)model.getData(i, 1));
			if (v > 0)
				model.setData(i, 2, String.format("%.3g%%", v / total * 100.0));
		}
	}
	
	protected String formatAssignment(String assignment) {
		if (assignment == null) {
			assignment = NOT_ASSIGNED;
		}
		
		return assignment;
	}

	public WContainerWidget getWidget() {
		return this;
	}

	public void reset() {
		if (table != null) {
			this.removeChild(table);
			table = null;			
		}
	}
}
