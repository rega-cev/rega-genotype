package rega.genotype.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.utils.FileUtil;
import rega.genotype.utils.GsonUtil;

/**
 * Json unique tool index stored on Repo server.
 * The ToolRepoService creates ToolIndex per tool (not version)
 * @author michael
 */
public class ToolIndexes {
	public static String TOOL_INDEXES_FILE_NAME = "ToolIndexes.json";
	
	private List<ToolIndex> indexes = new ArrayList<ToolIndexes.ToolIndex>();
	
	public static ToolIndexes parseJsonAsList(String json) {
		return GsonUtil.parseJson(json, ToolIndexes.class);
	}

	public String toJson() {
		return GsonUtil.toJson(this);
	}

	public void save(String externalDir) throws IOException {
		FileUtil.writeStringToFile(new File(externalDir + TOOL_INDEXES_FILE_NAME), toJson());
	}

	public ToolIndex getIndex(String toolId) {
		for (ToolIndex toolIndex: indexes)
			if (toolIndex.getToolId().equals(toolId))
				return toolIndex;

		return null;
	}

	public boolean removeIndex(String toolId, String version) {
		for (ToolIndex index: indexes) 
			if (index.getToolId().equals(toolId) 
					&& index.getToolVersion().equals(version))
				return indexes.remove(index);

		return false;
	}

	public List<ToolIndex> getIndexes() {
		return indexes;
	}

	public void setIndexes(List<ToolIndex> indexes) {
		this.indexes = indexes;
	}

	// classes
	
	public static class ToolIndex {
		// publisher unique pwd created when automatically with global config and stored also global config.
		// sent by header in PUBLISH_REQ
		private String publisherName; 
		private String publisherPassword; 
		private String toolId;
		private String toolVersion;
		private String fileName;

		public ToolIndex(){}

		public ToolIndex(String publisherPassword, String toolId,
				String toolVersion, String publisherName, String fileName){
			this.publisherPassword = publisherPassword;
			this.toolId = toolId;
			this.publisherName = publisherName;
			this.fileName = fileName;
			this.toolVersion = toolVersion;
		}

		public String getPublisherPassword() {
			return publisherPassword;
		}
		public void setPublisherPassword(String publisherPassword) {
			this.publisherPassword = publisherPassword;
		}

		public String getToolId() {
			return toolId;
		}

		public void setToolId(String toolId) {
			this.toolId = toolId;
		}

		public String getPublisherName() {
			return publisherName;
		}

		public void setPublisherName(String publisherName) {
			this.publisherName = publisherName;
		}

		public File getFile(String repoDir) {
			return new File(repoDir, fileName);
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getToolVersion() {
			return toolVersion;
		}

		public void setToolVersion(String toolVersion) {
			this.toolVersion = toolVersion;
		}
	}
}
