package rega.genotype.ngs.model;

import rega.genotype.AbstractSequence;

/**
 * represent consensus-contigs (output of make consensus)
 * 
 * @author michael
 */
public class Contig extends AbstractSequence{
	private Double cov;
	private int length;
	private Integer endPosition;
	private Integer startPosition;
	private String id;
	private String sequence; //nucleotides. 

	//<contig id="1" length="7366" cov="5.35085">
	public Contig(String id, int length, Integer startPosition, 
			Integer endPosition, Double cov, String sequence){
		this.id = id;
		this.length = length;
		this.startPosition = startPosition;
		this.endPosition = endPosition;
		this.cov = cov;
		this.sequence = sequence;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Double getCov() {
		return cov;
	}
	public void setCov(Double cov) {
		this.cov = cov;
	}
	public int getLength() {
		return length;
	}
	public void setLength(Integer length) {
		this.length = length;
	}
	public String getSequence() {
		return sequence;
	}
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
	public Integer getEndPosition() {
		return endPosition;
	}
	public void setEndPosition(Integer endPosition) {
		this.endPosition = endPosition;
	}
	public Integer getStartPosition() {
		return startPosition;
	}
	public void setStartPosition(Integer startPosition) {
		this.startPosition = startPosition;
	}

	public double getReadCount(int readLength) {
		return (cov * length) / readLength;
	}

	public double getCovPercentage(int refLength) {
		return (double)length / (double)refLength  * (double)100;
	}

	public double getDeepCov(int readLength) {
		return getReadCount(readLength) * (double)readLength / length;
	}
	@Override
	public String getName() {
		return "Contig" + id;
	}
	@Override
	public void setName(String name) {
		throw new RuntimeException("The contig name is computed.");
	}
	@Override
	public boolean isNameCapped() {
		return false;
	}
	@Override
	public String getDescription() {
		return "length_" + length + "__cov_" + cov ;
	}
	@Override
	public String getQuality() {
		return null;
	}
	@Override
	public void removeChar(int i) {
	}
	@Override
	public AbstractSequence sourceSequence() {
		return null;
	}
}
