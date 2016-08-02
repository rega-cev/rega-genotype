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
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.EnumSet;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.commons.io.IOUtils;

import rega.genotype.ApplicationException;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.utils.StreamReaderThread;
import eu.webtoolkit.jwt.FileUtils;
import eu.webtoolkit.jwt.Utils;
import eu.webtoolkit.jwt.Utils.HtmlEncodingFlag;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WResource;
import eu.webtoolkit.jwt.WResource.DispositionType;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;

/**
 * General utility class for creating supporting data and images for a genotype job.
 * 
 * @author simbre1
 *
 */
public class GenotypeLib {
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
	
	private static void imageMagickConvert(File in, File out) throws IOException, InterruptedException {
		System.err.println("Running: " + Settings.getInstance().getImageMagickConvertCmd() + " " + in.getAbsolutePath() + " " + out.getAbsolutePath());

		Process ps = new ProcessBuilder(Settings.getInstance().getImageMagickConvertCmd(), in.getAbsolutePath(), out.getAbsolutePath()).start();
		
		StreamReaderThread stdout = new StreamReaderThread(ps.getInputStream(), System.out, "image magick stdout: ");
		stdout.start();
		
		StreamReaderThread stderr = new StreamReaderThread(ps.getErrorStream(), System.err, "image magick stderr: ");
		stderr.start();
		
		ps.waitFor();
	}
	
	private static void epsToPng(File in, File out) throws IOException, InterruptedException {
		imageMagickConvert(in, out);
	}
	
	public static File getSignalPNG(File epsFile) {
		try {
			File pngFile = File.createTempFile("signal", ".png");
			
			epsToPng(epsFile, pngFile);

			return pngFile;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
		
		return null;
	}

	public static File getTreePNG(File jobDir, File treeFile) {
		File pngFile = new File(treeFile.getPath().replace(".tre", ".png"));
		if (!pngFile.exists() && treeFile.exists()) {
			try {
				File epsFile = getTreeEPS(jobDir, treeFile);
				
				epsToPng(epsFile, pngFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return pngFile;
	}

	public static File getTreePDF(File jobDir, File treeFile) {
		File pdfFile = new File(treeFile.getPath().replace(".tre", ".pdf"));
		if (!pdfFile.exists() && treeFile.exists()) {
			try{
				File epsFile = getTreeEPS(jobDir, treeFile);
				
				epsToPdf(epsFile, pdfFile);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		return pdfFile;
	}

	private static void epsToPdf(File epsFile, File pdfFile) throws IOException, InterruptedException {
		Process ps = new ProcessBuilder(Settings.getInstance().getEpsToPdfCmd(), epsFile.getAbsolutePath(), "--outfile=" + pdfFile.getAbsolutePath()).start();
		
		StreamReaderThread stdout = new StreamReaderThread(ps.getInputStream(), System.out, "epstopdf stdout: ");
		stdout.start();
		
		StreamReaderThread stderr = new StreamReaderThread(ps.getErrorStream(), System.err, "epstopdf stderr: ");
		stderr.start();
		
		ps.waitFor();
	}

	private static File getTreeEPS(File jobDir, File treeFile)
			throws IOException, InterruptedException, ApplicationException,
			FileNotFoundException {
		File epsFile = new File(treeFile.getPath().replace(".tre", ".eps"));
		if (epsFile.exists())
			return epsFile;

		Runtime runtime = Runtime.getRuntime();
		Process proc;
		int result;
		String cmd;

		cmd = Settings.treeGraphCommand +" -t "+ treeFile.getAbsolutePath();
		System.err.println(cmd);
		proc = runtime.exec(cmd, null, jobDir);
		
        InputStream inputStream = proc.getInputStream();

        LineNumberReader reader
            = new LineNumberReader(new InputStreamReader(inputStream));

        Pattern taxaCountPattern = Pattern.compile("(\\d+) taxa read.");

        int taxa = 0;
        for (;;) {
            String s = reader.readLine();
            if (s == null)
                break;
            Matcher m = taxaCountPattern.matcher(s);
            
            if (m.find()) {
                taxa = Integer.valueOf(m.group(1)).intValue();
            }
        }

		if ((result = proc.waitFor()) != 0)
			throw new ApplicationException(cmd +" exited with error: "+result);

		proc.getErrorStream().close();
		proc.getInputStream().close();
		proc.getOutputStream().close();

		File tgfFile = new File(treeFile.getPath().replace(".tre", ".tgf"));
		File resizedTgfFile = new File(treeFile.getPath().replace(".tre", ".resized.tgf"));

		BufferedReader in = new BufferedReader(new FileReader(tgfFile));
		PrintStream out = new PrintStream(new FileOutputStream(resizedTgfFile));
		String line;
		while((line = in.readLine()) != null){
			line = line.replace("\\width{" ,"%\\width{");
			line = line.replace("\\height{" ,"%\\height{");
			line = line.replace("\\margin{" ,"%\\margin{");
			line = line.replace("\\style{r}{plain}","%\\style{r}{plain}");
			line = line.replace("\\style{default}{plain}","%\\style{default}{plain}");
			line = line.replaceAll("\\\\len\\{-", "\\\\len\\{");
			out.println(line);
			
			if (line.equals("\\begindef")) {
				out.println("\\paper{a2}");
				out.println("\\width{170}");
				out.println("\\height{" + (taxa * 8.6) + "}");
				out.println("\\margin{10}{10}{10}{10}");
				out.println("\\style{default}{plain}{13}");
				out.println("\\style{r}{plain}{13}");
			}
		}
		out.close();
		in.close();
		
		tgfFile.delete();
		resizedTgfFile.renameTo(tgfFile);
		
		cmd = Settings.treeGraphCommand +" -p "+ tgfFile.getAbsolutePath();
		System.err.println(cmd);
		proc = runtime.exec(cmd, null, jobDir);
		if((result = proc.waitFor()) != 0)
			throw new ApplicationException(cmd +" exited with error: "+result);

		proc.getErrorStream().close();
		proc.getInputStream().close();
		proc.getOutputStream().close();

		return epsFile;
	}
	
	public static File createJobDir(String jobDir) {
		File d;
		Random r = new Random(new Date().getTime());
		do {
			d = new File(jobDir + File.separator + r.nextInt(Integer.MAX_VALUE));
		} while(d.exists());
		
		d.mkdirs();
		return d;
	}

	public static WImage getWImageFromResource(WResource resource) {
		WImage result = new WImage(resource, new WString(""));
		resource.suggestFileName("x.png");
		return result;
	}
	
	public static InputStream getResourceAsStream(String path) throws IOException {
		return FileUtils.getResourceAsStream(path);
	}

	public static WImage getWImageFromResource(final OrganismDefinition od, final String fileName, WContainerWidget parent) throws IOException {
		final InputStream is = getResourceAsStream(od.getXmlPath() + File.separator + fileName);
		
		if (is == null)
			return null;
		else 
			IOUtils.closeQuietly(is);
		
		return new WImage(new WLink(new WResource() {
			@Override
			protected void handleRequest(WebRequest request, WebResponse response) throws IOException {
				response.setContentType("image/" + fileName.substring(fileName.lastIndexOf('.')+1));
				
            	InputStream is = getResourceAsStream(od.getXmlPath() + File.separator + fileName);
            	if (is == null)
            		return;
                try {
                    IOUtils.copy(is, response.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
            	} finally {
            		IOUtils.closeQuietly(is);
            	}
			}
        }), new WString(""), parent);
	}
	
	public static File getFile(File jobDir, String fileName) {
		return new File(jobDir.getAbsolutePath() + File.separatorChar + fileName);
	}
	
	public static WAnchor getAnchor(String text, String fileType, WResource resource, String suggestedName) {
		resource.setDispositionType(DispositionType.Attachment);
		if (suggestedName != null)
			resource.suggestFileName(suggestedName);
		
		WAnchor anchor = new WAnchor("", text);
		anchor.setObjectName("resource");
		anchor.setStyleClass("link");
		anchor.setLink(new WLink(resource));
		
		return anchor;
	}
	
	public static void zip(File dir, OutputStream os) {
		byte[] buffer = new byte[1024*1024];

		try {

			ZipOutputStream out = new ZipOutputStream(os);

			out.setLevel(Deflater.DEFAULT_COMPRESSION);

			for (File f : dir.listFiles()) {
				FileInputStream in = new FileInputStream(f);

				//new entry, relative path
				out.putNextEntry(new ZipEntry(f.getAbsolutePath().replace(dir.getAbsolutePath() + File.separatorChar, "")));

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

	public static String readFileToString(File f) throws IOException {
	    StringBuilder contents = new StringBuilder();
	    
	    BufferedReader input = null;
		try {
			input = new BufferedReader(new FileReader(f));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	    
		String line = null;

	        while (( line = input.readLine()) != null){
	          contents.append(line);
	          contents.append(System.getProperty("line.separator"));
	        }

	    return contents.toString();
	}
	
    public static String getEscapedValue(GenotypeResultParser p, String name) {
    	String value = p.getValue(name);
    	if (value == null)
    		return null;
    	else
    		return Utils.htmlEncode(value, EnumSet.of(HtmlEncodingFlag.EncodeNewLines));
    }
}
