package rega.genotype.ui.forms;

import java.awt.Color;
import java.util.EnumSet;

import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.framework.widgets.TableView;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.AlignmentFlag;
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.SortOrder;
import eu.webtoolkit.jwt.ViewItemRenderFlag;
import eu.webtoolkit.jwt.WAbstractItemDelegate;
import eu.webtoolkit.jwt.WAbstractItemModel;
import eu.webtoolkit.jwt.WBrush;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WStandardItem;
import eu.webtoolkit.jwt.WStandardItemModel;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.chart.LabelOption;
import eu.webtoolkit.jwt.chart.WPieChart;

public class DefaultJobOverviewSummary extends JobOverviewSummary {
	private WStandardItemModel model;
	
	private WPieChart pieChart;
	private SummaryTableView table;
	
	private AbstractJobOverview jobOverview;
	
	private double total = 0;
	
	private class SummaryTableView extends TableView {
		public SummaryTableView(final WAbstractItemModel model,
				WContainerWidget parent) {
			super(parent);
			getTable().setStyleClass("assignment-overview");
			setModel(model);
			initTotalRow();
			
			model.layoutChanged().addListener(this, new Signal.Listener() {
				public void trigger() {
					setModel(model);
					initTotalRow();
				}
			});
		}

		private void initTotalRow() {
			int rowCount = getTable().getRowCount();
			getTable().insertRow(rowCount);
			getTable().getElementAt(rowCount, 0).addWidget(new WText(tr("detailsForm.summary.total")));
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

		new WText(tr("detailsForm.summary.title"), this.getElementAt(0, 0));
		
		table = new SummaryTableView(model, this.getElementAt(0, 0));
		this.getElementAt(0, 0).setVerticalAlignment(AlignmentFlag.AlignMiddle);
		model.dataChanged().addListener(this, new Signal2.Listener<WModelIndex, WModelIndex>() {
			public void trigger(WModelIndex arg1, WModelIndex arg2) {
				table.updateTotalRow(total);
			}
		});

		table.setItemDelegateForColumn(3, new WAbstractItemDelegate() {
			@Override
			public WWidget update(WWidget widget, WModelIndex index, EnumSet<ViewItemRenderFlag> flags) {
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
		this.getElementAt(0, 1).setVerticalAlignment(AlignmentFlag.AlignMiddle);
		pieChart.setModel(model);
		pieChart.setLabelsColumn(0);
		pieChart.setDataColumn(1);
        pieChart.setDisplayLabels(LabelOption.NoLabels);
        pieChart.resize(200, 150);
        pieChart.setStartAngle(90);
        
		this.setHidden(false);
	}
	
	public void update(GenotypeResultParser parser, OrganismDefinition od) {
		String assignment = formatAssignment(GenotypeLib.getEscapedValue(parser, "/genotype_result/sequence/conclusion/assigned/name"));
		
		if (table == null) {
			init();
		}
		
		boolean inserted = true;
		for (int i = 0; i < model.getRowCount(); i++) {
			String label = (String)model.getData(i, 0);
			int cmp = assignment.compareTo(label);
			if (cmp == 0) {
				model.setData(i, 1, (Integer)model.getData(i, 1) + 1);
				inserted = false;
				break;
			}
		}
		
		if (inserted) {
			int insertPosition = model.getRowCount();
							
			model.insertRow(insertPosition);
			
			String id = GenotypeLib.getEscapedValue(parser, "/genotype_result/sequence/conclusion/assigned/major/assigned/id");
			if (id == null) 
				id = GenotypeLib.getEscapedValue(parser, "/genotype_result/sequence/conclusion/assigned/id");
			
			Color c = od.getGenome().getAttributes().getColors().get(id);
			if (c == null)
				c = od.getGenome().getAttributes().getColors().get("other");
			WColor wc;
			if (c != null) {
				wc = new WColor(c.getRed(), c.getGreen(), c.getBlue());
			} else {
				wc = pieChart.getPalette().getBrush(Math.abs(assignment.hashCode())).getColor();
			}
			
			WStandardItem item = new WStandardItem(assignment);
			item.setLink(new WLink(WLink.Type.InternalPath, jobOverview.getJobPath() + "/" + JobForm.FILTER_PREFIX + encodeAssignment(assignment)));
			model.setItem(insertPosition, 0, item);
			model.setData(insertPosition, 1, 1);
			model.setData(insertPosition, 2, 0.0);
			model.setData(insertPosition, 3, wc, ItemDataRole.UserRole + 1);
		}

		model.sort(1, SortOrder.DescendingOrder);

		total++;
		
		for (int i = 0; i < model.getRowCount(); i++) {
			int v = ((Integer)model.getData(i, 1));
			if (v > 0)
				model.setData(i, 2, String.format("%.3g%%", v / total * 100.0));
			WBrush b = new WBrush((WColor) model.getData(i, 3, ItemDataRole.UserRole + 1));
			pieChart.setBrush(i, b);
		}
	}
	
	public void reset() {
		if (table != null) {
			this.clear();
			table = null;
			total = 0;
		}
	}

	@Override
	public Side getLocation() {
		return Side.Top;
	}
}
