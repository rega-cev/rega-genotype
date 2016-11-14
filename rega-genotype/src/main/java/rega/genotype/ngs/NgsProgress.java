package rega.genotype.ngs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import rega.genotype.utils.FileUtil;
import rega.genotype.utils.GsonUtil;

/**
 * JSON file that monitors NGS analysis state.
 * Used by NgsWidget.
 * 
 * @author michael
 */
public class NgsProgress {
	public static final String NGS_PROGRESS_FILL = "ngs_progress";

	public enum State {
		Init(0, "uploading"),
		QC(1, "runing QC"),
		Preprocessing(2, "runing preprocessing"),
		QC2(3, "runing QC of preprocessed."),
		Diamond(4, "runing diamond blast"),
		Spades(5, "runing spades"),
		FinishedAll(6, "finished");

		public final int code;
		public final String text;
		State(int code, String text) {
			this.code = code;
			this.text = text;
		}
	}

	// Analysis variables.
	private State state = State.Init;
	private String errors = new String();
	private List<String> spadesErrors = new ArrayList<String>(); // spades can crash on files with small amount of sequences, in that case it is still good to check the other viruses.
	private String fastqPE1FileName;// File with forward reads.
	private String fastqPE2FileName;// File with reverse reads.
	private String fastqSEFileName; // File with interlaced forward and reverse paired-end reads.
	private Map<String, Integer> diamondBlastResults = new TreeMap<String, Integer>();// count sequences per taxon.

	public NgsProgress() {}

	public static NgsProgress read(File workDir) {
		File ngsProgressFile = new File(workDir, NGS_PROGRESS_FILL);
		if (!ngsProgressFile.exists())
			return null;

		String json = FileUtil.readFile(ngsProgressFile);
		return parseJson(json);
	}

	public static NgsProgress parseJson(String json) {
		if (json == null)
			return null;

		return GsonUtil.parseJson(json, NgsProgress.class);
	}

	public String toJson() {
		return GsonUtil.toJson(this);
	}

	public void save(File workDir) {
		try {
			FileUtil.writeStringToFile(new File(workDir ,NGS_PROGRESS_FILL), toJson());
		} catch (IOException e) {
			e.printStackTrace();
			assert(false);
		}
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public String getErrors() {
		return errors;
	}

	public void setErrors(String errors) {
		this.errors = errors;
	}

	public String getFastqPE1FileName() {
		return fastqPE1FileName;
	}

	public void setFastqPE1FileName(String fastqPE1FileName) {
		this.fastqPE1FileName = fastqPE1FileName;
	}

	public String getFastqPE2FileName() {
		return fastqPE2FileName;
	}

	public void setFastqPE2FileName(String fastqPE2FileName) {
		this.fastqPE2FileName = fastqPE2FileName;
	}

	public String getFastqSEFileName() {
		return fastqSEFileName;
	}

	public void setFastqSEFileName(String fastqSEFileName) {
		this.fastqSEFileName = fastqSEFileName;
	}

	public List<String> getSpadesErrors() {
		return spadesErrors;
	}

	public void setSpadesErrors(List<String> spadesErrors) {
		this.spadesErrors = spadesErrors;
	}

	public Map<String, Integer> getDiamondBlastResults() {
		return diamondBlastResults;
	}

	public void setDiamondBlastResults(Map<String, Integer> diamondBlastResults) {
		this.diamondBlastResults = diamondBlastResults;
	}
}