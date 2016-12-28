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
	public static final String NGS_MODULE_AA_VIRUSES_DB = Config.TRANSIENT_DATABASES_FOLDER_NAME + File.separator + "aa-virus.dmnd";
	public static final String NGS_MODULE_AA_VIRUSES_FASTA = Config.TRANSIENT_DATABASES_FOLDER_NAME + File.separator + "aa-viruses.fasta";

	public static final String NGS_MODULE_TRIMMOMATIC_JAR = "trimmomatic.jar";
	public static final String NGS_MODULE_FASTQC_FILE = "fastqc";
	public static final String NGS_MODULE_ADAPTERS_FILE = "adapters.fasta";

	// internal to ngs module, used to create AA database for diamond blast step (NGS_MODULE_AA_VIRUSES_DB)
	private String aaFileName = null; 

	private int consensusToolMaxGap = 10;
	private int consensusToolMaxMissing = 100;
	private int consensusToolMinCount = 10;
	private int consensusToolMixtureMinPct = 20;
	private int consensusToolAbsoluteCutoff = 70;
	private int consensusToolMinSingleSeqCov = 4;
	private double consensusToolRelativeCutoff = 0.7;
	private String diamondOptions = "-e 0.001";
	private String spadesOptions = "";
	private int minRefContigLength = 400;
	private double refMaxBlastEValue = 1E-5;
	private double refMinBlastBitScore = 50;

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

	public static File adaptersFilePath(File modulePath) {
		return new File(modulePath, NGS_MODULE_ADAPTERS_FILE);
	}

	public static File trimmomaticPath(File modulePath) {
		return new File(modulePath, NGS_MODULE_TRIMMOMATIC_JAR);
	}

	public static File fastQCpath(File modulePath) {
		return new File(modulePath, NGS_MODULE_FASTQC_FILE);
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

	public int getConsensusToolMaxGap() {
		return consensusToolMaxGap;
	}

	public void setConsensusToolMaxGap(int consensusToolMaxGap) {
		this.consensusToolMaxGap = consensusToolMaxGap;
	}

	public int getConsensusToolMaxMissing() {
		return consensusToolMaxMissing;
	}

	public void setConsensusToolMaxMissing(int consensusToolMaxMissing) {
		this.consensusToolMaxMissing = consensusToolMaxMissing;
	}

	public int getConsensusToolMinCount() {
		return consensusToolMinCount;
	}

	public void setConsensusToolMinCount(int consensusToolMinCount) {
		this.consensusToolMinCount = consensusToolMinCount;
	}

	public int getConsensusToolMixtureMinPct() {
		return consensusToolMixtureMinPct;
	}

	public void setConsensusToolMixtureMinPct(int consensusToolMixtureMinPct) {
		this.consensusToolMixtureMinPct = consensusToolMixtureMinPct;
	}

	public int getConsensusToolAbsoluteCutoff() {
		return consensusToolAbsoluteCutoff;
	}

	public void setConsensusToolAbsoluteCutoff(int consensusToolAbsoluteCutoff) {
		this.consensusToolAbsoluteCutoff = consensusToolAbsoluteCutoff;
	}

	public int getConsensusToolMinSingleSeqCov() {
		return consensusToolMinSingleSeqCov;
	}

	public void setConsensusToolMinSingleSeqCov(int consensusToolMinSingleSeqCov) {
		this.consensusToolMinSingleSeqCov = consensusToolMinSingleSeqCov;
	}

	public double getConsensusToolRelativeCutoff() {
		return consensusToolRelativeCutoff;
	}

	public void setConsensusToolRelativeCutoff(double consensusToolRelativeCutoff) {
		this.consensusToolRelativeCutoff = consensusToolRelativeCutoff;
	}
}
