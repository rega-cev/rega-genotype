package rega.genotype.ui.admin.file_editor.xml;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import rega.genotype.ui.admin.file_editor.xml.ConfigXmlReader.FileManifest.FileType;
import rega.genotype.ui.admin.file_editor.xml.ConfigXmlWriter.Genome;
import rega.genotype.ui.admin.file_editor.xml.ConfigXmlWriter.ToolMetadata;

/**
 * Utility class to read config.xml
 * 
 * TODO: use it in GenericDefinition and HivDefinition
 * 
 * @author michael
 */
public class ConfigXmlReader {

	public static List<FileManifest> readFileManifests(File xmlDir) throws JDOMException, IOException {
		List<FileManifest> ans = new ArrayList<FileManifest>();

		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(xmlDir.getAbsolutePath() + File.separator + "config.xml");
		Element root = document.getRootElement();
		Element cssE = root.getChild("css");
		if (cssE != null)
			for (Object o : cssE.getChildren("file")) {
				Element fileE = (Element) o;
				Attribute attribute = fileE.getAttribute("type");
				FileType fileType = FileType.CSS;
				if (attribute != null && attribute.getValue().equals("IE"))
					fileType = FileType.CSS_IE;
				String fileName = fileE.getText();

				ans.add(new FileManifest(fileName, fileType));
			}

		return ans;
	}

	public static Genome readGenome(File xmlDir) {
		File configFile = new File(xmlDir.getAbsolutePath(), "config.xml");
		if (!configFile.exists())
			return null;

		SAXBuilder builder = new SAXBuilder();
		Document document;
		try {			
			document = builder.build(configFile);
		} catch (JDOMException e1) {
			e1.printStackTrace();
			return null;
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
		Element root = document.getRootElement();
		Element genomeE = root.getChild("genome");
		Genome genome = new Genome();
		if (genomeE != null) {
			try {
				Element start = genomeE.getChild("start");
				if (start != null)
					genome.genomeStart = Integer.valueOf(start.getValue());

				Element end = genomeE.getChild("end");
				if (end != null)
					genome.genomeEnd = Integer.valueOf(end.getValue());

				Element imageStart = genomeE.getChild("image-start");
				if (imageStart != null)
					genome.imageStart = Integer.valueOf(imageStart.getValue());

				Element imageEnd = genomeE.getChild("image-end");
				if (imageEnd != null)
					genome.imageEnd = Integer.valueOf(imageEnd.getValue());

				Element color = genomeE.getChild("color");
				if (color != null)
					genome.color = color.getValue();

			} catch (NumberFormatException e) {
				return null;
			}
		}

		return genome;
	}

	public static ToolMetadata readMetadata(File xmlDir) {
		ToolMetadata ans = new ToolMetadata();

		File configxml = new File(xmlDir.getAbsolutePath() + File.separator + "config.xml");
		if (!configxml.exists())
			return null;
		SAXBuilder builder = new SAXBuilder();
		Document document;
		try {
			document = builder.build(configxml);
		} catch (JDOMException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		Element root = document.getRootElement();
		Element tableE = root.getChild("meta-data");
		if (tableE != null){
			Element clusterCountE = tableE.getChild("cluster-count");
			Element accessE = tableE.getChild("access");
			Element taxonomyE = tableE.getChild("taxonomy-ids");

			if (clusterCountE != null)
				ans.clusterCount = Integer.parseInt(clusterCountE.getText());
			if (accessE != null)
				ans.canAccess = Integer.parseInt(accessE.getText());
			if (taxonomyE != null)
				ans.taxonomyIds = ToolMetadata.parseJsonAsList(taxonomyE.getText());
		}
		return ans;
	}

	public static List<VerificationTableItem> readVerificationTable(File xmlDir) {
		List<VerificationTableItem> ans = new ArrayList<VerificationTableItem>();

		SAXBuilder builder = new SAXBuilder();
		Document document;
		try {
			document = builder.build(xmlDir.getAbsolutePath() + File.separator + "config.xml");
		} catch (JDOMException e) {
			e.printStackTrace();
			return defaultVerificationTable();
		} catch (IOException e) {
			e.printStackTrace();
			return defaultVerificationTable();
		}
		Element root = document.getRootElement();
		Element tableE = root.getChild("verification-table");
		if (tableE != null)
			for (Object o : tableE.getChildren("column")) {
				Element columnE = (Element) o;

				Element descriptionE = columnE.getChild("description");
				String description = descriptionE == null ? "(Empty)" :  descriptionE.getValue();

				Element valueE = columnE.getChild("value");
				String value = valueE == null ? "(Empty)" :  valueE.getValue();

				ans.add(new VerificationTableItem(description, value));
			}

		if (ans.isEmpty())
			return defaultVerificationTable();

		return ans;
	}
	public static List<VerificationTableItem> defaultVerificationTable() {
		List<VerificationTableItem> ans = new ArrayList<VerificationTableItem>();

		ans.add(new VerificationTableItem("Sequence name", "/genotype_result/sequence/@name"));
		ans.add(new VerificationTableItem("Type ID", "/genotype_result/sequence/conclusion[@id='type']/assigned/id"));
		ans.add(new VerificationTableItem("Subtype ID", "/genotype_result/sequence/conclusion[@id='subtype']/assigned/name"));

		return ans;
	}

	// classes

	public static class VerificationTableItem {
		private String description;
		private String resultsXmlVariable;
		public VerificationTableItem(String description, String resultsXmlVariable) {
			this.description = description;
			this.resultsXmlVariable = resultsXmlVariable;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public String getResultsXmlVariable() {
			return resultsXmlVariable;
		}
		public void setResultsXmlVariable(String resultsXmlVariable) {
			this.resultsXmlVariable = resultsXmlVariable;
		}
	}
	public static class FileManifest {
		public enum FileType {
			CSS,
			CSS_IE;
		};
		private FileType fileType;
		private String fileName;

		public FileManifest(String fileName, FileType fileType) {
			this.fileType = fileType;
			this.fileName = fileName;
		}

		public FileType getFileType() {
			return fileType;
		}
		public void setFileType(FileType fileType) {
			this.fileType = fileType;
		}
		public String getFileName() {
			return fileName;
		}
		public void setFileName(String fileName) {
			this.fileName = fileName;
		}
	}
}
