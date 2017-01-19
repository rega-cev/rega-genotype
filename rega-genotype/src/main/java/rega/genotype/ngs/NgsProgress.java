package rega.genotype.ngs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
		Init(0, "Init"),
		QC(1, "QC"),
		Preprocessing(2, "Preprocessing"),
		QC2(3, "QC of preprocessed."),
		Diamond(4, "Filtring"),
		Spades(5, "Assembly and Identification"),
		FinishedAll(6, "finished");

		public final int code;
		public final String text;
		State(int code, String text) {
			this.code = code;
			this.text = text;
		}
	}

	public static class BasketData {
		public BasketData(String scientificName, String ancestors, Integer readCountTotal) {
			this.setReadCountTotal(readCountTotal);
			this.setScientificName(scientificName);
			this.setAncestors(ancestors);
		}
		private String ancestors = null;
		private String scientificName = null;
		private Integer readCountTotal = null;
		private Integer readCountAfterMakeConsensus = null;

		public String getScientificName() {
			return scientificName;
		}
		public void setScientificName(String scientificName) {
			this.scientificName = scientificName;
		}
		public Integer getReadCountTotal() {
			return readCountTotal;
		}
		public void setReadCountTotal(Integer readCountTotal) {
			this.readCountTotal = readCountTotal;
		}
		public Integer getReadCountAfterMakeConsensus() {
			return readCountAfterMakeConsensus;
		}
		public void setReadCountAfterMakeConsensus(
				Integer readCountAfterMakeConsensus) {
			this.readCountAfterMakeConsensus = readCountAfterMakeConsensus;
		}
		public String getAncestors() {
			return ancestors;
		}
		public void setAncestors(String ancestors) {
			this.ancestors = ancestors;
		}
	}

	// Analysis variables.
	private State state = State.Init;
	private String errors = new String();
	private List<String> spadesErrors = new ArrayList<String>(); // spades can crash on files with small amount of sequences, in that case it is still good to check the other viruses.
	private String fastqPE1FileName;// File with forward reads.
	private String fastqPE2FileName;// File with reverse reads.
	private String fastqSEFileName; // File with interlaced forward and reverse paired-end reads.
	private Boolean skipPreprocessing = false;

	private Map<State, Long> stateStartTimeInMiliseconds = new TreeMap<NgsProgress.State, Long>();

	private Integer readLength = null;
	private Integer readCountInit = null;
	private Integer readCountAfterPrepocessing = null;

	private Map<String, BasketData> diamondBlastResults = new HashMap<String, BasketData>();// count sequences per taxon.
	private Map<String, BasketData> diamondBlastResultsBeforeMerge = new HashMap<String, BasketData>();// count sequences per taxon.

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
		stateStartTimeInMiliseconds.put(state, System.currentTimeMillis());
	}

	public Long getStateStartTime(State state) {
		return stateStartTimeInMiliseconds.get(state);
	}

	/**
	 * @return total read count at the beginning of state or null if the state is finished.
	 */
	public Integer getReadCountStartState(State state) {
		switch (state) {
		case Diamond:
			return readCountAfterPrepocessing;
		case FinishedAll:
			break; // TODO, unused
		case Init:
			return readCountInit;
		case Preprocessing:
			return readCountInit;
		case QC:
			return readCountInit;
		case QC2:
			return readCountAfterPrepocessing;
		case Spades:
			return totalReadCountAfterFiltring();
		}

		return null;
	}

	public Integer totalReadCountAfterFiltring() {
		if (diamondBlastResults.isEmpty())
			return null;

		Integer ans = 0;
		for (BasketData b :diamondBlastResults.values())
			ans += b.readCountTotal;

		return ans;
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

	public Map<String, BasketData> getDiamondBlastResults() {
		return diamondBlastResults;
	}

	public void setDiamondBlastResults(Map<String, BasketData> diamondBlastResults) {
		this.diamondBlastResults = diamondBlastResults;
	}

	public Boolean getSkipPreprocessing() {
		return skipPreprocessing;
	}

	public void setSkipPreprocessing(Boolean skipPreprocessing) {
		this.skipPreprocessing = skipPreprocessing;
	}

	public Integer getReadCountInit() {
		return readCountInit;
	}

	public void setReadCountInit(Integer readCountInit) {
		this.readCountInit = readCountInit;
	}

	public Integer getReadCountAfterPrepocessing() {
		return readCountAfterPrepocessing;
	}

	public void setReadCountAfterPrepocessing(Integer readCountAfterPrepocessing) {
		this.readCountAfterPrepocessing = readCountAfterPrepocessing;
	}

	public Map<String, BasketData> getDiamondBlastResultsBeforeMerge() {
		return diamondBlastResultsBeforeMerge;
	}

	public void setDiamondBlastResultsBeforeMerge(
			Map<String, BasketData> diamondBlastResultsBeforeMerge) {
		this.diamondBlastResultsBeforeMerge = diamondBlastResultsBeforeMerge;
	}

	public Integer getReadLength() {
		return readLength;
	}

	public void setReadLength(Integer readLength) {
		this.readLength = readLength;
	}
}