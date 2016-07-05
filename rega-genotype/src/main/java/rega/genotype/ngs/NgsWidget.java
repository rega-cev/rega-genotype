package rega.genotype.ngs;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import rega.genotype.ngs.NgsProgress.State;

import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WText;

public class NgsWidget extends WContainerWidget{

	public NgsWidget(File workDir) {
		super();

		NgsProgress ngsProgress = NgsProgress.read(workDir);
		if (ngsProgress == null)
			return;

		new WText("NGS state is " + ngsProgress.getState(), this);

		if (ngsProgress.getState().code >= State.QcFinished.code) {
			File qcDir = new File(workDir, NgsAnalysis.QC_REPORT_DIR);

			for (File f: qcDir.listFiles()) {
				if (FilenameUtils.getExtension(f.getAbsolutePath()).equals("html")){
					WContainerWidget c = new WContainerWidget(this);
					new WText("QC report for ", c);
					WFileResource r = new WFileResource("html", f.getAbsolutePath());
					WLink link = new WLink(r);
					link.setTarget(AnchorTarget.TargetNewWindow);
					c.addWidget(new WAnchor(link, f.getName()));
				}
			}
		}
	}
}