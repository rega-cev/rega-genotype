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

	// classes
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
