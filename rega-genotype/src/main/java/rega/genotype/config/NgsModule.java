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

	public static final String NGS_MODULE_TRIMMOMATIC_JAR = "trimmomatic.jar";
	public static final String NGS_MODULE_FASTQC_FILE = "fastqc";
	public static final String NGS_MODULE_ADAPTERS_FILE = "adapters.fasta";

	// internal to ngs module, used to create AA database for diamond blast step (NGS_MODULE_AA_VIRUSES_DB)
	private String aaFileName = null; 
	private String taxonomyFileName = null;

	private String ncbiVirusesFileName = null;

	private String consensusToolMaxGap = "10";
	private String consensusToolMaxMissing = "100";
	private String consensusToolMinCount = "10";
	private String consensusToolMixtureMinPct = "20";
	private String consensusToolAbsoluteCutoff = "70";
	private String consensusToolMinSingleSeqCov = "4";
	private String diamondOptions = "-e 0.01";
	private String spadesOptions = "";
	private int minRefContigLength = 400;
	private double refMaxBlastEValue = 1E-5;
	private double refMinBlastBitScore = 50;
	private String consensusToolRelativeCutoff = "1";

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

	public static File adaptersFilePath(File modulePath) {
		return new File(modulePath, NGS_MODULE_ADAPTERS_FILE);
	}

	public static File trimmomaticPath(File modulePath) {
		return new File(modulePath, NGS_MODULE_TRIMMOMATIC_JAR);
	}

	public static File fastQCpath(File modulePath) {
		return new File(modulePath, NGS_MODULE_FASTQC_FILE);
	}

	public String getConsensusToolMaxGap() {
		return consensusToolMaxGap;
	}

	public void setConsensusToolMaxGap(String consensusToolMaxGap) {
		this.consensusToolMaxGap = consensusToolMaxGap;
	}

	public String getConsensusToolMaxMissing() {
		return consensusToolMaxMissing;
	}

	public void setConsensusToolMaxMissing(String consensusToolMaxMissing) {
		this.consensusToolMaxMissing = consensusToolMaxMissing;
	}

	public String getConsensusToolMinCount() {
		return consensusToolMinCount;
	}

	public void setConsensusToolMinCount(String consensusToolMinCount) {
		this.consensusToolMinCount = consensusToolMinCount;
	}

	public String getConsensusToolMixtureMinPct() {
		return consensusToolMixtureMinPct;
	}

	public void setConsensusToolMixtureMinPct(String consensusToolMixtureMinPct) {
		this.consensusToolMixtureMinPct = consensusToolMixtureMinPct;
	}

	public void setConsensusToolAbsoluteCutoff(String consensusToolAbsoluteCutoff) {
		this.consensusToolAbsoluteCutoff = consensusToolAbsoluteCutoff;
	}

	public String getConsensusToolAbsoluteCutoff() {
		return consensusToolAbsoluteCutoff;
	}

	public void setConsensusToolRelativeCutoff(String consensusToolRelativeCutoff) {
		this.consensusToolRelativeCutoff = consensusToolRelativeCutoff;
	}

	public String getConsensusToolRelativeCutoff() {
		return consensusToolRelativeCutoff;
	}

	public String getConsensusToolMinSingleSeqCov() {
		return consensusToolMinSingleSeqCov;
	}

	public void setConsensusToolMinSingleSeqCov(
			String consensusToolMinSingleSeqCov) {
		this.consensusToolMinSingleSeqCov = consensusToolMinSingleSeqCov;
	}

	public String getDiamondOptions() {
		return diamondOptions;
	}

	public void setDiamondOptions(String diamondOptions) {
		this.diamondOptions = diamondOptions;
	}

	public String getSpadesOptions() {
		return spadesOptions;
	}

	public void setSpadesOptions(String spadesOptions) {
		this.spadesOptions = spadesOptions;
	}

	public int getRefMinContigLength() {
		return minRefContigLength;
	}
	
	public void setRefMinContigLength(int minRefContigLength) {
		this.minRefContigLength = minRefContigLength;
	}

	public double getRefMaxBlastEValue() {
		return refMaxBlastEValue;
	}
	
	public void setRefMaxBlastEValue(double refMaxBlastEValue) {
		this.refMaxBlastEValue = refMaxBlastEValue;
	}

	public double getRefMinBlastBitScore() {
		return refMinBlastBitScore;
	}
	
	public void setRefMinBlastBitScore(double refMinBlastBitScore) {
		this.refMinBlastBitScore = refMinBlastBitScore;
	}
}
