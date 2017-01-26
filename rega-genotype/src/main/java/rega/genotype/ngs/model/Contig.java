package rega.genotype.ngs.model;

/**
 * represent consensus-contigs (output of make consensus)
 * 
 * @author michael
 */
public class Contig {
	private Double cov;
	private Integer length;
	private Integer endPosition;
	private String id;
	private String sequence; //nucleotides. 

	//<contig id="1" length="7366" cov="5.35085">
	public Contig(String id, Integer length, Integer endPosition, Double cov, String sequence){
		this.id = id;
		this.length = length;
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
	public Integer getLength() {
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
}
