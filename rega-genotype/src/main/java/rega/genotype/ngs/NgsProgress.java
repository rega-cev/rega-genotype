package rega.genotype.ngs;

import java.io.File;
import java.io.IOException;

import rega.genotype.utils.FileUtil;
import rega.genotype.utils.GsonUtil;

public class NgsProgress {
	public static final String NGS_PROGRESS_FILL = "ngs_progress";

	public enum State {
		UploadStarted(0),
		FastQ_File_Uploaded(1),
		QcFinished(2),
		PreprocessingFinished(3),
		Qc2Finished(4),
		DiamondFinished(5),
		SpadesFinished(6),
		FinishedAll(7);

		public final int code;
		State(int code) {
			this.code = code;
		}
	}

	private State state = State.UploadStarted;
	private String errors = new String();
	private String fastqPE1FileName;// File with forward reads.
	private String fastqPE2FileName;// File with reverse reads.
	private String fastqSEFileName; // File with interlaced forward and reverse paired-end reads.

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
}