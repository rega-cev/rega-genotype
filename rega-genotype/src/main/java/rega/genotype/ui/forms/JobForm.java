package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
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
		
		GenotypeMain.getApp().internalPathChanged().addListener(this,
				new Signal1.Listener<String>() {
					public void trigger(String internalPath) {
						handleInternalPath();
					}
				});
	}
	
	public void handleInternalPath() {
		if (GenotypeMain.getApp().internalPathMatches("/" + JOB_URL + "/")) {
			String jobId = GenotypeMain.getApp().getInternalPathNextPart("/" + JOB_URL + "/");
			
			if (!jobOverview.existsJob(jobId)) {
				error.setText(tr("monitorForm.nonExistingJobId").arg(jobId));
				showWidget(error);
				return;
			}
			
			Integer sequenceId = null;
			String filter = null;
			try {
				filter = GenotypeMain.getApp().getInternalPathNextPart("/" + JOB_URL + "/" + jobId + "/");
				sequenceId = Integer.parseInt(filter);
				filter = null;
			} catch (NumberFormatException nfe) {
			}
			
			if (filter != null && filter.trim().equals(""))
				filter = null;
			
			if (sequenceId == null) {
				jobOverview.init(jobId, filter);
				jobIdChanged.trigger(jobId);
				showWidget(jobOverview);
			} else {
				String detailed = GenotypeMain.getApp().getInternalPathNextPart("/" + JOB_URL + "/" + jobId + "/" + sequenceId +"/");
				
				WString errorMsg;
				WWidget widget;
				if (detailed.startsWith(RecombinationForm.URL) 
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
		}
	}
	
	private void showWidget(WWidget form) {
		for (WWidget w : this.getChildren()) {
			w.setHidden(w != form);
		}
	}
	
	public Signal1<String> getJobIdChanged() {
		return jobIdChanged;
	}
}
