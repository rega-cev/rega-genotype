package rega.genotype.ui.admin.file_editor.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.python.google.common.reflect.TypeToken;

import rega.genotype.utils.GsonUtil;

public class ConfigXmlWriter {
	public enum CssTheme {
		Detault, RIVM;

		public static CssTheme fromString(String s) {
			return s != null && s.equals(RIVM.name()) ? RIVM : Detault;
		}
	}

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

	public static void writeMetaData(File workdir, ToolMetadata metaData) throws JDOMException, IOException {
		File configFile = new File(workdir, "config.xml");
		if (configFile.exists()) {
			SAXBuilder builder = new SAXBuilder();
			Document document = builder.build(configFile);
			Element root = document.getRootElement();

			root.removeChildren("meta-data");

			Element metaDataE = new Element("meta-data");
			add(metaDataE, "cluster-count", metaData.clusterCount);
			if (metaData.canAccess != null)
				add(metaDataE, "access", metaData.canAccess);
			add(metaDataE, "taxonomy-ids", metaData.taxonomyIdsJson());

			root.addContent(metaDataE);

			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(document, new FileWriter(configFile));
		} else {
			// TODO
		}
	}

	public static void writeTheme(File workdir, CssTheme theme) throws JDOMException, IOException {
		File configFile = new File(workdir, "config.xml");
		if (configFile.exists()) {
			SAXBuilder builder = new SAXBuilder();
			Document document = builder.build(configFile);
			Element root = document.getRootElement();

			root.removeChildren("css-theme");

			Element themeE = new Element("css-theme");
			themeE.setText(theme.name());

			root.addContent(themeE);

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

	/**
	 * For some tool blast.xml can be very big. Some data that can describe the tool is
	 * saved here. (because parsing blast.xml can take long)
	 */
	public static class ToolMetadata {
		public Integer clusterCount = null;
		public Integer canAccess = null; // pan-viral tool can redirect to other tools.
		public Set<String> taxonomyIds = null;

		public String taxonomyIdsJson() {
			return GsonUtil.toJson(taxonomyIds, false);
		}

		public static Set<String> parseJsonAsList(String json) {
			return new HashSet(
					GsonUtil.parseJson(json, new TypeToken<List<String> >() {}.getType()));
		}
	}

}
