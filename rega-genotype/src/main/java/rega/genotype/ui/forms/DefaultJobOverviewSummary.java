package rega.genotype.ui.forms;

import java.awt.Color;
import java.util.EnumSet;

import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.data.OrganismDefinition;
import eu.webtoolkit.jwt.AlignmentFlag;
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.ViewItemRenderFlag;
import eu.webtoolkit.jwt.WAbstractItemDelegate;
import eu.webtoolkit.jwt.WAbstractItemModel;
import eu.webtoolkit.jwt.WBrush;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WStandardItem;
import eu.webtoolkit.jwt.WStandardItemModel;
import eu.webtoolkit.jwt.WTableView;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.chart.LabelOption;
import eu.webtoolkit.jwt.chart.WPieChart;

public class DefaultJobOverviewSummary extends JobOverviewSummary {
	private WStandardItemModel model;
	
	private WPieChart pieChart;
	private SummaryTableView table;
	
	private AbstractJobOverview jobOverview;
	
	//TODO use the text in the table
	private double total = 0;
	
	private class SummaryTableView extends WTableView {
		public SummaryTableView(WAbstractItemModel model,
				WContainerWidget parent) {
			super(parent);
			getTable().setStyleClass("assignment-overview");
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
	
	public DefaultJobOverviewSummary(AbstractJobOverview jobOverview) {
		this.jobOverview = jobOverview;
		this.setHidden(true);
	}
	
	private void init() {
		this.setStyleClass("jobOverviewSummary");
		
		model = new WStandardItemModel();
		
		model.insertColumns(0, 4);

		model.setHeaderData(0, tr("detailsForm.summary.assignment"));
		model.setHeaderData(1, tr("detailsForm.summary.numberSeqs"));
		model.setHeaderData(2, tr("detailsForm.summary.percentage"));
		model.setHeaderData(3, tr("detailsForm.summary.legend"));
		
		table = new SummaryTableView(model, this.getElementAt(0, 0));
		this.getElementAt(0, 0).setContentAlignment(AlignmentFlag.AlignRight);
		this.getElementAt(0, 0).setVerticalAlignment(AlignmentFlag.AlignMiddle);
		model.dataChanged().addListener(this, new Signal2.Listener<WModelIndex, WModelIndex>() {
			public void trigger(WModelIndex arg1, WModelIndex arg2) {
				table.updateTotalRow(total);
			}
		});
		table.setItemDelegateForColumn(3, new WAbstractItemDelegate(){
			@Override
			public WWidget update(WWidget widget, WModelIndex index,
					EnumSet<ViewItemRenderFlag> flags) {
				WContainerWidget w = new WContainerWidget();
				w.setStyleClass("legend-item");
				WColor c = (WColor)index.getData(ItemDataRole.UserRole + 1);
				if (c != null)
					w.getDecorationStyle().setBackgroundColor(c);
				
				w.setMargin(WLength.Auto, Side.Left, Side.Right);
				
				return w;
			}
		});
		
		pieChart = new WPieChart(this.getElementAt(0, 1));
		this.getElementAt(0, 1).setContentAlignment(AlignmentFlag.AlignLeft);
		this.getElementAt(0, 1).setVerticalAlignment(AlignmentFlag.AlignMiddle);
		pieChart.setModel(model);
		pieChart.setLabelsColumn(0);
		pieChart.setDataColumn(1);
        pieChart.setDisplayLabels(LabelOption.NoLabels);
        pieChart.resize(200, 150);
        
		this.setHidden(false);
	}
	
	public void update(GenotypeResultParser parser, OrganismDefinition od) {
		String assignment = formatAssignment(parser.getEscapedValue("/genotype_result/sequence/conclusion/assigned/name"));
		
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
			
			String majorAssignment = parser.getEscapedValue("/genotype_result/sequence/conclusion/assigned/major/assigned/id");
			if (majorAssignment == null) 
				majorAssignment = assignment;
			
			Color c = od.getGenome().getAttributes().getColors().get(majorAssignment);
			WColor wc;
			if (c != null) {
				wc = new WColor(c.getRed(), c.getGreen(), c.getBlue());
			} else {
				wc = pieChart.getPalette().getBrush(model.getRowCount()).getColor();
			}
			WBrush b = new WBrush(wc);
			pieChart.setBrush(insertPosition, b);
			
			WStandardItem item = new WStandardItem(assignment);
			item.setInternalPath(jobOverview.getJobPath() + "/" + encodeAssignment(assignment));
			model.setItem(insertPosition, 0, item);
			model.setData(insertPosition, 1, 1);
			model.setData(insertPosition, 2, 0.0);
			model.setData(insertPosition, 3, wc, ItemDataRole.UserRole + 1);
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

	public void reset() {
		if (table != null) {
			this.clear();
			table = null;
			total = 0;
		}
	}
}
