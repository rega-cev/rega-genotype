/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.recombination;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.ui.util.ImageConverter;
import rega.genotype.utils.Table;
import eu.webtoolkit.jwt.PenStyle;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WPaintDevice;
import eu.webtoolkit.jwt.WPainter;
import eu.webtoolkit.jwt.WPen;
import eu.webtoolkit.jwt.WPointF;
import eu.webtoolkit.jwt.WSvgImage;
import eu.webtoolkit.jwt.chart.Axis;
import eu.webtoolkit.jwt.chart.AxisValue;
import eu.webtoolkit.jwt.chart.ChartType;
import eu.webtoolkit.jwt.chart.SeriesType;
import eu.webtoolkit.jwt.chart.WCartesianChart;
import eu.webtoolkit.jwt.chart.WDataSeries;
import eu.webtoolkit.jwt.servlet.UploadedFile;

/**
 * Recombination plot.
 * 
 * @author pieter
 */
public class RecombinationPlot extends WCartesianChart {
	private String csvData;
	private OrganismDefinition od;
	private int cutoff;
	
	public RecombinationPlot(String csvData, OrganismDefinition od) {
		this.csvData = csvData;
		this.od = od;
		this.cutoff = 70;
		
		CsvModel model = new CsvModel(new Table(new ByteArrayInputStream(csvData.getBytes()), false, '\t'));
        this.setModel(model);
        this.setXSeriesColumn(0);
        this.setLegendEnabled(true);

        this.setType(ChartType.ScatterPlot);

        this.getAxis(Axis.XAxis).setLocation(AxisValue.ZeroValue);
        this.getAxis(Axis.YAxis).setLocation(AxisValue.ZeroValue);
        this.getAxis(Axis.XAxis).setLabelFormat("%.0f");
        this.getAxis(Axis.YAxis).setLabelFormat("%.0f");

        this.setPlotAreaPadding(60, Side.Left);
        this.setPlotAreaPadding(80, Side.Right);
        this.setPlotAreaPadding(50, Side.Top, Side.Bottom);

        this.getAxis(Axis.XAxis).setTitle("Nucleotide position");
        this.getAxis(Axis.YAxis).setTitle("Support");
        
        this.setTitle("Bootscan analysis");

        Map<String, Color> genomeColors = od.getGenome().getAttributes().getColors();
        for (int i = 1; i < model.getColumnCount(); i++) {
        	Color c = genomeColors.get(model.getHeaderData(i));
        	if (c == null)
        		c = genomeColors.get("CRF");

        	WDataSeries ds = new WDataSeries(i, SeriesType.LineSeries);
        	if (c != null) {
        		WPen pen = new WPen(new WColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()));
        		pen.setWidth(new WLength(3));
        		ds.setPen(pen);
        	}
    		this.addSeries(ds);
        }

        this.resize(720, 450);
	}
	
	protected void paintEvent(WPaintDevice paintDevice) {
		super.paintEvent(paintDevice);
		
		//draw cutoff
		WPointF from = this.mapToDevice(0, cutoff);
		WPointF to = this.mapToDevice(this.getAxis(Axis.XAxis).getMaximum(), cutoff);
		WPen cutoffPen = new WPen(WColor.red);
		cutoffPen.setStyle(PenStyle.DashLine);
		paintDevice.getPainter().setPen(cutoffPen);
		paintDevice.drawLine(from.getX(), from.getY(), to.getX(), to.getY());
	}
	
	public File getRecombinationCSV(File jobDir, int sequenceIndex, String type) {
		File csvFile = new File(jobDir.getAbsolutePath() + File.separatorChar + "plot_" + sequenceIndex + "_" + type + ".csv");
		if(!csvFile.exists()) {
			try {
				GenotypeLib.writeStringToFile(csvFile, csvData);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return csvFile;
	}
	
	public File getRecombinationPDF(File jobDir, int sequenceIndex, String type) throws IOException {
		File pdfFile = new File(jobDir.getAbsolutePath() + File.separatorChar + "plot_" + sequenceIndex + "_" + type + ".pdf");
		if (!pdfFile.exists()) {
			WSvgImage image = new WSvgImage(new WLength(720), new WLength(450));
			WPainter painter = new WPainter(image);
			this.paint(painter);
			painter.end();
			File tmp = File.createTempFile("recombination", "svg");
			FileOutputStream fos = new FileOutputStream(tmp);
			image.write(fos);
			fos.flush();
			fos.close();
			ImageConverter.svgToPdf(tmp, pdfFile);
			tmp.delete();
		}
		return pdfFile;
	}
}
