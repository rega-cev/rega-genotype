package rega.genotype.ui.forms;

import java.util.HashMap;
import java.util.Map;

import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.data.OrganismDefinition;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WStandardItemModel;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.chart.WCartesianChart;

public class DefaultJobOverviewFilterSummary extends WContainerWidget implements JobOverviewSummary {
	class Average {
		Double total;
		Double min, max;
	}
	
	class BarView {
		private WCartesianChart barChart;
		private WStandardItemModel model;
		private WTable table;
		
		int nrSequences;
		private Map<CharSequence, Average> averageMap = new HashMap<CharSequence, Average>();
		
		public BarView(WContainerWidget barParent, WContainerWidget tableParent, String filter, String chartTitle) {
			barChart = new WCartesianChart(barParent);
			table = new WTable(tableParent);
			model = new WStandardItemModel(); 
			barChart.setModel(model);
		}
		
		public void setHeaders(String filter, WString h1, WString h2, WString h3) {			
			textToTable(0, 0, tr("detailsForm.summary.filter.nrSequences"));
			textToTable(1, 0, filter);
			textToTable(2, 0, h1);
			textToTable(3, 0, h2);
			textToTable(4, 0, h3);
			
			addRowToModel(h1);
			addRowToModel(h2);
			addRowToModel(h3);
			
			averageMap.put(h1, new Average());
			averageMap.put(h2, new Average());
			averageMap.put(h2, new Average());
		}
		
		public void setData(WString header, double value) {
			Average average = averageMap.get(header);
			if (average.total == null) 
				average.total = 0.0;
			average.total += value;
			
			if (average.max == null || value > average.max) 
				average.max = value;
			
			if (average.min == null || value < average.min) 
				average.min = value;
		}
		
		public void updateData() {
			table.getElementAt(1, 1).clear();
			table.getElementAt(1, 1).addWidget(new WText(nrSequences+""));
			for (int i = 0; i < 3; i++) {
				WString header = ((WText)table.getElementAt(i + 2, 0).getChildren().get(0)).getText();
				Average average = averageMap.get(header);
				table.getElementAt(i + 2, 1).clear();
				table.getElementAt(i + 2, 1).addWidget(new WText(average.total/nrSequences+""));
				table.getElementAt(i + 3, 1).clear();
				table.getElementAt(i + 3, 1).addWidget(new WText(average.min + " - " + average.max));
				
				model.setData(i, 0, average.total/nrSequences);
			}
		}
		
		public void addRowToModel(CharSequence value) {
			int insertPosition = model.getRowCount();
			model.insertRow(insertPosition);
			model.setHeaderData(insertPosition, value);
		}
		
		public void textToTable(int row, int column, CharSequence value) {
			table.getElementAt(row, column).clear();
			table.getElementAt(row, column).addWidget(new WText(value));
		}
	}
	
	public WContainerWidget getWidget() {
		return this;
	}

	public void reset() {
		
	}

	public void update(GenotypeResultParser parser, OrganismDefinition od) {
		
	}
}