package rega.genotype.config;

import rega.genotype.ui.util.GsonUtil;

/**
 * Read json tool identifier. Stored in the tool dir (tool xml dir)
 * Created by tool/ version author, and can not be edited by other users.
 *  
 * @author michael
 */
public class ToolMenifest {
	private String name;
	private String id;
	private String version;
	private boolean blastTool;

	public static ToolMenifest parseJson(String json) {
		return GsonUtil.parseJson(json, ToolMenifest.class);
	}

	public String toJson() {
		return GsonUtil.toJson(this);
	}

	/**
	 * @return unique tool id (used as the tool dir name) 
	 */
	public String getToolId() {
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
