package rega.genotype.ui.ngs;

import java.io.File;
import java.util.List;

import rega.genotype.ngs.NgsProgress;
import rega.genotype.ngs.NgsProgress.SequenceMetadata;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WText;

public class CoverageWidget extends WContainerWidget{

	public CoverageWidget(File jobDir, List<String> sequenceNames) {
		
		// TODO temp stub remove before pushing to master !!!!!!!!!!!!!!!!!!
		WImage covImage = new WImage(new WFileResource("image", 
				"/home/michael/projects/rega-genotype-my-docs/ngs visualization/cov_img.png"),
				"TEST Image");

		covImage.resize(220*3, 80*3);
		addWidget(covImage);

		WContainerWidget downloadscContainer = new WContainerWidget(this);

		new WText("Downloads: ", downloadscContainer);

		NgsProgress ngsProgress = NgsProgress.read(jobDir);

		// TODO: assume that all sequnces are from the same virus (this will always be true when we add NCBI database reference sequences)
		SequenceMetadata sequenceMetadata = ngsProgress.getSequenceMetadata().get(sequenceNames.get(0));

		createAnchor("Consensus (fasta)", sequenceMetadata.consensusFile, downloadscContainer);
		createAnchor("Contigs (fasta)", sequenceMetadata.contigsFile, downloadscContainer);
		//TODO: sam bam files 
		createAnchor("Contigs (SAM)", new File("TODO! sam bam files "), downloadscContainer); // TODO
		createAnchor("Contigs (BAM)", new File("TODO! sam bam files "), downloadscContainer); // TODO
	}

	private WAnchor createAnchor(String text, File file, WContainerWidget parent) {
		WAnchor a = new WAnchor(parent);
		WLink l = new WLink(new WFileResource("text", file.getAbsolutePath()));
		l.setTarget(AnchorTarget.TargetNewWindow);
		a.setLink(l);
		a.setText(text);
		a.setInline(true);
		a.setMargin(10, Side.Right);
		return a;
	}
}
