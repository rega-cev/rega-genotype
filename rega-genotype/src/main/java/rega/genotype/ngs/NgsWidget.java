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

/**
 * Present ngs analysis state in job overview tab. 
 * 
 * @author michael
 */
public class NgsWidget extends WContainerWidget{

	public NgsWidget(File workDir) {
		super();

		NgsProgress ngsProgress = NgsProgress.read(workDir);
		if (ngsProgress == null)
			return;

		new WText("NGS state is " + ngsProgress.getState(), this);
		if (!ngsProgress.getErrors().isEmpty() )
			new WText("<div>Error: " + ngsProgress.getErrors() + "</div>", this);

		if (ngsProgress.getState().code >= State.Preprocessing.code) {
			new WText("<div> QC before preprocessing</div>", this);
			File qcDir = new File(workDir, NgsAnalysis.QC_REPORT_DIR);
			addQC(qcDir);
		}

		if (ngsProgress.getState().code >= State.Diamond.code) {
			new WText("<div> QC after preprocessing</div>", this);
			File qcDir = new File(workDir, NgsAnalysis.QC_REPORT_AFTER_PREPROCESS_DIR);
			addQC(qcDir);
		}

	}

	private void addQC(File qcDir) {
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