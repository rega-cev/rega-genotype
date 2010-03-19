package rega.genotype.ui.forms;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.data.OrganismDefinition;
import eu.webtoolkit.jwt.AlignmentFlag;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WStandardItem;
import eu.webtoolkit.jwt.WStandardItemModel;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.chart.Axis;
import eu.webtoolkit.jwt.chart.SeriesType;
import eu.webtoolkit.jwt.chart.WCartesianChart;
import eu.webtoolkit.jwt.chart.WDataSeries;

public class DefaultJobOverviewFilterSummary extends JobOverviewSummary {
	private BarView njTreeScanStats;
	private BarView bootscanStats;

	class Average {
		Double total;
		Double min, max;
	}
	
	class BarView {
		private WCartesianChart barChart;
		private WStandardItemModel model;
		private WTable table;
		
		int nrSequences;
		private Map<String, Average> averageMap = new HashMap<String, Average>();
		
		public BarView(WContainerWidget barParent, WContainerWidget tableParent, CharSequence chartTitle, String axisFormat) {
			barChart = new WCartesianChart(barParent);
			table = new WTable(tableParent);
			table.setStyleClass("assignment-overview-filter");
			model = new WStandardItemModel(); 
			barChart.setModel(model);

			barChart.resize(400, 200);

			barChart.setXSeriesColumn(0);
			barChart.setLegendEnabled(true);

			barChart.setPlotAreaPadding(50, Side.Left);
			barChart.setPlotAreaPadding(250, Side.Right);
			barChart.setPlotAreaPadding(50, Side.Top, Side.Bottom);
			barChart.getAxis(Axis.YAxis).setLabelFormat(axisFormat);

			barChart.setTitle(chartTitle);

			barParent.setContentAlignment(AlignmentFlag.AlignRight);
			barParent.setVerticalAlignment(AlignmentFlag.AlignMiddle);
			tableParent.setContentAlignment(AlignmentFlag.AlignLeft);
			tableParent.setVerticalAlignment(AlignmentFlag.AlignMiddle);
		}
		
		public void setHeaders(String filter, WString h1, WString h2, WString h3) {			
			textToTable(0, 0, filter);
			textToTable(1, 0, tr("detailsForm.summary.filter.nrSequences"));
			textToTable(2, 0, h1);
			textToTable(3, 0, h2);
			textToTable(4, 0, h3);
			
			model.insertColumns(0, 4);
			model.setHeaderData(1, h1);
			model.setHeaderData(2, h2);
			model.setHeaderData(3, h3);
			
			averageMap.put(h1.getKey(), new Average());
			averageMap.put(h2.getKey(), new Average());
			averageMap.put(h3.getKey(), new Average());
			
			for (int i = 1; i < model.getColumnCount(); ++i) {
	            WDataSeries s = new WDataSeries(i, SeriesType.BarSeries);
	            barChart.addSeries(s);
	        }
		}
		
		public void setData(String header, String valueS) {
			if(valueS == null)
				return;
			
			double value = Double.parseDouble(valueS);
			
			Average average = averageMap.get(header);
			if (average.total == null) 
				average.total = 0.0;
			average.total += value;
			
			if (average.max == null || value > average.max) 
				average.max = value;
			
			if (average.min == null || value < average.min) 
				average.min = value;
		}
		
		public void updateData(String assignment) {
			table.getElementAt(1, 1).clear();
			table.getElementAt(1, 1).addWidget(new WText(nrSequences + ""));
			
			if (model.getRowCount() < 1)
				model.insertRow(0);

			model.setData(0, 0, "");

			DecimalFormat formatter = new DecimalFormat("0.00");
			for (int i = 0; i < 3; i++) {
				WString header = ((WText)table.getElementAt(i + 2, 0).getChildren().get(0)).getText();
				Average average = averageMap.get(header.getKey());
				table.getElementAt(i + 2, 1).clear();
				table.getElementAt(i + 2, 1).addWidget(new WText(
						average.total == null ? "n/a" : formatter.format(average.total/nrSequences)));
				table.getElementAt(i + 2, 2).clear();
				table.getElementAt(i + 2, 2).addWidget(new WText(average.min == null ? "n/a" :
						"(" + formatter.format(average.min)	+ " - " + formatter.format(average.max) + ")"));

				model.setItem(0, i + 1, new WStandardItem(
						average.total == null ? "n/a" : formatter.format(average.total/nrSequences)));
			}
		}
		
		public void textToTable(int row, int column, CharSequence value) {
			table.getElementAt(row, column).clear();
			table.getElementAt(row, column).addWidget(new WText(value));
		}
		
		public void setHidden(boolean hidden) {
			table.setHidden(hidden);
			barChart.setHidden(hidden);
		}
	}
	
	private void init(String filter) {
		this.setStyleClass("jobOverviewSummary");
		
		new WText(tr("detailsForm.summary.filter.title"), this.getElementAt(0, 0));
		this.getElementAt(0, 0).setColumnSpan(2);
		this.getElementAt(0, 0).setContentAlignment(AlignmentFlag.AlignCenter);
		
		njTreeScanStats = new BarView(this.getElementAt(1, 0), 
				this.getElementAt(1, 1), 
				tr("detailsForm.summary.filter.njTreeStats"),
				"%.0f");
		njTreeScanStats.setHeaders(filter, tr("detailsForm.summary.filter.njTreeStats.avgBootstrap"), 
				tr("detailsForm.summary.filter.njTreeStats.avgBootstrapInside"), 
				tr("detailsForm.summary.filter.njTreeStats.avgBootstrapOutside"));
		
		bootscanStats = new BarView(this.getElementAt(2, 0),
				this.getElementAt(2, 1),
				tr("detailsForm.summary.filter.bootscanStats"),
				"%.2f");
		bootscanStats.setHeaders(filter, tr("detailsForm.summary.filter.njTreeStats.avgBootscan"), 
				tr("detailsForm.summary.filter.njTreeStats.avgBootscanSupport"), 
				tr("detailsForm.summary.filter.njTreeStats.avgBootscanNoSupport"));
	}

	public void reset() {
		this.clear();
		njTreeScanStats = null;
	}

	public void update(GenotypeResultParser p, OrganismDefinition od) {
		String assignment = p.getEscapedValue("/genotype_result/sequence/conclusion/assigned/name");
		
		if (njTreeScanStats == null)
			init(assignment);

		String id = od.getProfileScanType(p);
		
		if (!p.elementExists("/genotype_result/sequence/result[@id='" + id + "']"))
			id += "-puzzle";
		
		if (p.elementExists("/genotype_result/sequence/result[@id='" + id + "']")) {
			njTreeScanStats.nrSequences++;
			
			njTreeScanStats.setData("detailsForm.summary.filter.njTreeStats.avgBootstrap", 
					p.getEscapedValue("/genotype_result/sequence/result[@id='" + id + "']/best/support"));
			njTreeScanStats.setData("detailsForm.summary.filter.njTreeStats.avgBootstrapInside",
					p.getEscapedValue("/genotype_result/sequence/result[@id='" + id + "']/best/inner"));
			njTreeScanStats.setData("detailsForm.summary.filter.njTreeStats.avgBootstrapOutside",
					p.getEscapedValue("/genotype_result/sequence/result[@id='" + id + "']/best/outer"));
			
			njTreeScanStats.updateData(assignment);
		}
		
		if (p.elementExists("/genotype_result/sequence/result[@id='scan-" + id + "']")) {
			bootscanStats.nrSequences++;
			
			bootscanStats.setHidden(false);
			bootscanStats.setData("detailsForm.summary.filter.njTreeStats.avgBootscan", 
					p.getEscapedValue("/genotype_result/sequence/result[@id='scan-" + id + "']/support[@id='best']"));
			bootscanStats.setData("detailsForm.summary.filter.njTreeStats.avgBootscanSupport",
					p.getEscapedValue("/genotype_result/sequence/result[@id='scan-" + id + "']/support[@id='assigned']"));
			bootscanStats.setData("detailsForm.summary.filter.njTreeStats.avgBootscanNoSupport",
					p.getEscapedValue("/genotype_result/sequence/result[@id='scan-" + id + "']/nosupport[@id='assigned']"));
			
			bootscanStats.updateData(assignment);
		} else {
			bootscanStats.setHidden(true);
		}
	}

	@Override
	public Side getLocation() {
		return Side.Bottom;
	}
}