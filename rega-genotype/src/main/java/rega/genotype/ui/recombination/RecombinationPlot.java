/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.recombination;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.utils.Table;

import com.pdfjet.Letter;
import com.pdfjet.PDF;
import com.pdfjet.Page;

import eu.webtoolkit.jwt.PenStyle;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WPaintDevice;
import eu.webtoolkit.jwt.WPainter;
import eu.webtoolkit.jwt.WPdfImage;
import eu.webtoolkit.jwt.WPen;
import eu.webtoolkit.jwt.WPointF;
import eu.webtoolkit.jwt.WRasterPaintDevice;
import eu.webtoolkit.jwt.chart.Axis;
import eu.webtoolkit.jwt.chart.AxisValue;
import eu.webtoolkit.jwt.chart.ChartType;
import eu.webtoolkit.jwt.chart.SeriesType;
import eu.webtoolkit.jwt.chart.WCartesianChart;
import eu.webtoolkit.jwt.chart.WDataSeries;

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
        	if (c == null)
        		c = genomeColors.get("other");

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
	
	private void paintCutoff(WPainter p) {
		//draw cutoff
		WPointF from = this.mapToDevice(0, cutoff);
		WPointF to = this.mapToDevice(this.getAxis(Axis.XAxis).getMaximum(), cutoff);
		WPen cutoffPen = new WPen(WColor.red);
		cutoffPen.setStyle(PenStyle.DashLine);
		p.setPen(cutoffPen);
		p.drawLine(from.getX(), from.getY(), to.getX(), to.getY());
	}
	
	protected void paintEvent(WPaintDevice paintDevice) {
		super.paintEvent(paintDevice);
		
		paintCutoff(paintDevice.getPainter());
	}
	
	public void streamRecombinationCSV(File jobDir, int sequenceIndex, String type, OutputStream os) throws IOException {
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
		bw.write(csvData);
		bw.flush();
		bw.close();
		os.flush();
	}
	
	public void streamRecombinationPDF(File jobDir, int sequenceIndex, String type, OutputStream os) throws Exception {
		PDF pdf = new PDF(os);
		Page page = new Page(pdf, Letter.PORTRAIT);
		WPdfImage image = new WPdfImage(pdf, page, 0, 0, new WLength(720), new WLength(450));
		WPainter painter = new WPainter(image);
		this.paint(painter);
		paintCutoff(painter);
		painter.end();
		pdf.flush();
		os.flush();
	}
	
	public void streamRecombinationPNG(File jobDir, int sequenceIndex, String type, OutputStream os) throws Exception {
		WRasterPaintDevice image = new WRasterPaintDevice("png", new WLength(720), new WLength(450));
		
		WPainter painter = new WPainter(image);
		this.paint(painter);
		paintCutoff(painter);
		painter.end();
		
		image.write(os);
		os.flush();
	}
}
