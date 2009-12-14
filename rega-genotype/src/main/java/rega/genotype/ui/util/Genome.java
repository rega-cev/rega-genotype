/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import rega.genotype.ui.forms.RecombinationForm.Region;
import rega.genotype.utils.Table;

/**
 * Abstract class, provides basis for drawing genome map images.
 * 
 * @author simbre1
 *
 */
public class Genome {
	private GenomeAttributes attributes;
	
	public Genome(GenomeAttributes attributes){
		this.attributes = attributes;
	}
	
	public GenomeAttributes getAttributes(){
		return attributes;
	}
	
	public int imgX(int pos) {
		return (int)(attributes.getGenomeImageStartX() + ((double)pos - attributes.getGenomeStart())
		         /((double)attributes.getGenomeEnd() - attributes.getGenomeStart())
		         *((double)attributes.getGenomeImageEndX() - attributes.getGenomeImageStartX()));
	}
	
	public File getGenomePNG(File jobDir, int sequenceIndex, String genotype, int start, int end, int variant, String type, String csvData, List<Region> regions) throws IOException{
		File pngFile = new File(jobDir.getAbsolutePath() 
				+ File.separatorChar + "genome"
				+ "_" + sequenceIndex 
				+ "_" + type 
				+ "_" + (regions == null ? variant : "large") 
				+".png");
	
		if (!pngFile.exists()) {
			BufferedImage image = drawImage(genotype,start,end,variant,csvData,regions);
		    ImageIO.write(image, "png", pngFile);
		}
		
		return pngFile;
	}
	
	protected BufferedImage drawImage(String genotype, int start, int end, int variant, String csvData, List<Region> regions) throws IOException{
		int w[];
		String assign[];
		int scanWindowSize;
		int scanStepSize;
		if(csvData!=null) {
			Table csvTable = new Table(new ByteArrayInputStream(csvData.getBytes()), false, '\t');
		    
			w = new int[csvTable.numRows()-1];
		    assign = new String[csvTable.numRows()-1];
		    
		    for(int i = 1; i<csvTable.numRows(); i++) {
		    	w[i-1] = Integer.parseInt(csvTable.valueAt(0, i));
		    	assign[i-1] = csvTable.valueAt(csvTable.numColumns() -1 -variant, i);
		    }
		    
		    scanWindowSize = w[0]*2;
		    scanStepSize = w[1] - w[0];
		} else {
			w = new int[0];
			assign = new String[0];
			scanWindowSize = 0;
			scanStepSize = 0;
		}

	    Image genomePng = ImageIO.read(
	    		this.getClass().getResourceAsStream(
	    				attributes.getOrganismDefinition().getOrganismDirectory()
	    				+"/genome_"+ (regions == null ? variant : "large") +".png"));

		int imgWidth = genomePng.getWidth(null);
	    int imgHeight = genomePng.getHeight(null);
	    
	    BufferedImage image = new BufferedImage(imgWidth,imgHeight,BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g2d = (Graphics2D)image.getGraphics();
	    
	    //gray background
	    g2d.setColor(new Color(230, 230, 230));
	    g2d.fillRect(0, 0, imgWidth, imgHeight);
	    
	    Map<String, Color> colorMap = attributes.getColors();
	    
	    Color bgcolor = colorMap.get("-");

	    if (w.length == 0) {
	      bgcolor = colorMap.get(genotype);
	    }
	    
	    g2d.setColor(bgcolor);
	    g2d.fillRect(imgX(start), 0, imgX(end)-imgX(start), imgHeight);
	    
	    int x1, x2;
	    for (int c=0; c < w.length; c++) {
	        if (c == 0)
	        	x1 = start + w[c] - scanWindowSize/2;
	        else
	        	x1 = start + w[c] - scanStepSize/2;

	        if (c == w.length-1)
	        	x2 = start + w[c] + scanWindowSize/2;
	        else
	        	x2 = start + w[c] + scanStepSize/2;

	        Color color = colorMap.get(assign[c]);
	        if (color == null)
	        	color = colorMap.get("CRF");
	        g2d.setColor(color);
	        g2d.fillRect(imgX(x1), 0, imgX(x2)-imgX(x1), imgHeight);
	    }
	    
	    g2d.drawImage(genomePng, 0, 0, imgWidth, imgHeight, null);
	    
	    if(regions != null){
	    	g2d.setColor(Color.BLACK);
	    	g2d.setStroke(new BasicStroke(1,
	    			BasicStroke.CAP_BUTT,
	    			BasicStroke.JOIN_BEVEL,
	    			0,
	    			new float[] {5},
	    			0));

	    	g2d.setFont(new Font("sans serif", Font.BOLD, attributes.getFontSize()));
	    	g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	    	for (Region region : regions) {
	    		int xstart = imgX(region.start);
    			g2d.drawLine(xstart, attributes.getGenomeImageStartY(), xstart, attributes.getGenomeImageEndY());
	    		int xend = imgX(region.end);
    			g2d.drawLine(xend, attributes.getGenomeImageStartY(), xend, attributes.getGenomeImageEndY());
	    			
    			drawCenteredString(g2d,"( " + (regions.indexOf(region) + 1) + " )", (xstart+xend) / 2,attributes.getGenomeImageEndY());
    		}	    	
	    }
	    return image;
	}
	
	protected void drawCenteredString(Graphics2D g2d, String str, int x, int y){
		g2d.drawString(str,
				(int)(x - g2d.getFontMetrics().getStringBounds(str, g2d).getWidth()/2),
				y);
	}
	
	public File getSmallGenomePNG(File jobDir, int sequenceIndex, String genotype, int start, int end, int variant, String type, String csvData) throws IOException {
		  File smallPngFile = new File(jobDir.getAbsolutePath() + File.separatorChar + "genomesmall_" + sequenceIndex + "_" + type + "_" + variant + ".png");

		  if (!smallPngFile.exists()) {
		    File pngFile = getGenomePNG(jobDir, sequenceIndex, genotype, start, end, variant, type, csvData);
		    GenotypeLib.scalePNG(pngFile, smallPngFile, 40.0);
		  }

		  return smallPngFile;
	}

	public File getGenomePNG(File jobDir, int sequenceIndex, String genotype, int start, int end, int variant, String type, String csvData) throws IOException {
		return getGenomePNG(jobDir, sequenceIndex, genotype, start, end, variant, type, csvData, null);
	}
}
