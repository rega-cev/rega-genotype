package rega.genotype.ngs.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import rega.genotype.utils.FileUtil;

/**
 * Represents all the data from ngs-results.xml
 * 
 * @author michael
 */
public class NgsResultsModel {
	public enum State {
		Init(0, "Init"),
		QC(1, "QC"),
		Preprocessing(2, "Preprocessing"),
		QC2(3, "QC of preprocessed."),
		Diamond(4, "Filtering"),
		Spades(5, "Assembly and Identification"),
		FinishedAll(6, "Finished");

		public final int code;
		public final String text;
		State(int code, String text) {
			this.code = code;
			this.text = text;
		}
	}

	private State state = State.Init;
	private Map<State, Long> stateStartTimeInMiliseconds = new TreeMap<State, Long>();

	private String errors = new String();
	private List<String> spadesErrors = new ArrayList<String>(); // spades can crash on files with small amount of sequences, in that case it is still good to check the other viruses.
	private String fastqPE1FileName;// File with forward reads.
	private String fastqPE2FileName;// File with reverse reads.
	private String fastqSEFileName; // File with interlaced forward and reverse paired-end reads.
	private Boolean skipPreprocessing = false;

	private Integer readLength = null;
	private Integer readCountInit = null;
	private Integer readCountAfterPrepocessing = null;

	private Map<String, DiamondBucket> diamondBlastResults = new HashMap<String, DiamondBucket>();// count sequences per taxon.
	private Map<String, DiamondBucket> diamondBlastResultsBeforeMerge = new HashMap<String, DiamondBucket>();// count sequences per taxon.
	private List<ConsensusBucket> consensusBuckets = new ArrayList<ConsensusBucket>();

	/**
	 * @return The current running state. So the previous state was written.
	 */
	public State getState() {
		return state;
	}
	public Long getStateStartTime(State state) {
		return stateStartTimeInMiliseconds.get(state);
	}
	public void setStateTime(State state, Long time) {
		stateStartTimeInMiliseconds.put(state, time);
	}
	public void readStateTime(State state, Long time) {
		this.state = state;
		stateStartTimeInMiliseconds.put(state, time);
	}
	/**
	 * @return total read count at the beginning of state or null if the state is finished.
	 */
	public Integer getReadCountStartState(State state) {
		switch (state) {
		case Diamond:
			return skipPreprocessing ? readCountInit : readCountAfterPrepocessing;
		case FinishedAll:
			break; // TODO, unused
		case Init:
			return readCountInit;
		case Preprocessing:
			return readCountInit;
		case QC:
			return readCountInit;
		case QC2:
			return skipPreprocessing ? readCountInit : readCountAfterPrepocessing ;
		case Spades:
			return totalReadCountAfterfiltering();
		}

		return null;
	}
	public Integer totalReadCountAfterfiltering() {
		if (diamondBlastResults.isEmpty())
			return null;

		Integer ans = 0;
		for (DiamondBucket b :diamondBlastResults.values())
			ans += b.getReadCountTotal();

		return ans;
	}
	public String getErrors() {
		return errors;
	}
	public void setErrors(String errors) {
		this.errors = errors;
	}
	public List<String> getSpadesErrors() {
		return spadesErrors;
	}
	public void setSpadesErrors(List<String> spadesErrors) {
		this.spadesErrors = spadesErrors;
	}

	public String getInputName() {
		if (isPairEnd()) {
			if (fastqPE1FileName != null)
				return FileUtil.removeExtention(fastqPE1FileName);
		} else if (fastqSEFileName != null){
			return FileUtil.removeExtention(fastqSEFileName);
		}

		return "";
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
	public Boolean getSkipPreprocessing() {
		return skipPreprocessing;
	}
	public void setSkipPreprocessing(Boolean skipPreprocessing) {
		this.skipPreprocessing = skipPreprocessing;
	}
	public Integer getReadLength() {
		return readLength;
	}
	public void setReadLength(Integer readLength) {
		this.readLength = readLength;
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
	public Map<String, DiamondBucket> getDiamondBlastResults() {
		return diamondBlastResults;
	}
	public void setDiamondBlastResults(Map<String, DiamondBucket> diamondBlastResults) {
		this.diamondBlastResults = diamondBlastResults;
	}
	public Map<String, DiamondBucket> getDiamondBlastResultsBeforeMerge() {
		return diamondBlastResultsBeforeMerge;
	}
	public void setDiamondBlastResultsBeforeMerge(
			Map<String, DiamondBucket> diamondBlastResultsBeforeMerge) {
		this.diamondBlastResultsBeforeMerge = diamondBlastResultsBeforeMerge;
	}
	public List<ConsensusBucket> getConsensusBuckets() {
		return consensusBuckets;
	}

	public boolean isPairEnd() {
		return fastqSEFileName == null;
	}
}
