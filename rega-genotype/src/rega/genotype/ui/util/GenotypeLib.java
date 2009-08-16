package rega.genotype.ui.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.PhyloClusterAnalysis;
import rega.genotype.SequenceAlign;
import rega.genotype.viruses.hiv.HIVTool;

public class GenotypeLib {
	public static void initSettings(Settings s) {
		PhyloClusterAnalysis.paupCommand = s.getPaupCmd();
		SequenceAlign.clustalWPath = s.getClustalWCmd();
		GenotypeTool.setXmlBasePath(s.getXmlPath().getAbsolutePath());
		BlastAnalysis.blastPath = s.getBlastPath().getAbsolutePath();
		PhyloClusterAnalysis.puzzleCommand = s.getTreePuzzleCmd();
	}

	public static void startAnalysis(File jobDir, Class analysis,
			Settings settings) {

	}

	public static void scalePNG(File in, File out, double perc) throws IOException {
		Image i = ImageIO.read(in);
		Image resizedImage = null;

		int newWidth = (int) (i.getWidth(null) * perc / 100.0);
		int newHeight = (int) (i.getHeight(null) * perc / 100.0);

		resizedImage = i.getScaledInstance(newWidth, newHeight,
				Image.SCALE_SMOOTH);

		// This code ensures that all the pixels in the image are loaded.
		Image temp = new ImageIcon(resizedImage).getImage();

		// Create the buffered image.
		BufferedImage bufferedImage = new BufferedImage(temp.getWidth(null),
				temp.getHeight(null), BufferedImage.TYPE_INT_RGB);

		// Copy image to buffered image.
		Graphics g = bufferedImage.createGraphics();

		// Clear background and paint the image.
		g.setColor(Color.white);
		g.fillRect(0, 0, temp.getWidth(null), temp.getHeight(null));
		g.drawImage(temp, 0, 0, null);
		g.dispose();

		// Soften.
		float softenFactor = 0.05f;
		float[] softenArray = { 0, softenFactor, 0, softenFactor,
				1 - (softenFactor * 4), softenFactor, 0, softenFactor, 0 };
		Kernel kernel = new Kernel(3, 3, softenArray);
		ConvolveOp cOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
		bufferedImage = cOp.filter(bufferedImage, null);

		ImageIO.write(bufferedImage, "png", out);
	}

	public static void getSignalPNG(int jobid, File svgFile, File pngFile) {
		if(!pngFile.exists() && svgFile.exists()){
			ImageConverter.svgToPng(svgFile, pngFile);
		}
	}
	
	public static void getTreePDF(int jobid, File svgFile, File pdfFile) {
		if (!pdfFile.exists() && svgFile.exists()) {
			ImageConverter.svgToPdf(svgFile, pdfFile);
		}
	}


	public static void main(String[] args) {
		Settings s = Settings.getInstance();

		initSettings(s);

		try {
			HIVTool hiv = new HIVTool(new File(
					"/home/simbre1/tmp/genotype"));
			hiv.analyze(
					"/home/simbre1/tmp/genotype/seq.fasta",
					"/home/simbre1/tmp/genotype/result.xml");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParameterProblemException e) {
			e.printStackTrace();
		} catch (FileFormatException e) {
			e.printStackTrace();
		}
	}
}
