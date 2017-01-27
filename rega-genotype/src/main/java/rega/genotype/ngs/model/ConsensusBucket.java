package rega.genotype.ngs.model;

import java.util.ArrayList;
import java.util.List;

/**
 * make consensus is called on all contigs that came from spades and where best aligned 
 * with a given reference sequence.
 * 
 * The bucket contains 1 consensus of all spades contigs and a list of consensus contigs.
 * 
 * @author michael
 */
public class ConsensusBucket {
	private String diamondBucket;

	private String consensusName;
	private String consensusDescription;
	private Integer consensusLength;
	private String concludedTaxonomyId;
	private String concludedId;
	private String concludedName;
	private String srcDatabase;

	private String refName;
	private String refDescription;
	private int refLen;
	private List<Contig> contigs = new ArrayList<Contig>();

	public ConsensusBucket() {
		
	}
	public ConsensusBucket(String diamondBucket, String refName,
			String refDescription, int refLen) {
		this.diamondBucket = diamondBucket;
		this.refName = refName;
		this.refDescription = refDescription;
		this.refLen = refLen;
	}

	public String getDiamondBucket() {
		return diamondBucket;
	}

	public void setDiamondBucket(String diamondBucket) {
		this.diamondBucket = diamondBucket;
	}

	public String getRefName() {
		return refName;
	}

	public void setRefName(String refName) {
		this.refName = refName;
	}

	public String getRefDescription() {
		return refDescription;
	}

	public void setRefDescription(String refDescription) {
		this.refDescription = refDescription;
	}

	public int getRefLen() {
		return refLen;
	}

	public void setRefLen(int refLen) {
		this.refLen = refLen;
	}
	public List<Contig> getContigs() {
		return contigs;
	}
	public void setContigs(List<Contig> contigs) {
		this.contigs = contigs;
	}
	public String getConsensusName() {
		return consensusName;
	}
	public void setConsensusName(String consensusName) {
		this.consensusName = consensusName;
	}
	public String getConsensusDescription() {
		return consensusDescription;
	}
	public void setConsensusDescription(String consensusDescription) {
		this.consensusDescription = consensusDescription;
	}
	public String getConcludedTaxonomyId() {
		return concludedTaxonomyId;
	}
	public void setConcludedTaxonomyId(String concludedTaxonomyId) {
		this.concludedTaxonomyId = concludedTaxonomyId;
	}
	public String getConcludedId() {
		return concludedId;
	}
	public void setConcludedId(String concludedId) {
		this.concludedId = concludedId;
	}
	public String getConcludedName() {
		return concludedName;
	}
	public void setConcludedName(String concludedName) {
		this.concludedName = concludedName;
	}
	public String getSrcDatabase() {
		return srcDatabase;
	}
	public void setSrcDatabase(String srcDatabase) {
		this.srcDatabase = srcDatabase;
	}
	public Integer getConsensusLength() {
		return consensusLength;
	}
	public void setConsensusLength(Integer consensusLength) {
		this.consensusLength = consensusLength;
	}

	public double getTotalContigsLen(){
		double contigsLen = 0;
		for (Contig contig: getContigs()) 
			contigsLen += contig.getLength();
		return contigsLen;
	}

	/**
	 * We can only estimate read count based on counting how much reads where 
	 * used for every contig.
	 * @param readLength - ngs reads have constant size depending on the machine.
	 * @return
	 */
	public double getTotalReadCount(int readLength){
		double readCount = 0;
		for (Contig contig: getContigs()) 
			readCount += contig.getReadCount(readLength);
		return readCount;
	}

	public double getDeepCov(int readLength){
		return getTotalReadCount(readLength) * 
				(double)readLength / getTotalContigsLen();
	}

	public double getCovPercentage() {
		return getTotalContigsLen() / getRefLen()  * 100;

	}
}
