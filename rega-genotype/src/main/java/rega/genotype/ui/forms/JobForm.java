package rega.genotype.ui.forms;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.ngs.NgsDetailsForm;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;

/**
 * Container grouping together AbstractJobOverview and DetailsForm.
 * 
 * @author pieter
 */ 
public class JobForm extends AbstractForm {
	public static final String JOB_URL = "job";
	
	public static final String SEQUENCE_PREFIX = "sequence-";
	public static final String FILTER_PREFIX = "filter-";
	public static final String BUCKET_PATH = "bucket";
	
	private DetailsForm details;
	private AbstractJobOverview jobOverview;
	private RecombinationForm recombination;
	private WText error;
	
	private Signal1<String> jobIdChanged = new Signal1<String>();

	public JobForm(GenotypeWindow main, AbstractJobOverview jobOverview) {
		super(main);
		
		details = new DetailsForm(main);
		addWidget(details);
		this.jobOverview = jobOverview;
		addWidget(jobOverview);
		try {
			recombination = new RecombinationForm(main);
			addWidget(recombination);
		} catch (RuntimeException e) {
			
		}
		error = new WText();
		addWidget(error);
	}
	
	public void handleInternalPath(String internalPath) {
		StringTokenizer internalPathTokenizer = new StringTokenizer(internalPath, "//");
		
		try {
			String jobId = internalPathTokenizer.nextToken();
			Integer sequenceId = null;
			String filter = null;
			if (internalPathTokenizer.hasMoreElements()) {
				String token = internalPathTokenizer.nextToken();
				if (token.startsWith(SEQUENCE_PREFIX))
					sequenceId = Integer.parseInt(token.substring(SEQUENCE_PREFIX.length()));
				else if (token.startsWith(FILTER_PREFIX))
					filter = token.substring(FILTER_PREFIX.length());
				else if (token.equals(BUCKET_PATH))
					if (internalPathTokenizer.hasMoreElements()) {
						String bucketId = internalPathTokenizer.nextToken();
						if (!jobOverview.existsJob(jobId)) {
							error.setText(tr("monitorForm.nonExistingJobId").arg(jobId));
							showWidget(error);
							return;
						}
						File jobDir = jobOverview.getJobDir(jobId);

						NgsDetailsForm ngsDetails = new NgsDetailsForm(
								getMain(), jobDir, bucketId);
						addWidget(ngsDetails);
						jobIdChanged.trigger(jobId);
						showWidget(ngsDetails);
						return;
					}
			}

			String detailed = null;
			if (internalPathTokenizer.hasMoreElements())
				detailed = internalPathTokenizer.nextToken();
			
			if (!jobOverview.existsJob(jobId)) {
				error.setText(tr("monitorForm.nonExistingJobId").arg(jobId));
				showWidget(error);
				return;
			}

			if (filter != null && filter.trim().equals(""))
				filter = null;
			
			if (sequenceId == null) {
				jobOverview.init(jobId, filter);
				jobIdChanged.trigger(jobId);
				showWidget(jobOverview);
			} else {
				WString errorMsg;
				WWidget widget;
			
				if (detailed != null && detailed.startsWith(RecombinationForm.URL) 
					&& detailed.length() > RecombinationForm.URL.length() + 1
					&& recombination != null) {
					String type = detailed.substring(RecombinationForm.URL.length() + 1);
					errorMsg = recombination.init(jobOverview.getJobDir(jobId), sequenceId, type);
					widget = recombination;
				} else {
					errorMsg = details.init(jobOverview.getJobDir(jobId), sequenceId);
					widget = details;
				}
				
				if (errorMsg == null) {
					jobIdChanged.trigger(jobId);
					showWidget(widget);
				} else {
					error.setText(errorMsg.arg(jobId));
					showWidget(error);				
				}
			}
			jobOverview.handleInternalPath(internalPath);
		} catch (NoSuchElementException e) {
			error.setText(tr("monitorForm.nonExistingJobId").arg(""));
			showWidget(error);
		}
	}
	
	private void showWidget(WWidget form) {
		for (WWidget w : this.getChildren()) {
			w.setHidden(w != form);
		}
	}
	
	public Signal1<String> jobIdChanged() {
		return jobIdChanged;
	}
}
