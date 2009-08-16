/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.recombination;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.GenotypeLib;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Constructs recombination plots using the jfreechart api.
 * 
 * @author simbre1
 *
 */
public class RecombinationPlot {
	public static JFreeChart getRecombinationPlot(String dataCsv, OrganismDefinition od) throws FileNotFoundException, UnsupportedEncodingException {
		CsvDataset n = new CsvDataset(new Table(new ByteArrayInputStream(dataCsv.getBytes()), false, '\t'));
		JFreeChart chart = ChartFactory.createXYLineChart("Bootscan Analyses", 																			
				"nuleotides position", 
				"bootstrap values", 
				n, 
				PlotOrientation.VERTICAL, 
				true, 
				true, 
				false 
				);
		
		XYPlot plot = chart.getXYPlot();
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(false);
		
		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setTickUnit(new NumberTickUnit(20), true, true);
		
		NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
		xAxis.setTickUnit(new NumberTickUnit(200), true, true);
		xAxis.setRange(n.getXValue(0, 0), n.getXValue(0, n.getItemCount(0)-1));
		
		chart.getLegend().setPosition(RectangleEdge.RIGHT);
		
		//colors curves
		Map<String, Color> genomeColors = od.getGenome().COLORS();
		for(int i=0; i<n.getSeriesCount();i++) {
			Color c = genomeColors.get(n.getSeriesKey(i));
			if(c!=null) {
				plot.getRenderer().setSeriesPaint(i, c);
			} else {
				System.err.println("Error: cannot find genome color: " + n.getSeriesKey(i));
			}
		}
		//colors curves
		
		//cutoff
		XYSeriesCollection cutoffCollection = new XYSeriesCollection();
		XYSeries cutoffSeries = new XYSeries("70% cutoff");
		cutoffSeries.add(n.getXValue(0, 0), 70.0);
		cutoffSeries.add(n.getXValue(0, n.getItemCount(0)-1), 70.0);
		cutoffCollection.addSeries(cutoffSeries);
		plot.setDataset(1, cutoffCollection);
		plot.setRenderer(1, new XYLineAndShapeRenderer(true, false));
		plot.getRenderer(1).setBaseStroke(new BasicStroke(2.0f,
	                       BasicStroke.CAP_ROUND,
	                       BasicStroke.JOIN_ROUND,
	                       1.0f,
	                       new float[] {10.0f, 6.0f},
	                       0.0f));
		plot.getRenderer(1).setSeriesPaint(0, Color.RED);
		//cutoff

		return chart;
	}
	
	public static File getRecombinationPNG(File jobDir, int sequenceIndex, String type, String csvData, OrganismDefinition od) throws UnsupportedEncodingException, IOException {
		File pngFile = new File(jobDir.getAbsolutePath() + File.separatorChar + "plot_" + sequenceIndex + "_" + type + ".png");
		if(!pngFile.exists()) {
			FileOutputStream fos = new FileOutputStream(pngFile);
			ChartUtilities.writeChartAsPNG(fos, getRecombinationPlot(csvData, od), 720, 450);
			fos.flush();
			fos.close();
		}
		return pngFile;
	}
	
	public static File getRecombinationCSV(File jobDir, int sequenceIndex, String type, String csvData) {
		File pdfFile = new File(jobDir.getAbsolutePath() + File.separatorChar + "plot_" + sequenceIndex + "_" + type + ".csv");
		if(!pdfFile.exists()) {
			try {
				GenotypeLib.writeStringToFile(pdfFile, csvData);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return pdfFile;
	}
	
	public static File getRecombinationPDF(File jobDir, int sequenceIndex, String type, String csvData, OrganismDefinition od) throws IOException {
		File pdfFile = new File(jobDir.getAbsolutePath() + File.separatorChar + "plot_" + sequenceIndex + "_" + type + ".pdf");
		
		if(!pdfFile.exists()) {
			FileOutputStream fos = new FileOutputStream(pdfFile);
			int width = 720;
			int height = 450;
			
			Rectangle pagesize = new Rectangle(width, height);
			Document document = new Document(pagesize, 50, 50, 50, 50);
			try {
				PdfWriter writer = PdfWriter.getInstance(document, fos);
				document.addAuthor("Rega Genotype Tool");
				document.addSubject("Recombination plot");
				document.open();
				PdfContentByte cb = writer.getDirectContent();
				PdfTemplate tp = cb.createTemplate(width, height);
				Graphics2D g2 = tp.createGraphics(width, height, new DefaultFontMapper());
				Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
				getRecombinationPlot(csvData, od).draw(g2, r2D, null);
				g2.dispose();
				cb.addTemplate(tp, 0, 0);
			} catch (DocumentException de) {
				System.err.println(de.getMessage());
			}
			document.close();
		}
		
		return pdfFile;
	}

	
	public static void main(String[] args) throws IOException {
//		JFrame f = new JFrame();
//		ChartPanel chartPanel = new ChartPanel(getRecombinationPlot(new File("/home/plibin0/projects/utrecht/recombination/data.csv")));
//		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
//		f.setContentPane(chartPanel);
//		f.setVisible(true);
//		
//		getRecombinationPNG(new File("/home/plibin0/projects/subtypetool/genomePng"), 0, "pure");
//		getRecombinationPDF(new File("/home/plibin0/projects/subtypetool/genomePng"), 0, "pure");
	}
}
