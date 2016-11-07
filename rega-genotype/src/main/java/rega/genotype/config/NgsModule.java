package rega.genotype.config;

import java.io.File;
import java.io.IOException;

import rega.genotype.utils.FileUtil;
import rega.genotype.utils.GsonUtil;

/**
 * NGS Moduls contains all the files and configuration needed for the ngs analysis. 
 * This is saved in json.
 * 
 * @author michael
 */
public class NgsModule {
	public static final String NGS_MODULE_ID = "NGS_Module";
	public static final String NGS_MODULE_FILE_NAME = "ngs-module.json";
	public static final String NGS_MODULE_AA_VIRUSES_DB = "aa-virus.dmnd";
	public static final String NGS_MODULE_UNIREF_VIRUSES_AA50 = "uniref-viruses-aa50.fasta";

	// internal to ngs module, used to create AA database for diamond blast step (NGS_MODULE_AA_VIRUSES_DB)
	private String aaFileName = null; 
	private String taxonomyFileName = null;

	private String ncbiVirusesFileName = null;
	
	public NgsModule() {}

	public static NgsModule read(File xmlDir) {
		File f = new File(xmlDir, NGS_MODULE_FILE_NAME);
		if (!f.exists())
			return null;

		return parseJson(FileUtil.readFile(f));
	}
	
	public static NgsModule parseJson(String json) {
		if (json == null)
			return null;

		return GsonUtil.parseJson(json, NgsModule.class);
	}

	public String toJson() {
		return GsonUtil.toJson(this);
	}

	public void save(File externalDir) {
		try {
			FileUtil.writeStringToFile(new File(externalDir, NGS_MODULE_FILE_NAME), toJson());
		} catch (IOException e) {
			e.printStackTrace();
			assert(false);
		}
	}

	public String getAADbFileName() {
		return NGS_MODULE_AA_VIRUSES_DB;
	}

	public String getAaFileName() {
		return aaFileName;
	}

	public void setAaFileName(String aaFileName) {
		this.aaFileName = aaFileName;
	}

	public String getTaxonomyFileName() {
		return taxonomyFileName;
	}

	public void setTaxonomyFileName(String taxonomyFileName) {
		this.taxonomyFileName = taxonomyFileName;
	}

	public String getNcbiVirusesFileName() {
		return ncbiVirusesFileName;
	}

	public void setNcbiVirusesFileName(String ncbiVirusesFileName) {
		this.ncbiVirusesFileName = ncbiVirusesFileName;
	}
}
