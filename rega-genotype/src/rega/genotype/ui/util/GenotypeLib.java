package rega.genotype.ui.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import net.sf.witty.wt.WAnchor;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WFileResource;
import net.sf.witty.wt.WImage;
import net.sf.witty.wt.WResource;

import org.apache.commons.io.IOUtils;

import rega.genotype.BlastAnalysis;
import rega.genotype.GenotypeTool;
import rega.genotype.PhyloClusterAnalysis;
import rega.genotype.SequenceAlign;

public class GenotypeLib {
	
	public static String treeGraphCommand = "/usr/bin/tgf";
	
	public static void initSettings(Settings s) {
		PhyloClusterAnalysis.paupCommand = s.getPaupCmd();
		SequenceAlign.clustalWPath = s.getClustalWCmd();
		GenotypeTool.setXmlBasePath(s.getXmlPath().getAbsolutePath() + File.separatorChar);
		BlastAnalysis.blastPath = s.getBlastPath().getAbsolutePath() + File.separatorChar;
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

	public static File getSignalPNG(File svgFile) {
		File pngFile = new File(svgFile.getAbsolutePath().replace(".svg", ".png"));
		if(!pngFile.exists() && svgFile.exists()){
			ImageConverter.svgToPng(svgFile, pngFile);
		}
		return pngFile;
	}
	
	public static File getTreePDF(File jobDir, File treeFile) {
		File pdfFile = new File(treeFile.getPath().replace(".tre", ".pdf"));
		if (!pdfFile.exists() && treeFile.exists()) {
			
			try{
				Runtime runtime = Runtime.getRuntime();
				runtime.exec(treeGraphCommand +" -t "+ treeFile.getAbsolutePath(), null, jobDir);
				
				File tgfFile = new File(treeFile.getPath().replace(".tre", ".tgf"));
				File resizedTgfFile = new File(treeFile.getPath().replace(".tre", ".resized.tgf"));
				
				BufferedReader in = new BufferedReader(new FileReader(tgfFile));
				PrintStream out = new PrintStream(new FileOutputStream(resizedTgfFile));
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
				
				tgfFile.delete();
				resizedTgfFile.renameTo(tgfFile);
				
				runtime.exec(treeGraphCommand +" -v "+ tgfFile.getAbsolutePath(), null, jobDir);
				File svgFile = new File(treeFile.getPath().replace(".tre", ".svg"));
				
				ImageConverter.svgToPdf(svgFile, pdfFile);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		return pdfFile;
	}
	
	public static File createJobDir(){
		File jobDir = Settings.getInstance().getJobDir();
		File d;
		Random r = new Random(new Date().getTime());
		do{
			d = new File(jobDir.getAbsolutePath() + File.separator + "job-" + r.nextInt(Integer.MAX_VALUE));
		}while(d.exists());
		
		d.mkdir();
		return d;
	}

	public static WImage getWImageFromFile(final File f) {
		WImage chartImage = new WImage(new WResource() {

            @Override
            public String resourceMimeType() {
                return "image/png";
            }

            @Override
            protected void streamResourceData(OutputStream stream) {
                try {
                	FileInputStream fis = new FileInputStream(f);
                    IOUtils.copy(fis, stream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
        }, (WContainerWidget)null);
		
		return chartImage;
	}
	
	public static File getFile(File jobDir, String fileName) {
		return new File(jobDir.getAbsolutePath() + File.separatorChar + fileName);
	}
	
	public static WAnchor getAnchor(String text, String fileType, File f) {
		WAnchor anchor = new WAnchor((String)null, WContainerWidget.lt(text));
		anchor.setStyleClass("link");
		anchor.setRef(new WFileResource(fileType, f.getAbsolutePath()).generateUrl());
		return anchor;
	}

	public static void main(String[] args) {
		Settings s = Settings.getInstance();

		initSettings(s);

//		try {
//			HIVTool hiv = new HIVTool(new File(
//					"/home/simbre1/tmp/genotype"));
//			hiv.analyze(
//					"/home/simbre1/tmp/genotype/seq.fasta",
//					"/home/simbre1/tmp/genotype/result.xml");
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (ParameterProblemException e) {
//			e.printStackTrace();
//		} catch (FileFormatException e) {
//			e.printStackTrace();
//		}
		
		getTreePDF(new File("/home/simbre1/tmp/genotype/"), new File("/home/simbre1/tmp/genotype/r7184492.tre"));
	}
}
