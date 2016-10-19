package rega.genotype.ui.admin.file_editor.ui;

import java.io.File;

import rega.genotype.config.Config.ToolConfig;
import rega.genotype.ui.admin.AdminNavigation;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WTabWidget;
import eu.webtoolkit.jwt.WText;

/**
 * Verification framework for typing tools. 
 * Contains self scan (bootstrap) analysis and golden sequences test.
 * 
 * @author michael
 */
public class ToolVerificationWidget extends WContainerWidget{
	public static final String SELF_SCAN_RESULT_FILE = "self-scan-result_";
	public static final String EXPECTED_RESULTS_FILE = "expected-results.xlsx";
	public static final String GOLDEN_SEQUENCES_RESULTS_FILE = "golden-sequences-result.xlsx";

	private Signal done = new Signal();

	public ToolVerificationWidget(final ToolConfig toolConfig, final File workDir) {
		WPushButton close = new WPushButton("Close", this);
		WPushButton editB = new WPushButton("Edit tool", this);
		editB.addStyleClass("float-right");
		close.addStyleClass("float-right");

		new WText("<h3 style=\"color: darkblue;margin-top: 0px;\">" +
				"Verify "+ toolConfig.getToolMenifest().getName() + " typing tool </h3>", this);

		close.clicked().addListener(close, new Signal.Listener() {
			public void trigger() {
				done.trigger();
			}
		});
		editB.clicked().addListener(editB, new Signal.Listener() {
			public void trigger() {
				AdminNavigation.setEditToolUrl(
						toolConfig.getToolMenifest().getId(),
						toolConfig.getToolMenifest().getVersion());
			}
		});
		WTabWidget tabs = new WTabWidget(this);
		SelfScanWidget selfScanWidget = new SelfScanWidget(toolConfig, workDir);
		tabs.addTab(selfScanWidget, "Self Scan");

		GoldenSequencesTestWidget goldenSequencesTestWidget = new GoldenSequencesTestWidget(toolConfig, workDir);
		tabs.addTab(goldenSequencesTestWidget, "Golden standard sequences test");
	}

	public Signal done() {
		return done;
	}
}
