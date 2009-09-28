package rega.genotype.ui.forms;

import java.awt.Color;

import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.data.OrganismDefinition;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.WAbstractItemModel;
import eu.webtoolkit.jwt.WBrush;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WContainerWidget;
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
	
	//TODO use the text in the table
	private double total = 0;
	
	private final String CHECK_THE_BOOTSCAN = "Check the bootscan";
	private final String NOT_ASSIGNED = "Not assigned";
	
	private class SummaryTableView extends WTableView {
		public SummaryTableView(WAbstractItemModel model,
				WContainerWidget parent) {
			super(parent);
			setModel(model);
			initTotalRow();
		}

		private void initTotalRow() {
			int rowCount = getTable().getRowCount();
			getTable().insertRow(rowCount);
			getTable().getElementAt(rowCount, 0).addWidget(
					new WText(tr("detailsForm.summary.total")));
			getTable().getRowAt(rowCount).setStyleClass("assignment-overview-total");
			getTable().getElementAt(rowCount, 2).addWidget(new WText("100%"));
		}

		public void updateTotalRow(double total) {
			int rowCount = getTable().getRowCount()-1;
			getTable().getElementAt(rowCount, 1).clear();
			getTable().getElementAt(rowCount, 1).addWidget(new WText((int)total + ""));
		}
	}
	
	public DefaultJobOverviewSummary() {
		this.setHidden(true);
	}
	
	private void init() {
		setStyleClass("assignment-overview");
		
		model = new WStandardItemModel();
		
		model.insertColumns(0, 3);

		model.setHeaderData(0, tr("detailsForm.summary.assignment"));
		model.setHeaderData(1, tr("detailsForm.summary.numberSeqs"));
		model.setHeaderData(2, tr("detailsForm.summary.percentage"));
		
		table = new SummaryTableView(model, this);
		model.dataChanged().addListener(this, new Signal2.Listener<WModelIndex, WModelIndex>() {
			public void trigger(WModelIndex arg1, WModelIndex arg2) {
				table.updateTotalRow(total);
			}
		});
		
		pieChart = new WPieChart(this);
		pieChart.setModel(model);
		pieChart.setLabelsColumn(0);
		pieChart.setDataColumn(1);
        pieChart.setDisplayLabels(LabelOption.Outside, LabelOption.TextLabel);
        pieChart.resize(500, 300);
        pieChart.setPlotAreaPadding(50);
        
		this.setHidden(false);
	}
	
	public void update(GenotypeResultParser parser, OrganismDefinition od) {
		String assignment = formatAssignment(parser.getEscapedValue("genotype_result.sequence.conclusion.assigned.name"));
		
		if (table == null) {
			init();
		}
		
		boolean inserted = true;
		String label;
		int cmp;
		for (int i = 0; i < model.getRowCount(); i++) {
			label = (String)model.getData(i, 0);
			cmp = assignment.compareTo(label);
			if (cmp == 0) {
				model.setData(i, 1, (Integer)model.getData(i, 1) + 1);
				inserted = false;
				break;
			}
		}
		
		if (inserted) {
			int insertPosition = determinePosition(assignment);
							
			model.insertRow(insertPosition);
			
			String majorAssignment = parser.getEscapedValue("genotype_result.sequence.conclusion.assigned.major.assigned.id");
			WBrush brush = pieChart.getBrush(insertPosition);
			Color c = od.getGenome().COLORS().get(majorAssignment);
			if (c != null) {
				brush.setColor(new WColor(c.getRed(), c.getGreen(), c.getBlue()));
				pieChart.setBrush(insertPosition, brush);
			}
			
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
	
	private int determinePosition(String assignment) {
		if (assignment.equals(CHECK_THE_BOOTSCAN) ||
				assignment.equals(this.NOT_ASSIGNED))
			return model.getRowCount();
			
		String label;
		for (int i = 0; i < model.getRowCount(); i++) {
			label = (String)model.getData(i, 0);
			if (label.equals(CHECK_THE_BOOTSCAN) ||
					label.equals(this.NOT_ASSIGNED))
				return i;
		}

		return 0;
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
			this.removeWidget(table);
			this.removeWidget(pieChart);
			table = null;
			total = 0;
		}
	}
}
