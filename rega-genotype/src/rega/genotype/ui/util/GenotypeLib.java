package rega.genotype.ui.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

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
	public static String treeGraphCommand = "/usr/bin/tgf";
	
	public static void initSettings(Settings s) {
		PhyloClusterAnalysis.paupCommand = s.getPaupCmd();
		SequenceAlign.clustalWPath = s.getClustalWCmd();
		GenotypeTool.setXmlBasePath(s.getXmlPath().getAbsolutePath());
		BlastAnalysis.blastPath = s.getBlastPath().getAbsolutePath();
		PhyloClusterAnalysis.puzzleCommand = s.getTreePuzzleCmd();
		treeGraphCommand = s.getTreeGraphCmd();
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
	
	public static File getCsvData(File jobDir, int sequenceIndex, String type) {
		return new File(jobDir.getAbsolutePath() + File.separatorChar + "plot_" + sequenceIndex + "_" + type + ".csv");
	}

	public static void getSignalPNG(File jodDir, File svgFile, File pngFile) {
		if(!pngFile.exists() && svgFile.exists()){
			ImageConverter.svgToPng(svgFile, pngFile);
		}
	}
	
	public static void getTreePDF(File jobDir, File treeFile, File pdfFile) {
		if (!pdfFile.exists() && treeFile.exists()) {
			File svgFile = new File(treeFile.getPath().replace(".tre", ".svg"));
			File tgfFile = new File(treeFile.getPath().replace(".tre", ".tgf"));
			
			try{
				Runtime runtime = Runtime.getRuntime();
				runtime.exec(treeGraphCommand +" -t "+ treeFile.getAbsolutePath(), null, jobDir);
				
				BufferedReader in = new BufferedReader(new FileReader(treeFile));
				PrintStream out = new PrintStream(new FileOutputStream(tgfFile));
				String line;
				while((line = in.readLine()) != null){
					line = line.replace("\\width{150}" ,"\\width{180}");
					line = line.replace("\\height{250}" ,"\\height{270}");
					line = line.replace("\\margin{0}{0}{0}{0}" ,"\\margin{10}{10}{10}{10}");
					line = line.replace("\\style{r}{plain}{14.4625}","\\style{r}{plain}{10}");
					out.println(line);
				}
				out.close();
				in.close();
				
				runtime.exec(treeGraphCommand +" -v "+ tgfFile.getAbsolutePath(), null, jobDir);	
				ImageConverter.svgToPdf(svgFile, pdfFile);
			}
			catch(Exception e){
				e.printStackTrace();
			}
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
