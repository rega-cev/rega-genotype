package rega.genotype.ui.admin.file_editor.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import rega.genotype.AlignmentAnalyses;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.BlastAnalysis;
import rega.genotype.BlastAnalysis.ReferenceTaxus;
import rega.genotype.BlastAnalysis.Region;
import rega.genotype.SequenceAlignment;
import rega.genotype.ui.framework.exeptions.RegaGenotypeExeption;

public class BlastXmlWriter {

	public BlastXmlWriter(File blastFile, AlignmentAnalyses alignmentAnalyses) throws IOException, RegaGenotypeExeption {

		if (alignmentAnalyses.getAllClusters().isEmpty())
			throw new RegaGenotypeExeption("Clusters must not be empty.");

		Element analyses = new Element("genotype-analyses");
		Document doc = new Document(analyses);
		doc.setRootElement(analyses);

		{   // alignment
			Element alignmentE = new Element("alignment");
			List<Attribute> attributes = new ArrayList<Attribute>();
			attributes.add(new Attribute("file", "blast.fasta"));
			attributes.add(new Attribute("data", SequenceAlignment.sequenceTypeName(
					alignmentAnalyses.getAlignment().getSequenceType())));
			alignmentE.setAttributes(attributes);
			doc.getRootElement().addContent(alignmentE);
		}
		{   // cluster
			Element clustersE = new Element("clusters");
			for (Cluster cluster: alignmentAnalyses.getAllClusters()) {
				Element clusterE = new Element("cluster");
				List<Attribute> attributes = new ArrayList<Attribute>();
				attributes.add(new Attribute("id", cluster.getId()));
				attributes.add(new Attribute("name", cluster.getName()));
				clusterE.setAttributes(attributes);
				add(clusterE, "description", cluster.getDescription());
				add(clusterE, "tool-id", cluster.getToolId());
				for (String taxaId: cluster.getTaxaIds()){
					Element taxusE = new Element("taxus");
					taxusE.setAttribute(new Attribute("name", taxaId));
					clusterE.addContent(taxusE);
				}
				clustersE.addContent(clusterE);
			}
			doc.getRootElement().addContent(clustersE);
		}
		{   // analysis
			BlastAnalysis analysis = (BlastAnalysis) alignmentAnalyses.getAnalysis("blast");

			Element analysisE = new Element("analysis");
			List<Attribute> attributes = new ArrayList<Attribute>();
			attributes.add(new Attribute("id", "blast"));
			attributes.add(new Attribute("type", "blast"));
			analysisE.setAttributes(attributes);
			add(analysisE, "identify", analysis.getId());
			add(analysisE, "options", analysis.getBlastOptions());
			
			if (analysis.getAbsCutoff() != null)
				add(analysisE, "absolute-cutoff", analysis.getAbsCutoff());
			if (analysis.getRelativeCutoff() != null)
				add(analysisE, "relative-cutoff", analysis.getRelativeCutoff());
			if (analysis.getAbsMaxEValue() != null)
				add(analysisE, "absolute-max-e-value", analysis.getAbsMaxEValue());
			if (analysis.getRelativeMaxEValue() != null)
				add(analysisE, "relative-max-e-value", analysis.getRelativeMaxEValue());

			// Reference Taxus
			for (ReferenceTaxus ref: analysis.getReferenceTaxus()){
				Element referenceTaxusE = new Element("regions");
				List<Attribute> refTaxusAttributes = new ArrayList<Attribute>();
				refTaxusAttributes.add(new Attribute("taxus", ref.getTaxus()));
				referenceTaxusE.setAttributes(refTaxusAttributes);

				for (Region region: ref.getRegions()) {
					Element regionE = new Element("region");
					List<Attribute> regionAttributes = new ArrayList<Attribute>();
					regionAttributes.add(new Attribute("name", region.getName()));
					regionAttributes.add(new Attribute("begin", "" + region.getBegin()));
					regionAttributes.add(new Attribute("end", "" + region.getEnd()));
					regionE.setAttributes(regionAttributes);
					referenceTaxusE.addContent(regionE);
				}
				analysisE.addContent(referenceTaxusE);
			}

			doc.getRootElement().addContent(analysisE);
		}

		// new XMLOutputter().output(doc, System.out);
		XMLOutputter xmlOutput = new XMLOutputter();

		// display nice nice
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(doc, new FileWriter(blastFile));
	}

    public void add(Element e, String tag, String value) {
		e.addContent(new Element(tag).setText(value));
    }
    
    public void add(Element e, String tag, float value) {
        add(e, tag, String.valueOf(Math.round(value * 1000)/1000.));
    }

    public void add(Element e, String tag, double value) {
        add(e, tag, String.valueOf(Math.round(value * 1000)/1000.));
    }

    public void add(Element e, String tag, int value) {
        add(e, tag, Integer.toString(value));
    }
}
