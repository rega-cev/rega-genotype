package rega.genotype.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import rega.genotype.utils.FileUtil;
import rega.genotype.utils.GsonUtil;

public class XmlPlaceholders {
	public static final String XML_VARIABLES_FILE_NAME = "xml_variables.json";

	// var name, var content.
	private Map<String, String> placeholders = new TreeMap<String, String>();

	public XmlPlaceholders() {}

	public static XmlPlaceholders parseJson(String json) {
		if (json == null)
			return null;

		return GsonUtil.parseJson(json, XmlPlaceholders.class);
	}

	public static XmlPlaceholders read(File xmlDir) {
		File file = new File(xmlDir, XML_VARIABLES_FILE_NAME);
		if (file.exists())
			return parseJson(FileUtil.readFile(file));
		else
			return null;
	}

	public String toJson() {
		return GsonUtil.toJson(this);
	}

	public void save(String externalDir) {
		try {
			FileUtil.writeStringToFile(new File(externalDir + File.separator + XML_VARIABLES_FILE_NAME), toJson());
		} catch (IOException e) {
			e.printStackTrace();
			assert(false);
		}
	}

	public Map<String, String> getPlaceholders() {
		return placeholders;
	}

	public void setPlaceholders(Map<String, String> placeholders) {
		this.placeholders = placeholders;
	}
}
