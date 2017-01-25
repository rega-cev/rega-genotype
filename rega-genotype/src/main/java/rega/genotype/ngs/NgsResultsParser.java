package rega.genotype.ngs;

import java.util.Arrays;
import java.util.List;

import org.jdom.Element;

import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ngs.model.ConsensusBucket;
import rega.genotype.ngs.model.Contig;
import rega.genotype.ngs.model.DiamondBucket;
import rega.genotype.ngs.model.NgsResultsModel;
import rega.genotype.ngs.model.NgsResultsModel.State;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.Signal;

public class NgsResultsParser extends GenotypeResultParser{

	private NgsResultsModel model;
	private Signal updateUiSignal = new Signal();

	public NgsResultsParser() {
		this.model = new NgsResultsModel();
		this.reportPaths = Arrays.asList(new String[]
				{"/genotype_result", "/genotype_result/assembly"});
		setReaderBlocksOnEof(true);
	}

	@Override
	public void updateUi() {
		updateUiSignal.trigger();
	}

	public Signal updateUiSignal() {
		return updateUiSignal;
	}

	@Override
	public void endReportElement(String tag) {
		if (tag.equals("init")) {
			model.setFastqPE1FileName(GenotypeLib.getEscapedValue(this,
					"/genotype_result/init/pe-1-file"));
			model.setFastqPE2FileName(GenotypeLib.getEscapedValue(this,
					"/genotype_result/init/pe-2-file"));
			model.readStateTime(State.Init, 0L);
			model.readStateTime(State.QC, Long.parseLong(GenotypeLib.getEscapedValue(this,
					"/genotype_result/init/end-time-ms")));
		} else if (tag.equals("qc1")) {
			model.readStateTime(State.Preprocessing, Long.parseLong(
					GenotypeLib.getEscapedValue(this,
							"/genotype_result/qc1/end-time-ms")));
			model.setReadLength(Integer.parseInt(
					GenotypeLib.getEscapedValue(this,
							"/genotype_result/qc1/read-length")));
			model.setReadCountInit(Integer.parseInt(
					GenotypeLib.getEscapedValue(this,
							"/genotype_result/qc1/read-count")));
		} else if (tag.equals("preprocessing")) {
			model.readStateTime(State.QC2, Long.parseLong(
					GenotypeLib.getEscapedValue(this,
							"/genotype_result/preprocessing/end-time-ms")));
		} else if (tag.equals("qc2")) {
			model.readStateTime(State.Diamond, Long.parseLong(
					GenotypeLib.getEscapedValue(this,
							"/genotype_result/qc2/end-time-ms")));
			model.setReadLength(Integer.parseInt(
					GenotypeLib.getEscapedValue(this,
							"/genotype_result/qc2/read-length")));
			model.setReadCountAfterPrepocessing(Integer.parseInt(
					GenotypeLib.getEscapedValue(this,
							"/genotype_result/qc2/read-count")));
		} else if (tag.equals("filtring")) {
			Element filtringE = getElement("/genotype_result/filtring");
			model.readStateTime(State.Spades, Long.parseLong(filtringE.getChildText("end-time-ms")));

			List<Element> children = filtringE.getChildren("diamond-bucket");
			for (Element bucketE: children){
				String id = bucketE.getAttributeValue("id");
				String scientificName = bucketE.getChildText("scientific-name");
				String ancestors = bucketE.getChildText("ancestors");
				String readCountTotal = bucketE.getChildText("read-count-total");
				DiamondBucket diamondBucket = new DiamondBucket(
						id, scientificName, ancestors, Integer.parseInt(readCountTotal));
				model.getDiamondBlastResults().put(id, diamondBucket);
			}
		} else if (tag.equals("assembly")) {
			//assembly/bucket/
			Element assemblyE = getElement("/genotype_result/assembly");
			model.readStateTime(State.FinishedAll, Long.parseLong(assemblyE.getChildText("end-time-ms")));
		} else if (tag.equals("bucket")) {
			List<Element> elements = getElements("/genotype_result/assembly/bucket");
			Element bucketE = elements.get(elements.size() - 1);
			ConsensusBucket bucket = new ConsensusBucket();
			bucket.setDiamondBucket(bucketE.getChildText("diamond_bucket"));
			bucket.setRefName(bucketE.getChildText("ref_name"));
			bucket.setRefDescription(bucketE.getChildText("ref_description"));
			bucket.setRefLen(Integer.parseInt(bucketE.getChildText("ref_length")));

			Element contigsE = bucketE.getChild("contigs");
			List<Element> contigs = contigsE.getChildren("contig");
			for (Element c: contigs) {
				String id = c.getAttribute("id").getValue();
				int contigLen = Integer.parseInt(c.getChild("length").getValue());
				double contigCov = Double.parseDouble(c.getChild("cov").getValue());
				Contig contig = new Contig(id, contigLen, contigCov, null);
				bucket.getContigs().add(contig);
			}
			Element sequenceE = bucketE.getChild("sequence");
			if (sequenceE != null) {
				bucket.setConsensusName(sequenceE.getAttributeValue("name"));
				bucket.setConsensusDescription(sequenceE.getAttributeValue("description"));
				bucket.setConsensusLength(
						Integer.parseInt(sequenceE.getAttributeValue("length")));

				Element clusterE = sequenceE.getChild("result").getChild("cluster");

				bucket.setConcludedId(clusterE.getChildText("concluded-id"));
				bucket.setConcludedName(clusterE.getChildText("concluded-name"));
				bucket.setConcludedTaxonomyId(clusterE.getChildText("taxonomy-id"));
				bucket.setSrcDatabase(clusterE.getChildText("src"));

				model.getConsensusBuckets().add(bucket);
			}
		}
	}

	public NgsResultsModel getModel() {
		return model;
	}

	public void setModel(NgsResultsModel model) {
		this.model = model;
	}
}