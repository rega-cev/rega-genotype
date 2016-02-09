package rega.genotype.ui.viruses.generic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Read json config (rega-genotype/rega-genotype/config)
 * 
 * @author michael
 *
 */
public class GenericDefinitionJsonConfig {
	/* json example.
	{
    name: "HAV Typing Tool",
    path: "HAV",
    version: "1.1",
    configuration: "/home/michael/projects/rega-genotype-extenal/xml/HAV/",
		jobDir: "/home/michael/projects/rega-genotype-extenal/job/",
    auto-update: "true"
  	}
	 */
	private String name;
	private String path; // organism
	private String configuration; // xmlPath
	private String jobDir;
	private boolean autoUpdate;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getConfiguration() {
		return configuration;
	}
	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}
	public boolean isAutoUpdate() {
		return autoUpdate;
	}
	public void setAutoUpdate(boolean autoUpdate) {
		this.autoUpdate = autoUpdate;
	}

	public static GenericDefinitionJsonConfig[] parseJson(String json) {
		GsonBuilder builder = new GsonBuilder().setPrettyPrinting().serializeNulls();
		Gson gson = builder.create();

		try {
			return gson.fromJson(json, new TypeToken<GenericDefinitionJsonConfig[]>() {}.getType());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public String getJobDir() {
		return jobDir;
	}
	public void setJobDir(String jobDir) {
		this.jobDir = jobDir;
	}
}
