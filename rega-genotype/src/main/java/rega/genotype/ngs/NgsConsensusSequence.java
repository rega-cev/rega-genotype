//package rega.genotype.ngs;
//
//import java.util.List;
//
///**
// * Sequence tool has 2 results 1) consensus 2) contigs
// * NgsSequence represents the consensus and stores also some data about the contigs.
// * 
// * @author michael
// */
//public class NgsConsensusSequence {
//	private BucketData bucketData;
//	private List<Contig> contigs;
//	private int uniqueId;
//	private String name;
//	private String description;
//	private String sequence; // Nucleotides
//
//	public NgsConsensusSequence(int uniqueId, String name, String description,
//			String sequence, BucketData bucketData,
//			List<Contig> contigs) {
//		this.uniqueId = uniqueId;
//		this.name = name;
//		this.description = description;
//		this.sequence = sequence;
//		this.bucketData = bucketData;
//		this.contigs = contigs;	
//	}
//
//	public BucketData getBucketData() {
//		return bucketData;
//	}
//
//	public void setBucketData(BucketData bucketData) {
//		this.bucketData = bucketData;
//	}
//
//	public List<Contig> getContigs() {
//		return contigs;
//	}
//
//	public void setContigs(List<Contig> contigs) {
//		this.contigs = contigs;
//	}
//
//	public int getUniqueId() {
//		return uniqueId;
//	}
//
//	public void setUniqueId(int uniqueId) {
//		this.uniqueId = uniqueId;
//	}
//
//	public String getName() {
//		return name;
//	}
//
//	public void setName(String name) {
//		this.name = name;
//	}
//
//	public String getDescription() {
//		return description;
//	}
//
//	public void setDescription(String description) {
//		this.description = description;
//	}
//
//	public String getSequence() {
//		return sequence;
//	}
//
//	public void setSequence(String sequence) {
//		this.sequence = sequence;
//	}
//
//	// classes
//	public static class Contig {
//		private Double cov;
//		private Integer length;
//		private String id;
//
//		//<contig id="1" length="7366" cov="5.35085">
//		public Contig(String id, Integer length, Double cov){
//			this.id = id;
//			this.length = length;
//			this.cov = cov;	
//		}
//		public String getId() {
//			return id;
//		}
//		public void setId(String id) {
//			this.id = id;
//		}
//		public Double getCov() {
//			return cov;
//		}
//		public void setCov(Double cov) {
//			this.cov = cov;
//		}
//		public Integer getLength() {
//			return length;
//		}
//		public void setLength(Integer length) {
//			this.length = length;
//		}
//	}
//
//	public static class BucketData {
//		private String diamondBucket;
//		private String refName;
//		private String refDescription;
//		private int refLen;
//
//		public BucketData(String diamondBucket, String refName,
//				String refDescription, int refLen) {
//			this.diamondBucket = diamondBucket;
//			this.refName = refName;
//			this.refDescription = refDescription;
//			this.refLen = refLen;
//		}
//
//		public String getDiamondBucket() {
//			return diamondBucket;
//		}
//
//		public void setDiamondBucket(String diamondBucket) {
//			this.diamondBucket = diamondBucket;
//		}
//
//		public String getRefName() {
//			return refName;
//		}
//
//		public void setRefName(String refName) {
//			this.refName = refName;
//		}
//
//		public String getRefDescription() {
//			return refDescription;
//		}
//
//		public void setRefDescription(String refDescription) {
//			this.refDescription = refDescription;
//		}
//
//		public int getRefLen() {
//			return refLen;
//		}
//
//		public void setRefLen(int refLen) {
//			this.refLen = refLen;
//		}
//	}
//}
