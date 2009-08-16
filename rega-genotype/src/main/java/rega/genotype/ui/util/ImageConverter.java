/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.util;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.fop.svg.PDFTranscoder;

public class ImageConverter {

	public static void svgToPng(File svgFile, File pngFile) {
		svgToPng(svgFile, pngFile, 0, 0);
	}

	public static void svgToPng(File svgFile, File pngFile, int width, int height) {
		svgToPng(svgFile, pngFile, width, height, null);
	}

	public static void svgToPng(File svgFile, File pngFile, int width, int height, Rectangle aoi) {
		PNGTranscoder trans = new PNGTranscoder();
		if(width > 0)
			trans.addTranscodingHint(PNGTranscoder.KEY_WIDTH, new Float(width));
		if(height > 0)
			trans.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, new Float(height));
		if (aoi != null)
			trans.addTranscodingHint(PNGTranscoder.KEY_AOI, aoi);
		transcode(trans, svgFile, pngFile);
	}

	public static void svgToPdf(File svgFile, File pdfFile) {
		svgToPdf(svgFile, pdfFile, 0, 0);
	}

	public static void svgToPdf(File svgFile, File pdfFile, int width, int height) {
		svgToPdf(svgFile, pdfFile, width, height, null);
	}

	public static void svgToPdf(File svgFile, File pdfFile, int width, int height, Rectangle aoi) {
		PDFTranscoder trans = new PDFTranscoder();
		if(width > 0)
			trans.addTranscodingHint(PDFTranscoder.KEY_WIDTH, new Float(width));
		if(height > 0)
			trans.addTranscodingHint(PDFTranscoder.KEY_HEIGHT, new Float(height));
		if (aoi != null)
			trans.addTranscodingHint(PDFTranscoder.KEY_AOI, aoi);
		transcode(trans, svgFile, pdfFile);
	}

	public static void transcode(Transcoder trans, File svgFile, File toFile) {
		// Transcode the file.
		try {
			TranscoderInput input = new TranscoderInput(svgFile.toURL().toString());
			OutputStream ostream = new FileOutputStream(toFile);
			TranscoderOutput output = new TranscoderOutput(ostream);
			trans.transcode(input, output);

			// Flush and close the output.
			ostream.flush();
			ostream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Rectangle aoi = null;
		if (args.length > 4) {
			aoi = new Rectangle(
					Integer.parseInt(args[4]),
					Integer.parseInt(args[5]),
					Integer.parseInt(args[6]),
					Integer.parseInt(args[7]));
		}
		svgToPng(new File(args[0]), new File(args[1]),
					Integer.parseInt(args[2]), // width
					Integer.parseInt(args[3]), // height
					aoi);
	}
}
