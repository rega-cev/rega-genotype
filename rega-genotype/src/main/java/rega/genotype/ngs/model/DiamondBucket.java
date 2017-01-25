package rega.genotype.ngs.model;

/**
 * Diamond buckets all input reads to taxonomy level.
 * 
 * @author michael
 */
public class DiamondBucket {
	private String taxonId;

	public DiamondBucket(String taxonId, String scientificName, String ancestors, Integer readCountTotal) {
		this.taxonId = taxonId;
		this.setReadCountTotal(readCountTotal);
		this.setScientificName(scientificName);
		this.setAncestors(ancestors);
	}
	private String ancestors = null;
	private String scientificName = null;
	private Integer readCountTotal = null;

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
	public String getAncestors() {
		return ancestors;
	}
	public void setAncestors(String ancestors) {
		this.ancestors = ancestors;
	}
	public String getTaxonId() {
		return taxonId;
	}
	public void setTaxonId(String taxonId) {
		this.taxonId = taxonId;
	}
}
