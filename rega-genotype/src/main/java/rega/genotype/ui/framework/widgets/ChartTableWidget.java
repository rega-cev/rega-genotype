package rega.genotype.ui.framework.widgets;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import eu.webtoolkit.jwt.AlignmentFlag;
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.PositionScheme;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.WAbstractItemModel;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WPainter;
import eu.webtoolkit.jwt.WRectF;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WText;
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
	protected WTable table = new WTable();
	private int chartDataColumn;
	private int colorColumn;
	protected WAbstractItemModel model;
	private WChartPalette chartPalette;
	private List<Integer> skipColumn = new ArrayList<Integer>(); // no good way to hide a column in html table

	public ChartTableWidget(WAbstractItemModel model, int chartDataColumn, 
			int colorColumn, WChartPalette chartPalette) {
		this.model = model;
		this.chartDataColumn = chartDataColumn;
		this.colorColumn = colorColumn;
		this.chartPalette = chartPalette;
	}

	public void init() {
		if (model.getRowCount() > 1)
			createChart();
		else
			skipColumn.add(colorColumn);
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
		chart.setDataColumn(chartDataColumn);
		chart.setDisplayLabels(LabelOption.Outside, LabelOption.TextLabel);
		chart.setPlotAreaPadding(30);
		chart.setPalette(chartPalette);
	}

	private String formatDouble(Double d) {
		DecimalFormat df = new DecimalFormat("#.#"); 
		return df.format(d);
	}

	private String getText(Object o) {
		String text = null;
		if (o != null) {
			if (o instanceof Double)
				text = formatDouble((Double) o);
			else
				text = o.toString();
		}

		return text;
	}

	protected void addText(int row, int column, Object o) {
		String text = getText(o);
		if (text != null)
			table.getElementAt(row, column).addWidget(
					new WText(text));
	}

	protected int tableCol(int column) {
		int ans = column;
		for (Integer c: skipColumn)
			if (c < column)
				ans--;

		return ans;
	}

	public void initTable() {
		addWidget(table);
		table.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));
		table.setHeaderCount(1);
		table.setStyleClass("jobTable");
		for (int c = 0; c < model.getColumnCount(); c++) {
			if (!skipColumn.contains(c)) {
				addText(0, tableCol(c), model.getHeaderData(c));
				table.getElementAt(0, tableCol(c)).setStyleClass("jobTableHeader");			
			}
		}

		for (int r = 0; r < model.getRowCount(); r++) {
			for (int c = 0; c < model.getColumnCount(); c++) {
				if (!skipColumn.contains(c))
					addWidget(r,c);
			}
		}
	}

	protected void addWidget(final int row, final int column) {
		if (column == colorColumn) {
			WContainerWidget w = new WContainerWidget();
			w.setStyleClass("legend-item");
			WColor color = (WColor)model.getData(row,column,ItemDataRole.UserRole + 1);
			if (color != null)
				w.getDecorationStyle().setBackgroundColor(color);

			w.setMargin(WLength.Auto, Side.Left, Side.Right);
			w.setMargin(15, Side.Top);
			w.resize(30, 30);
			table.getElementAt(row + 1, tableCol(column)).addWidget(w);
		} else if (model.getData(row, column, ItemDataRole.LinkRole) != null) {
			// add link
			WAnchor a = new WAnchor((WLink) model.getData(row, column, ItemDataRole.LinkRole));
			String text = getText(model.getData(row, column));
			if (text != null)
				a.setText(text);
			table.getElementAt(row + 1, tableCol(column)).addWidget(a);
		} else
			addText(row + 1, tableCol(column), model.getData(row, column));
	}

	public void addTotalsRow(int[] columns) {
		int row = table.getRowCount();
		addText(row, 0, "Totals");
		for (int c: columns) {
			double total = 0.0;
			for (int r = 0; r < model.getRowCount(); ++r) {
				Object data = model.getData(r, c);
				if (data != null && data instanceof Double)
					total += (Double)data;
				else if(data != null && data instanceof Integer)
					total += (Integer)data;
			}
			addText(row, c, total);
		}
	}
}
