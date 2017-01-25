package rega.genotype.ui.framework.widgets;

import java.util.EnumSet;

import eu.webtoolkit.jwt.AlignmentFlag;
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.PositionScheme;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.ViewItemRenderFlag;
import eu.webtoolkit.jwt.WAbstractItemDelegate;
import eu.webtoolkit.jwt.WAbstractItemModel;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WPainter;
import eu.webtoolkit.jwt.WRectF;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.chart.LabelOption;
import eu.webtoolkit.jwt.chart.WChartPalette;
import eu.webtoolkit.jwt.chart.WPieChart;

/**
 * Chart and table display the data from the same model.
 * 
 * @author michael
 */
public class ChartTableWidget extends WContainerWidget{
	protected WContainerWidget chartContainer = new WContainerWidget(); // used as a layer to draw the anchors on top of the chart.
	protected WPieChart chart;
	private StandardTableView table = new StandardTableView();
	private int chartDataColumn;
	private int chartDisplayColumn;
	private int colorColumn;
	private WAbstractItemModel model;
	private WChartPalette chartPalette;

	public ChartTableWidget(WAbstractItemModel model, int chartDataColumn, 
			int chartDisplayColumn, int colorColumn, WChartPalette chartPalette) {
		this.model = model;
		this.chartDataColumn = chartDataColumn;
		this.chartDisplayColumn = chartDisplayColumn;
		this.colorColumn = colorColumn;
		this.chartPalette = chartPalette;

		createChart();
		initTable();
	}

	private void createChart() {
		addWidget(chartContainer);
		chart = new WPieChart() {
			@Override
			protected void drawLabel(WPainter painter, WRectF rect,
					EnumSet<AlignmentFlag> alignmentFlags, CharSequence text,
					int row) {
				if (getModel().link(row, getDataColumn()) == null)
					super.drawLabel(painter, rect, alignmentFlags, text, row);
				else{
					WAnchor a = new WAnchor(getModel().link(row, getDataColumn()), text);
					chartContainer.addWidget(
							createLabelWidget(a, painter, rect, alignmentFlags));
				}
			}
		};
		chartContainer.addWidget(chart);
		chartContainer.setPositionScheme(PositionScheme.Relative);

		chartContainer.resize(800, 300);
		chartContainer.setMargin(new WLength(30), EnumSet.of(Side.Top, Side.Bottom));
		chartContainer.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));

		chart.resize(800, 300);
		chart.setMargin(new WLength(30), EnumSet.of(Side.Top, Side.Bottom));
		chart.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));
		chart.setStartAngle(90);

		chart.setModel(model);
		//chart.setLabelsColumn(CHART_DISPLAY_COLUMN);
		chart.setDataColumn(chartDataColumn);
		chart.setDisplayLabels(LabelOption.Outside, LabelOption.TextLabel);
		chart.setPlotAreaPadding(30);
		chart.setPalette(chartPalette);
	}

	public void initTable() {
		addWidget(table);
		table.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));
		table.setSortingEnabled(false);
		table.setWidth(new WLength(500));
		table.setStyleClass("blastResultsTable");
		//table.setHeaderHeight(new WLength(20));
		table.hideColumn(chartDisplayColumn);
		table.setModel(model);
		table.setTableWidth(true);

		table.setItemDelegateForColumn(colorColumn, new WAbstractItemDelegate() {
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

	}

	public StandardTableView getTable() {
		return table;
	}
}
