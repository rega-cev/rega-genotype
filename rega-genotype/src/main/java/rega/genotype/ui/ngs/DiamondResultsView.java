package rega.genotype.ui.ngs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;

import rega.genotype.ngs.NgsAnalysis;

import eu.webtoolkit.jwt.PositionScheme;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WStandardItemModel;
import eu.webtoolkit.jwt.chart.LabelOption;
import eu.webtoolkit.jwt.chart.WPieChart;

public class DiamondResultsView extends WContainerWidget{
	private static final int ASSINGMENT_COLUMN = 0;
	private static final int DATA_COLUMN = 1; // sequence count column. percentages of the chart.
	private static final int CHART_DISPLAY_COLUMN = 2;
	
	private WStandardItemModel blastResultModel = new WStandardItemModel();
	private WPieChart chart;
	private File workDir;

	public DiamondResultsView(File workDir) {
		this.workDir = workDir;

		// create blastResultModel
		blastResultModel = new WStandardItemModel();
		blastResultModel.insertColumns(blastResultModel.getColumnCount(), 3);
		blastResultModel.setHeaderData(ASSINGMENT_COLUMN, "Assignment");
		blastResultModel.setHeaderData(DATA_COLUMN, "Sequence count");
		
		fillResults();
		createChart();
	}

	public void fillResults() {

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

		//chart.setModel(blastResultModel);
	}

	// TODO: pre save that
	private int sequernceCount(File fasqFile) {
		BufferedReader reader;
		int lines = 0;
		try {
			reader = new BufferedReader(new FileReader(fasqFile));
			while (reader.readLine() != null) 
				lines++;
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lines / 4;
	}

	private void createChart() {
		chart = new WPieChart();
		addWidget(chart);
		setPositionScheme(PositionScheme.Relative);

		resize(800, 300);
		setMargin(new WLength(30), EnumSet.of(Side.Top, Side.Bottom));
		setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));

		chart.resize(800, 300);
		chart.setMargin(new WLength(30), EnumSet.of(Side.Top, Side.Bottom));
		chart.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));

		chart.setModel(blastResultModel);
		chart.setLabelsColumn(CHART_DISPLAY_COLUMN);    
		chart.setDataColumn(DATA_COLUMN);
		chart.setDisplayLabels(LabelOption.Outside, LabelOption.TextLabel);
		chart.setPlotAreaPadding(30);
	}
}
