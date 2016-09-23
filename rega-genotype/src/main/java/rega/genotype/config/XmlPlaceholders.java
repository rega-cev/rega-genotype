package rega.genotype.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import rega.genotype.utils.FileUtil;
import rega.genotype.utils.GsonUtil;

/**
 * Allow saving variables that will be inserted to resources.xml outside the xml file.
 * Used to be able to create tool templates.
 * 
 * @author michael
 */
public class XmlPlaceholders {
	public static final String XML_VARIABLES_FILE_NAME = "xml_variables.json";

	// var name, var content.
	private Map<String, PlaceHolderData> placeholders = new TreeMap<String, PlaceHolderData>();

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

	public Map<String, PlaceHolderData> getPlaceholders() {
		return placeholders;
	}

	public void setPlaceholders(Map<String, PlaceHolderData> placeholders) {
		this.placeholders = placeholders;
	}

	public static class PlaceHolderData {
		private String value = new String();
		private String info = new String();
		private String id = new String();

		public PlaceHolderData() {}
		public PlaceHolderData(String value, String info, String id) {
			this.value = value;
			this.id = id;
			this.info = info;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getInfo() {
			return info;
		}

		public void setInfo(String info) {
			this.info = info;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}
}
