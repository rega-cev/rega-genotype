package rega.genotype.ui.admin.file_editor.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class ConfigXmlWriter {

	public static void writeGenome(File workdir, Genome genome) throws JDOMException, IOException {
		File configFile = new File(workdir, "config.xml");
		if (configFile.exists()) {
			SAXBuilder builder = new SAXBuilder();
			Document document = builder.build(configFile);
			Element root = document.getRootElement();

			root.removeChildren("genome");

			Element genomeE = new Element("genome");
			add(genomeE, "color", genome.color);
			genomeE.getChild("color").setAttribute("assignment", "-");
			add(genomeE, "start", genome.genomeStart);
			add(genomeE, "end", genome.genomeEnd);
			add(genomeE, "image-start", genome.imageStart);
			add(genomeE, "image-end", genome.imageEnd);

			root.addContent(genomeE);

			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(document, new FileWriter(configFile));
		} else {
			// TODO
		}
	}

    private static void add(Element e, String tag, String value) {
		e.addContent(new Element(tag).setText(value));
    }

    private static void add(Element e, String tag, int value) {
        add(e, tag, Integer.toString(value));
    }

	// classes

	public static class Genome {
		public int imageStart;
		public int imageEnd;
		public int genomeStart;
		public int genomeEnd;
		public String color;
	}
}
