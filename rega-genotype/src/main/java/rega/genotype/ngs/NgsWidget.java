package rega.genotype.ngs;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.io.FilenameUtils;

import rega.genotype.framework.async.LongJobsScheduler;
import rega.genotype.ngs.NgsResultsTracer.State;
import rega.genotype.utils.Utils;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.Side;
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

	public NgsWidget(final File workDir) {
		super();

		final NgsResultsTracer ngsProgress = NgsResultsTracer.read(workDir);
		if (ngsProgress == null)
			return;

		new WText("<h2> NGS Analysis State </h2>", this);
		
		WContainerWidget preprocessingWidget = stateWidget(
				"Preprocessing", ngsProgress.getStateStartTime(State.Init), 
				ngsProgress.getStateStartTime(State.Diamond),
				ngsProgress.getReadCountStartState(State.Init),
				ngsProgress.getReadCountStartState(State.Diamond));

		WContainerWidget filtringWidget = stateWidget(
				State.Diamond.text, ngsProgress.getStateStartTime(State.Diamond), 
				ngsProgress.getStateStartTime(State.Spades),
				ngsProgress.getReadCountStartState(State.Diamond),
				ngsProgress.getReadCountStartState(State.Spades));

		WContainerWidget identificationWidget = stateWidget(
				State.Spades.text, ngsProgress.getStateStartTime(State.Spades), 
				ngsProgress.getStateStartTime(State.FinishedAll),
				ngsProgress.getReadCountStartState(State.Spades),
				ngsProgress.getReadCountStartState(State.FinishedAll));

		addWidget(preprocessingWidget);
		addWidget(filtringWidget);
		addWidget(identificationWidget);

		if (!ngsProgress.getErrors().isEmpty() )
			new WText("<div class=\"error\">Error: " + ngsProgress.getErrors() + "</div>", 
					preprocessingWidget);

		if (ngsProgress.getState().code >= State.Preprocessing.code) {
			new WText("<div> QC before preprocessing</div>", preprocessingWidget);
			File qcDir = new File(workDir, NgsFileSystem.QC_REPORT_DIR);
			addQC(qcDir, preprocessingWidget);
		}

		if (ngsProgress.getState().code >= State.Diamond.code) {
			if (ngsProgress.getSkipPreprocessing())
				new WText("<div> input sequences are OK -> skip  preprocessing.</div>", preprocessingWidget);
			else {
				new WText("<div> QC after preprocessing</div>", preprocessingWidget);
				File qcDir = new File(workDir, NgsFileSystem.QC_REPORT_AFTER_PREPROCESS_DIR);
				addQC(qcDir, preprocessingWidget);
			}
		}

		if (ngsProgress.getState().code == State.Diamond.code) {
			String jobState = LongJobsScheduler.getInstance().getJobState(workDir);
			new WText("<div> Diamond blast job state:" + jobState + "</div>", filtringWidget);
		}

		if (ngsProgress.getState().code == State.Spades.code) {
			String jobState = LongJobsScheduler.getInstance().getJobState(workDir);
			new WText("<div> Sapdes job state:" + jobState + "</div>", this);
		}

		// style

		if (ngsProgress.getState().code < State.Diamond.code){
			preprocessingWidget.addStyleClass("working");
			filtringWidget.addStyleClass("waiting");
			identificationWidget.addStyleClass("waiting");
		} else if (ngsProgress.getState() == State.Diamond){
			filtringWidget.addStyleClass("working");
			identificationWidget.addStyleClass("waiting");
		} else if (ngsProgress.getState() == State.Spades){
			identificationWidget.addStyleClass("working");
		}
		
	}

	private WContainerWidget stateWidget(String title, 
			Long startTime, Long endTime,
			Integer startReads, Integer endReads){
		WContainerWidget stateWidget = new WContainerWidget();
		new WText("<div><b>" + title + "</b> ("+ printTime(startTime, endTime) + ") </div>", 
				stateWidget);
		if (endReads != null)
			new WText("<div> Started with " + startReads + " reads. " + (startReads - endReads) + " reads where removed. </div>",
					stateWidget);

		stateWidget.setMargin(10, Side.Top);
		
		return stateWidget;
	}

	private String printTime(Long startTime, Long endTime) {
		if (startTime != null && endTime != null) {
			return  Utils.formatTime(endTime - startTime);
		} else {
			return "--";
		}
	}

	private void addQC(File qcDir, WContainerWidget preprocessingWidget) {
		if (qcDir.listFiles() == null)
			return;
		File[] files = qcDir.listFiles();
		Arrays.sort(files, new Comparator<File>() {
			public int compare(File o1, File o2) {
				return o1.getAbsolutePath().compareTo(o2.getAbsolutePath());
			}
		});
		for (File f: files) {
			if (FilenameUtils.getExtension(f.getAbsolutePath()).equals("html")){
				WContainerWidget c = new WContainerWidget(preprocessingWidget);
				new WText("QC report for ", c);
				WFileResource r = new WFileResource("html", f.getAbsolutePath());
				WLink link = new WLink(r);
				link.setTarget(AnchorTarget.TargetNewWindow);
				c.addWidget(new WAnchor(link, f.getName()));
			}
		}
	}
}