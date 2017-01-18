package rega.genotype;

import java.util.List;


/**
 * NgsSequence sequence contains more data:
 * Cov, reference sequence that was used to assemble it, what bucket it came from.
 * 
 * Sequence tool has 2 results 1) consensus 2) contigs
 * NgsSequence represents the consensus and stores also some data about the contigs.
 * 
 * @author michael
 */
public class NgsSequence extends Sequence {
	private BucketData bucketData;
	private List<Contig> contigs;

	public NgsSequence(String name, boolean nameCapped, String description,
			String sequence, String quality, 
			BucketData bucketData, List<Contig> contigs) {
		super(name, nameCapped, description, sequence, quality);
		this.bucketData = bucketData;
		this.contigs = contigs;	
	}

	public void writeSequenceMetadata(ResultTracer resultTracer) {
		resultTracer.increaseIndent();
		resultTracer.add("diamond_bucket", bucketData.getDiamondBucket());
		resultTracer.add("ref_length", bucketData.getRefLen());
		resultTracer.add("ref_description", bucketData.getRefDescription());
		resultTracer.add("ref_name", bucketData.getRefName());
		resultTracer.println("<contigs>");
		resultTracer.increaseIndent();
		for (Contig contig: contigs) {
			resultTracer.println("<contig id=\"" + contig.id + "\">");
			resultTracer.increaseIndent();
			resultTracer.add("length", contig.length);
			resultTracer.add("cov", contig.cov);
			resultTracer.add("nucleotides", contig.sequence);
			resultTracer.decreaseIndent();
			resultTracer.println("</contig>");
		}
		resultTracer.decreaseIndent();
		resultTracer.println("</contigs>");
		resultTracer.decreaseIndent();
	}

	public BucketData getBucketData() {
		return bucketData;
	}

	public void setBucketData(BucketData bucketData) {
		this.bucketData = bucketData;
	}

	public List<Contig> getContigs() {
		return contigs;
	}

	public void setContigs(List<Contig> contigs) {
		this.contigs = contigs;
	}

	// classes
	public static class Contig {
		private Double cov;
		private Integer length;
		private String id;
		private String sequence; //nucleotides. 

		//<contig id="1" length="7366" cov="5.35085">
		public Contig(String id, Integer length, Double cov, String sequence){
			this.id = id;
			this.length = length;
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
	}

	public static class BucketData {
		private String diamondBucket;
		private String refName;
		private String refDescription;
		private int refLen;

		public BucketData(String diamondBucket, String refName,
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
	}
}
