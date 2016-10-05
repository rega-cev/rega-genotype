package rega.genotype.ui.admin.file_editor.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;

import rega.genotype.AlignmentAnalyses;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.AnalysisException;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.admin.AdminNavigation;
import rega.genotype.ui.admin.file_editor.blast.BlastFileEditor;
import rega.genotype.ui.forms.details.DefaultRecombinationDetailsForm;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.recombination.CsvModel.Mode;
import rega.genotype.ui.viruses.generic.GenericDefinition;
import rega.genotype.viruses.generic.GenericTool;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WApplication.UpdateLock;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WIntValidator;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WRegExpValidator;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTabWidget;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WValidator;

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

		new WText("<h3>Verify "+ toolConfig.getToolMenifest().getName() + " typing tool </h3>", this);

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
		tabs.addTab(goldenSequencesTestWidget, "Golden sequences test");
	}

	public Signal done() {
		return done;
	}

	// classes

	/**
	 * Test bootstrap values for every cluster.
	 */
	public static class SelfScanWidget extends Template {
		public SelfScanWidget(final ToolConfig toolConfig, final File workDir) {
			super(tr("admin.config.self-scan-widget"));

			final WPushButton runB = new WPushButton("Run self scan");
			final WLineEdit windowLE = new WLineEdit("200,300,400,500,750,1000,1500,2000,3000,4000");
			final WLineEdit stepLE = new WLineEdit("100");
			final WContainerWidget reportContainer = new WContainerWidget();

			WValidator v = new WIntValidator();
			v.setMandatory(true);

			WValidator rv = new WRegExpValidator("^\\d+(\\,\\d+)*$");
			rv.setMandatory(true);
			windowLE.setValidator(rv);
			stepLE.setValidator(v);

			windowLE.setWidth(new WLength(300));
			stepLE.setWidth(new WLength(300));

			bindWidget("run", runB);
			bindWidget("window", windowLE);
			bindWidget("step-size", stepLE);
			bindWidget("report", reportContainer);

			runB.clicked().addListener(runB, new Signal.Listener() {
				public void trigger() {
					reportContainer.clear();
					final WTabWidget reportTabs = new WTabWidget(reportContainer);
					final WApplication app = WApplication.getInstance();

					Thread t = new Thread(new Runnable() {
						public void run() {
							AlignmentAnalyses blastAnalysis = BlastFileEditor.readBlastXml(toolConfig.getConfigurationFile());
							for (Cluster c: blastAnalysis.getAllClusters()) {
								String analysisFile = toolConfig.getConfiguration() 
										+ "phylo-" + c.getId() + ".xml";
								if (new File(analysisFile).exists()) {
									String analysisId = null;

									String[] windowSizes = windowLE.getText().split(",");
									int stepSize;
									try {
										stepSize = Integer.valueOf(stepLE.getText());
									} catch (NumberFormatException e) {
										e.printStackTrace();
										return;
									}

									for (String ws: windowSizes) {
										int windowSize;
										try {
											windowSize = Integer.valueOf(ws);
										} catch (NumberFormatException e) {
											e.printStackTrace();
											continue;
										}

										String traceFile = workDir.getAbsolutePath() + File.separator + traceFileName(ws);

										GenotypeTool genotypeTool = createGenotypeTool(toolConfig, workDir);
										try {
											genotypeTool.startTracer(traceFile);
										} catch (FileNotFoundException e1) {
											e1.printStackTrace();
											return;
										}
										try {
											genotypeTool.analyzeSelf(traceFile, analysisFile, windowSize, stepSize, analysisId);
										} catch (AnalysisException e1) {
											e1.printStackTrace();
											continue;
										}

										genotypeTool.stopTracer();

										// Show scan plot.
										GenotypeResultParser parser = new GenotypeResultParser();
										File scanFile = new File(workDir.getAbsolutePath(), traceFileName(ws));
										parser.parseFile(scanFile);

										List<Element> phyloMajorResults = parser.getElements("genotype_result/result");

										for (Element phyloMajorElement: phyloMajorResults)
											if (phyloMajorElement.getAttributeValue("id").endsWith("self-scan")) {
												String scanResult = "genotype_result/result[@id='" + phyloMajorElement.getAttributeValue("id") + "']";
												if (parser.elementExists(scanResult)) {
													UpdateLock updateLock = app.getUpdateLock();

													DefaultRecombinationDetailsForm defaultRecombinationDetailsForm = 
															new DefaultRecombinationDetailsForm(scanResult, "major", 
																	new WString("Scan analysis result"), Mode.SelfScan);
													defaultRecombinationDetailsForm.setOverflow(Overflow.OverflowAuto);
													GenericDefinition od;
													try {
														od = new GenericDefinition(toolConfig);
													} catch (JDOMException e) {
														e.printStackTrace();
														return;
													} catch (IOException e) {
														e.printStackTrace();
														return;
													}
													defaultRecombinationDetailsForm.fillForm(parser, od, workDir);
													WContainerWidget reportC = new WContainerWidget();
													reportTabs.addTab(reportC, ws);
													reportC.addWidget(new WText("<div>Self scan: " + phyloMajorElement.getAttributeValue("id") + "</div>"));
													reportC.addWidget(defaultRecombinationDetailsForm);

													app.triggerUpdate();
													updateLock.release();
												}
											}
									}
								}
							}
						}
					});
					t.start();
				}

				private String traceFileName(String windowSize) {
					return SELF_SCAN_RESULT_FILE + windowSize + ".xml";
				}

				private GenotypeTool createGenotypeTool(
						final ToolConfig toolConfig, final File workDir) {
					GenotypeTool genotypeTool;
					try {
						genotypeTool = new GenericTool(toolConfig, workDir);
					} catch (IOException e1) {
						e1.printStackTrace();
						return null;
					} catch (ParameterProblemException e1) {
						e1.printStackTrace();
						return null;
					} catch (FileFormatException e1) {
						e1.printStackTrace();
						return null;
					}
					return genotypeTool;
				}
			});
		}
	}
}
