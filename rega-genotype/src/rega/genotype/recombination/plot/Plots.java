package rega.genotype.recombination.plot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.swing.JFrame;

import net.sf.regadb.csv.Table;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;

public class Plots extends JFrame {
	public Plots(String title) {
		super(title);
		FileReader f = null;
		try {
			f = new FileReader(new File("/home/plibin0/projects/utrecht/recombination/data.csv"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		CsvDataset n = new CsvDataset(new Table(f, false, '\t'));
		JFreeChart chart = ChartFactory.createTimeSeriesChart(
				"Bootscan Analyses", // chart title
				"nuleotides position", // x axis label
				"bootstrap values", // y axis label
				n, // data
				true, // include legend
				true, // tooltips
				false // urls
				);
		XYPlot plot = chart.getXYPlot();
		XYItemRenderer renderer = plot.getRenderer();

		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		setContentPane(chartPanel);
	}

	public static void main(String[] args) {
		Plots demo = new Plots("Bootscan Analyses");
		demo.pack();
		//RefineryUtilities.centerFrameOnScreen(demo);
		demo.setVisible(true);
	}
}
