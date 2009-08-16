/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.utils.Table;

/**
 * Abstract class, provides basis for drawing genome map images.
 * 
 * @author simbre1
 *
 */
public abstract class Genome {
	public abstract Map<String, Color> COLORS();
	public abstract int IMGGENOMESTART();
	public abstract int IMGGENOMEEND();
	public abstract int GENOMESTART();
	public abstract int GENOMEEND();
	public abstract OrganismDefinition getOrganismDefinition();
	
	public int imgX(int pos) {
		return (int)(IMGGENOMESTART() + ((double)pos - GENOMESTART())
		         /((double)GENOMEEND() - GENOMESTART())
		         *((double)IMGGENOMEEND() - IMGGENOMESTART()));
	}
	
	public File getGenomePNG(File jobDir, int sequenceIndex, String thegenotype, int start, int end, int variant, String type, String csvData) throws IOException {
		File pngFile = new File(jobDir.getAbsolutePath() + File.separatorChar + "genome_" + sequenceIndex + "_" + type + "_" + variant + ".png");
	
		if(!pngFile.exists()) {
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

			int imgWidth = 584;
		    int imgHeight = 150;
		    
		    BufferedImage image = new BufferedImage(imgWidth,imgHeight,BufferedImage.TYPE_INT_ARGB);
		    Graphics2D g2d = (Graphics2D)image.getGraphics();
		    
		    //gray background
		    g2d.setColor(new Color(230, 230, 230));
		    g2d.fillRect(0, 0, imgWidth, imgHeight);
		    
		    Map<String, Color> colorMap = COLORS();
		    
		    Color bgcolor = colorMap.get("-");
		    if (w.length == 0) {
		      bgcolor = colorMap.get(thegenotype);
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

		        g2d.setColor(colorMap.get(assign[c]));
		        g2d.fillRect(imgX(x1), 0, imgX(x2)-imgX(x1), imgHeight);
		    }
	
		    Image genomePng = ImageIO.read(this.getClass().getResourceAsStream(getOrganismDefinition().getOrganismDirectory()+"/genome_"+variant+".png"));
		    g2d.drawImage(genomePng, 0, 0, imgWidth, imgHeight, null);
		    
		    ImageIO.write(image, "png", pngFile);
		}
		
		return pngFile;
	}
	
	public File getSmallGenomePNG(File jobDir, int sequenceIndex, String genotype, int start, int end, int variant, String type, String csvData) throws IOException {
		  File smallPngFile = new File(jobDir.getAbsolutePath() + File.separatorChar + "genomesmall_" + sequenceIndex + "_" + type + "_" + variant + ".png");

		  if (!smallPngFile.exists()) {
		    File pngFile = getGenomePNG(jobDir, sequenceIndex, genotype, start, end, variant, type, csvData);
		    GenotypeLib.scalePNG(pngFile, smallPngFile, 40.0);
		  }

		  return smallPngFile;
	}
}
