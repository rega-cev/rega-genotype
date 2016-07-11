package rega.genotype.ui.ngs;

import java.io.File;
import java.util.EnumSet;
import java.util.Map;

import rega.genotype.ngs.NgsAnalysis;
import eu.webtoolkit.jwt.PositionScheme;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WStandardItemModel;
import eu.webtoolkit.jwt.WTableView;
import eu.webtoolkit.jwt.chart.LabelOption;
import eu.webtoolkit.jwt.chart.WPieChart;

public class DiamondResultsView extends WContainerWidget{
	private static final int ASSINGMENT_COLUMN = 0;
	private static final int DATA_COLUMN = 1; // sequence count column. percentages of the chart.
	private static final int CHART_DISPLAY_COLUMN = 2;

	private WStandardItemModel blastResultModel = new WStandardItemModel();
	private WPieChart chart;
	private WTableView table;
	private File workDir;

	public DiamondResultsView(File workDir) {
		this.workDir = workDir;

		// create blastResultModel
		blastResultModel = new WStandardItemModel();
		blastResultModel.insertColumns(blastResultModel.getColumnCount(), 3);
		blastResultModel.setHeaderData(ASSINGMENT_COLUMN, "Assignment");
		blastResultModel.setHeaderData(DATA_COLUMN, "Sequence count");

		Map<String, Integer> countDiamondREsults = NgsAnalysis.countDiamondREsults(workDir);

		for (Map.Entry<String, Integer> e: countDiamondREsults.entrySet()) {
			int sequenceCount = e.getValue();
			String taxonNmae = e.getKey();

			int row = blastResultModel.getRowCount();
			blastResultModel.insertRows(row, 1);
			blastResultModel.setData(row, ASSINGMENT_COLUMN, taxonNmae);
			blastResultModel.setData(row, DATA_COLUMN, sequenceCount); // percentage
			blastResultModel.setData(row, CHART_DISPLAY_COLUMN, 
					taxonNmae + " (" + sequenceCount + ")");
		}

		// chart
		chart = new WPieChart();
		addWidget(chart);
		setPositionScheme(PositionScheme.Relative);

		setMargin(new WLength(30), EnumSet.of(Side.Top, Side.Bottom));
		setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));

		chart.resize(800, 300);
		//chart.setMargin(new WLength(30), EnumSet.of(Side.Top, Side.Bottom));
		chart.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));

		chart.setModel(blastResultModel);
		chart.setLabelsColumn(CHART_DISPLAY_COLUMN);    
		chart.setDataColumn(DATA_COLUMN);
		chart.setDisplayLabels(LabelOption.Outside, LabelOption.TextLabel);
		chart.setPlotAreaPadding(30);

		// table
		table = new WTableView(this);
		table.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));
		table.setWidth(new WLength(500));
		table.setHeight(new WLength(200));
		table.setStyleClass("blastResultsTable");
		table.hideColumn(CHART_DISPLAY_COLUMN);
		table.setColumnWidth(0, new WLength(340));
		table.setColumnWidth(1, new WLength(60));
		table.setModel(blastResultModel);
	}
}
