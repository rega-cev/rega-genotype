package rega.genotype.ui.ngs;

import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.ngs.NgsProgress;
import rega.genotype.ngs.NgsProgress.BasketData;
import rega.genotype.taxonomy.TaxonomyModel;
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.PositionScheme;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.ViewItemRenderFlag;
import eu.webtoolkit.jwt.WAbstractItemDelegate;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WStandardItemModel;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTableView;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.chart.LabelOption;
import eu.webtoolkit.jwt.chart.WPieChart;

public class DiamondResultsView extends WContainerWidget{
	private static final int ASSINGMENT_COLUMN = 0;
	private static final int DATA_COLUMN = 1; // sequence count column. percentages of the chart.
	private static final int CHART_DISPLAY_COLUMN = 2;
	private static final int COLOR_COLUMN =         3;

	private WStandardItemModel blastResultModel = new WStandardItemModel();
	private WPieChart chart;
	private WTableView table;

	public DiamondResultsView(File workDir) {

		// create blastResultModel
		blastResultModel = new WStandardItemModel();
		blastResultModel.insertColumns(blastResultModel.getColumnCount(), 4);

		// chart
		chart = new WPieChart();
		addWidget(chart);
		setPositionScheme(PositionScheme.Relative);

		setMargin(new WLength(30), EnumSet.of(Side.Top, Side.Bottom));
		setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));

		chart.resize(500, 260);
		//chart.setMargin(new WLength(30), EnumSet.of(Side.Top, Side.Bottom));
		chart.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));

		chart.setModel(blastResultModel);
		//chart.setLabelsColumn(CHART_DISPLAY_COLUMN);    
		chart.setDataColumn(DATA_COLUMN);
		chart.setDisplayLabels(LabelOption.Outside, LabelOption.TextLabel);
		chart.setPlotAreaPadding(30);

		// table
		table = new WTableView(this);
		table.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));
		table.setWidth(new WLength(920));
		table.setHeight(new WLength(220));
		//table.setStyleClass("blastResultsTable");
		table.setHeaderHeight(new WLength(20));
		table.hideColumn(CHART_DISPLAY_COLUMN);
		table.setColumnWidth(ASSINGMENT_COLUMN, new WLength(740));
		table.setColumnWidth(DATA_COLUMN, new WLength(80));
		table.setColumnWidth(COLOR_COLUMN, new WLength(60));

		table.setItemDelegateForColumn(COLOR_COLUMN, new WAbstractItemDelegate() {
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

		table.setModel(blastResultModel);

		// fill model

		blastResultModel.setHeaderData(ASSINGMENT_COLUMN, new WString("Assignment"));
		blastResultModel.setHeaderData(DATA_COLUMN, "Sequence count");
		blastResultModel.setHeaderData(COLOR_COLUMN, "Legend");
		
		NgsProgress ngsProgress = NgsProgress.read(workDir);

		Map<String, BasketData> countDiamondResults = ngsProgress.getDiamondBlastResults();
		if (countDiamondResults == null)
			countDiamondResults = new HashMap<String, BasketData>();

		int i = 0;
		for (Map.Entry<String, BasketData> e: countDiamondResults.entrySet()) {
			BasketData basketData = e.getValue();
			int sequenceCount = basketData.getReadCountTotal();
			String taxonNmae = e.getKey();

			String hirarchy = TaxonomyModel.getInstance().getHirarchy(taxonNmae, 100);
			int row = blastResultModel.getRowCount();
			blastResultModel.insertRows(row, 1);
			blastResultModel.setData(row, ASSINGMENT_COLUMN,
					taxonNmae + ": " + hirarchy);
			blastResultModel.setData(row, DATA_COLUMN, sequenceCount); // percentage
			blastResultModel.setData(row, CHART_DISPLAY_COLUMN, 
					taxonNmae + "_" + basketData.getScientificName() + " (" + sequenceCount + ")");

			WColor color = chart.getPalette().getBrush(i).getColor();
			blastResultModel.setData(row, COLOR_COLUMN, color, 
					ItemDataRole.UserRole + 1);
			i++;
		}
	}
}
