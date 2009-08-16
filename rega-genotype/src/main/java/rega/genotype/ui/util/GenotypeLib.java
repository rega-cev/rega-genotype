/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.commons.io.IOUtils;

import rega.genotype.ApplicationException;
import rega.genotype.BlastAnalysis;
import rega.genotype.GenotypeTool;
import rega.genotype.PhyloClusterAnalysis;
import rega.genotype.SequenceAlign;
import rega.genotype.ui.data.OrganismDefinition;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WResource;
import eu.webtoolkit.jwt.WString;

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

	@SuppressWarnings("unchecked")
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
				Process proc;
				int result;
				String cmd;
				
				cmd = treeGraphCommand +" -t "+ treeFile.getAbsolutePath();
				System.err.println(cmd);
				proc = runtime.exec(cmd, null, jobDir);
				if((result = proc.waitFor()) != 0)
					throw new ApplicationException(cmd +" exited with error: "+result);
				
				File tgfFile = new File(treeFile.getPath().replace(".tre", ".tgf"));
				File resizedTgfFile = new File(treeFile.getPath().replace(".tre", ".resized.tgf"));
				
				BufferedReader in = new BufferedReader(new FileReader(tgfFile));
				PrintStream out = new PrintStream(new FileOutputStream(resizedTgfFile));
				String line;
				while((line = in.readLine()) != null){
					line = line.replace("\\width{150}" ,"\\width{180}");
					line = line.replace("\\height{250}" ,"\\height{370}");
					line = line.replace("\\margin{0}{0}{0}{0}" ,"\\margin{10}{10}{10}{10}");
					line = line.replace("\\style{r}{plain}{14.4625}","\\style{r}{plain}{10}");
					line = line.replaceAll("\\\\len\\{-", "\\\\len\\{");
					out.println(line);
					
					if (line.equals("\\begindef")) {
						out.println("\\paper{a2}");
					}
				}
				out.close();
				in.close();
				
				tgfFile.delete();
				resizedTgfFile.renameTo(tgfFile);
				
				cmd = treeGraphCommand +" -v "+ tgfFile.getAbsolutePath();
				System.err.println(cmd);
				proc = runtime.exec(cmd, null, jobDir);
				if((result = proc.waitFor()) != 0)
					throw new ApplicationException(cmd +" exited with error: "+result);
				
				File svgFile = new File(treeFile.getPath().replace(".tre", ".svg"));
				ImageConverter.svgToPdf(svgFile, pdfFile);
				svgFile.delete();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		return pdfFile;
	}
	
	public static File createJobDir(OrganismDefinition od){
		File jobDir = Settings.getInstance().getJobDir(od);
		File d;
		Random r = new Random(new Date().getTime());
		do{
			d = new File(jobDir.getAbsolutePath() + File.separator + r.nextInt(Integer.MAX_VALUE));
		}while(d.exists());
		
		d.mkdir();
		return d;
	}

	public static WImage getWImageFromFile(final File f) {
		System.out.println("*** getWImageFromFile: "+ f.getAbsolutePath());
		WImage chartImage = new WImage(new WResource() {
			
			{
				suggestFileName("x.png");
			}

            @Override
            public String resourceMimeType() {
                return "image/png";
            }

            @Override
            protected boolean streamResourceData(OutputStream stream, HashMap<String, String> arguments) throws IOException {
				try {
					FileInputStream fis = new FileInputStream(f);
	                try {
	                    IOUtils.copy(fis, stream);
	                    stream.flush();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	                finally{
	                	IOUtils.closeQuietly(fis);
	                }
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
				return true;
            }
            
        }, new WString(""), (WContainerWidget)null);
		
		return chartImage;
	}
	
	public static WImage getWImageFromResource(final OrganismDefinition od, final String fileName, WContainerWidget parent) {
		System.out.println("*** getWImageFromResource: "+ fileName);
		return new WImage(new WResource() {
            @Override
            public String resourceMimeType() {
                return "image/"+fileName.substring(fileName.lastIndexOf('.')+1);
            }
            @Override
            protected boolean streamResourceData(OutputStream stream, HashMap<String, String> arguments) throws IOException {
            	InputStream is = this.getClass().getResourceAsStream(od.getOrganismDirectory()+fileName);
                try {
                    IOUtils.copy(is, stream);
                    stream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
            	} finally {
            		IOUtils.closeQuietly(is);
            	}
            	
            	return true;
            }
        }, new WString(""), parent);
	}
	
	public static File getFile(File jobDir, String fileName) {
		return new File(jobDir.getAbsolutePath() + File.separatorChar + fileName);
	}
	
	public static WAnchor getAnchor(String text, String fileType, File f) {
		WAnchor anchor = new WAnchor("", WContainerWidget.lt(text));
		anchor.setStyleClass("link");
		anchor.setTarget(AnchorTarget.TargetNewWindow);
		WResource fr = new WFileResource(fileType, f.getAbsolutePath());
		fr.suggestFileName(f.getName());
		anchor.setRef(fr.generateUrl());
		return anchor;
	}
	
	public static File getZipArchiveFileName(File dir){
		return new File(dir.getAbsolutePath()+".zip");
	}

	public static void zip(File dir, File zipFile) {
		byte[] buffer = new byte[1024*1024];

		try {

			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));

			out.setLevel(Deflater.DEFAULT_COMPRESSION);

			for (File f : dir.listFiles()) {
				FileInputStream in = new FileInputStream(f);

				//new entry, relative path
				out.putNextEntry(new ZipEntry(f.getAbsolutePath().replace(dir.getAbsolutePath()+File.separatorChar, "")));

				int len;
				while ((len = in.read(buffer)) > 0) {
					out.write(buffer, 0, len);
				}
				
				out.closeEntry();
				in.close();
			}
			out.close();
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Settings s = Settings.getInstance();

		initSettings(s);

// try {
// HIVTool hiv = new HIVTool(new File(
// "/home/simbre1/tmp/genotype"));
// hiv.analyze(
// "/home/simbre1/tmp/genotype/seq.fasta",
// "/home/simbre1/tmp/genotype/result.xml");
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
