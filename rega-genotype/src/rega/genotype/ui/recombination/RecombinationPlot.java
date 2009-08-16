package rega.genotype.ui.recombination;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;

import rega.genotype.ui.util.GenotypeLib;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

public class RecombinationPlot {
	public static JFreeChart getRecombinationPlot(File dataCsv) throws FileNotFoundException, UnsupportedEncodingException {
		CsvDataset n = new CsvDataset(Table.readTable(dataCsv, '\t'));
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
		
		return chart;
	}
	
	public static File getRecombinationPNG(File jobDir, int sequenceIndex, String type) throws UnsupportedEncodingException, IOException {
		File pngFile = new File(jobDir.getAbsolutePath() + File.separatorChar + "plot_" + sequenceIndex + "_" + type + ".png");
		if(!pngFile.exists()) {
			File csvFile = GenotypeLib.getCsvData(jobDir, sequenceIndex, type);
			FileOutputStream fos = new FileOutputStream(pngFile);
			ChartUtilities.writeChartAsPNG(fos, getRecombinationPlot(csvFile), 720, 450);
			fos.flush();
			fos.close();
		}
		return pngFile;
	}
	
	public static File getRecombinationPDF(File jobDir, int sequenceIndex, String type) throws IOException {
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
				File csvFile = GenotypeLib.getCsvData(jobDir, sequenceIndex, type);
				getRecombinationPlot(csvFile).draw(g2, r2D, null);
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
		JFrame f = new JFrame();
		ChartPanel chartPanel = new ChartPanel(getRecombinationPlot(new File("/home/plibin0/projects/utrecht/recombination/data.csv")));
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		f.setContentPane(chartPanel);
		f.setVisible(true);
		
		getRecombinationPNG(new File("/home/plibin0/projects/subtypetool/genomePng"), 0, "pure");
		getRecombinationPDF(new File("/home/plibin0/projects/subtypetool/genomePng"), 0, "pure");
	}
}
