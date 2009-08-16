package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.StateLink;
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
	public static final String JOB_URL = "/job";
	
	private DetailsForm details;
	private AbstractJobOverview jobOverview;
	private WText error;
	
	private StateLink stateLink;
	
	public JobForm(GenotypeWindow main, AbstractJobOverview jobOverview) {
		super(main, null, null);
		
		details = new DetailsForm(main);
		addWidget(details);
		this.jobOverview = jobOverview;
		addWidget(jobOverview);
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
		if (GenotypeMain.getApp().isInternalPathMatches(JOB_URL + "/")) {
			String jobId = GenotypeMain.getApp().getInternalPathNextPart(JOB_URL + "/");
			
			if (!jobOverview.existsJob(jobId)) {
				error.setText(tr("monitorForm.nonExistingJobId").arg(jobId));
				showWidget(error);
				return;
			}
			
			String sequenceId = GenotypeMain.getApp().getInternalPathNextPart(JOB_URL + "/" + jobId + "/");
			
			if (sequenceId.equals("")) {
				jobOverview.init(jobId);
				stateLink.setVarValue(jobId);
				showWidget(jobOverview);
			} else {
				WString errorMsg = details.init(jobOverview.getJobDir(jobId), sequenceId);
				if (errorMsg == null) {
					stateLink.setVarValue(jobId);
					showWidget(details);
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
	
	public void setStateLink(StateLink stateLink) {
		this.stateLink = stateLink;
	}
}
