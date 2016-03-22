package rega.genotype.config;

import java.io.File;
import java.io.IOException;
import java.util.List;

import rega.genotype.ui.util.FileUtil;
import rega.genotype.ui.util.GsonUtil;

import com.google.gson.reflect.TypeToken;

/**
 * Read json tool identifier. Stored in the tool dir (tool xml dir)
 * Created by tool/ version author, and can not be edited by other users.
 *  
 * @author michael
 */
public class ToolManifest {
	public static final String MANIFEST_FILE_NAME = "manifest.json";
	private String name;
	private String id;
	private String version;
	private boolean blastTool;

	public ToolManifest() {}

	public static ToolManifest parseJson(String json) {
		return GsonUtil.parseJson(json, ToolManifest.class);
	}

	public static List<ToolManifest> parseJsonAsList(String json) {
		return GsonUtil.parseJson(json, new TypeToken<List<ToolManifest> >() {}.getType());
	}

	public String toJson() {
		return GsonUtil.toJson(this);
	}

	public void save(String externalDir) throws IOException {
		FileUtil.writeStringToFile(new File(externalDir + MANIFEST_FILE_NAME), toJson());
	}
	/**
	 * @return unique tool id (used as the tool dir name) 
	 */
	public String getUniqueToolId() {
		return id + version;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public boolean isBlastTool() {
		return blastTool;
	}
	public void setBlastTool(boolean blastTool) {
		this.blastTool = blastTool;
	}
}
