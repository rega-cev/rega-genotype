package rega.genotype.ui.util;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

public class SvgToPng {
	public static void convert(File svgFile, File pngFile, int width, int height, Rectangle aoi) {
		PNGTranscoder trans = new PNGTranscoder();
		trans.addTranscodingHint(PNGTranscoder.KEY_WIDTH, new Float(width));
		trans.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, new Float(height));
		if(aoi != null)
			trans.addTranscodingHint(PNGTranscoder.KEY_AOI, aoi);

		// Transcode the file.
		try {
			TranscoderInput input = new TranscoderInput(svgFile.toURL()
					.toString());
			OutputStream ostream = new FileOutputStream(pngFile);
			TranscoderOutput output = new TranscoderOutput(ostream);
			trans.transcode(input, output);

			// Flush and close the output.
			ostream.flush();
			ostream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		Rectangle aoi = null;
		if(args.length > 4){
			aoi = new Rectangle(
					Integer.parseInt(args[4]),
					Integer.parseInt(args[5]),
					Integer.parseInt(args[6]),
					Integer.parseInt(args[7]));
		}
		convert(new File(args[0]),new File(args[1]),
				Integer.parseInt(args[2]),	//width
				Integer.parseInt(args[3]),	//height
				aoi);
	}
}
