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
import rega.genotype.ui.admin.file_editor.blast.BlastFileEditor;
import rega.genotype.ui.forms.details.DefaultRecombinationDetailsForm;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.recombination.CsvModel.Mode;
import rega.genotype.ui.viruses.generic.GenericDefinition;
import rega.genotype.viruses.generic.GenericTool;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WContainerWidget.Overflow;
import eu.webtoolkit.jwt.WIntValidator;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTabWidget;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WValidator;

public class ToolVerificationWidget extends WTabWidget{
	public static final String SELF_SCAN_RESULT_FILE = "self-scan-result.xml";

	public ToolVerificationWidget(final ToolConfig toolConfig) {
		SelfScanWidget selfScanWidget = new SelfScanWidget(toolConfig);
		addTab(selfScanWidget, "Self Scan");
	}

	// classes

	public static class SelfScanWidget extends Template {
		public SelfScanWidget(final ToolConfig toolConfig) {
			super(tr("admin.config.self-scan-widget"));

			final WPushButton runB = new WPushButton("Run self scan");
			final WLineEdit windowLE = new WLineEdit("500");
			final WLineEdit stepLE = new WLineEdit("100");
			final WContainerWidget reportContainer = new WContainerWidget();

			WValidator v = new WIntValidator();
			v.setMandatory(true);

			windowLE.setValidator(v);
			stepLE.setValidator(v);

			bindWidget("run", runB);
			bindWidget("window", windowLE);
			bindWidget("step-size", stepLE);
			bindWidget("report", reportContainer);

			runB.clicked().addListener(runB, new Signal.Listener() {
				public void trigger() {

					reportContainer.clear();

					File workDir = new File(toolConfig.getJobDir());
					workDir.mkdirs();

					String traceFile = workDir.getAbsolutePath() + File.separator + SELF_SCAN_RESULT_FILE;

					GenotypeTool genotypeTool;
					try {
						genotypeTool = new GenericTool(toolConfig, workDir);
					} catch (IOException e1) {
						e1.printStackTrace();
						return;
					} catch (ParameterProblemException e1) {
						e1.printStackTrace();
						return;
					} catch (FileFormatException e1) {
						e1.printStackTrace();
						return;
					}

					AlignmentAnalyses blastAnalysis = BlastFileEditor.readBlastXml(workDir);
					for (Cluster c: blastAnalysis.getAllClusters()) {
						String analysisFile = toolConfig.getConfiguration() 
								+ "phylo-" + c.getId() + ".xml";
						if (new File(analysisFile).exists()) {
							String analysisId = null;
							try {
								genotypeTool.startTracer(traceFile);
							} catch (FileNotFoundException e1) {
								e1.printStackTrace();
								return;
							}
							try {
								int windowSize = Integer.valueOf(windowLE.getText());
								int stepSize = Integer.valueOf(stepLE.getText());
								genotypeTool.analyzeSelf(traceFile, analysisFile, windowSize, stepSize, analysisId);
							} catch (AnalysisException e) {
								e.printStackTrace();
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}
							genotypeTool.stopTracer();

							// Show scan plot.
							GenotypeResultParser parser = new GenotypeResultParser();
							File scanFile = new File(workDir.getAbsolutePath(), SELF_SCAN_RESULT_FILE);
							parser.parseFile(scanFile);

							List<Element> phyloMajorResults = parser.getElements("genotype_result/result");

							for (Element phyloMajorElement: phyloMajorResults)
								if (phyloMajorElement.getAttributeValue("id").endsWith("self-scan")) {
									String scanResult = "genotype_result/result[@id='" + phyloMajorElement.getAttributeValue("id") + "']";
									if (parser.elementExists(scanResult)) {
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
										reportContainer.addWidget(new WText("<div>Self scan: " + phyloMajorElement.getAttributeValue("id") + "</div>"));
										bindWidget("report", defaultRecombinationDetailsForm);
									}
								}
						}
					}

				}
			});
		}

	}

	public static class GoldenSequencesTestWidget extends Template {

	}
}
