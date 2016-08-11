package rega.genotype.ui.admin.file_editor.blast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.ApplicationException;
import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.SequenceAlignment;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.admin.file_editor.xml.BlastXmlWriter;
import rega.genotype.ui.admin.file_editor.xml.PanViralToolGenerator;
import rega.genotype.ui.framework.widgets.Dialogs;
import rega.genotype.ui.framework.widgets.DirtyHandler;
import rega.genotype.ui.framework.widgets.StandardTableView;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.utils.FileUtil;
import eu.webtoolkit.jwt.Icon;
import eu.webtoolkit.jwt.SelectionBehavior;
import eu.webtoolkit.jwt.SelectionMode;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal.Listener;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.StandardButton;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WApplication.UpdateLock;
import eu.webtoolkit.jwt.WDialog.DialogCode;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WFileUpload;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WMessageBox;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WPanel;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WStackedWidget;
import eu.webtoolkit.jwt.WText;

/**
 * Editor for blast.xml and blast.fasta.
 * 
 * @author michael
 */
public class BlastFileEditor extends WContainerWidget{
	private File workDir;
	private BlastAnalysisForm analysis;
	private WStackedWidget stack = new WStackedWidget(this);
	private Template layout = new Template(tr("admin.config.blast-file-editor"));
	private AlignmentAnalyses alignmentAnalyses;
	private ClusterTableModel clusterTableModel;
	private Signal1<Integer> editingInnerXmlElement = new Signal1<Integer>();
	private DirtyHandler dirtyHandler;
	private ReferenceTaxaTable referenceTaxaTable;

	public BlastFileEditor(final File workDir, final DirtyHandler dirtyHandler) {
		this.workDir = workDir;
		this.dirtyHandler = dirtyHandler;

		final WPushButton addSequencesB = new WPushButton("Add sequences");
		final WPushButton autoCreatePanViralToolB = new WPushButton("Auto create pav-viral tool");

		alignmentAnalyses = readBlastXml(workDir);

		WPanel analysisPanel = new WPanel();
		analysisPanel.addStyleClass("admin-panel");
		analysisPanel.setTitle("Analysis");
		analysis = new BlastAnalysisForm(
				(BlastAnalysis) alignmentAnalyses.getAnalysis("blast"));
		analysisPanel.setCentralWidget(analysis);
		BlastAnalysis blastAnalysis = (BlastAnalysis) alignmentAnalyses.getAnalysis("blast");
		referenceTaxaTable = new ReferenceTaxaTable(blastAnalysis);
		WPanel refTaxaPanel = new WPanel();
		refTaxaPanel.addStyleClass("admin-panel");
		refTaxaPanel.setTitle("Reference taxa");
		refTaxaPanel.setCentralWidget(referenceTaxaTable);
		refTaxaPanel.setCollapsible(true);
		refTaxaPanel.setCollapsed(blastAnalysis.getReferenceTaxus().isEmpty());

		createClustersTable(alignmentAnalyses);

		//bind

		stack.addWidget(layout);
		layout.bindWidget("analysis", analysisPanel);
		layout.bindWidget("ref-taxa", refTaxaPanel);
		layout.bindWidget("add-sequences", addSequencesB);
		layout.bindWidget("auto-create-pan-viral", autoCreatePanViralToolB);

		addSequencesB.clicked().addListener(addSequencesB, new Signal.Listener() {
			public void trigger() {
				final AddSequencesDialog d = new AddSequencesDialog(
						null, alignmentAnalyses, toolConfig());
				d.finished().addListener(d, new Signal1.Listener<WDialog.DialogCode>() {
					public void trigger(DialogCode arg) {
						if(arg == DialogCode.Accepted){
							Map<AbstractSequence, Cluster> selectedSequences = d.getSelectedSequences();
							for (Map.Entry<AbstractSequence, Cluster> e: selectedSequences.entrySet()){
								AbstractSequence sequence = e.getKey();
								Cluster cluster = e.getValue();
								cluster.addTaxus(sequence.getName());
								alignmentAnalyses.getAlignment().addSequence(sequence);
							}
						}
					}
				});
			}
		});

		autoCreatePanViralToolB.clicked().addListener(autoCreatePanViralToolB, new Signal.Listener() {
			public void trigger() {
				final WDialog d = new WDialog("Auto create pan-viral tool");
				d.show();
				final WPushButton close = new WPushButton("Close", d.getFooter());
				close.clicked().addListener(close, new Signal.Listener() {
					public void trigger() {
						d.reject();
					}
				});
				d.getContents().addWidget(new WText("<div>A new pan-viral tool will be auto created.</div>"
						+ "<div>The tool will contain all viruses that have accession number in ICTV Master Species List.</div>"
						+ "<div>This will overwrite your blast configuration.</div>"
						+ "<div>Note: accession numbers that are not properly formatted will be ignored. </div>"
						+ "<div>Upload ICTV Master Species List in xlsx format.</div>"));
				final WFileUpload upload = new WFileUpload(d.getContents());
				upload.setFilters(".xlsx");

				final WPushButton createB = new WPushButton("Create", d.getContents());
				final WText info = new WText(d.getContents());
				info.addStyleClass("auto-form-info");
				info.setInline(false);
				createB.clicked().addListener(createB, new Signal.Listener() {
					public void trigger() {
						upload.upload();
					}
				});
				createB.setMargin(10);

				upload.uploaded().addListener(upload, new Signal.Listener() {
					public void trigger() {
						if (upload.getUploadedFiles().size() == 0) 
							info.setText("Upload file first.");
						else {
							info.setText("Creating Pan-viral tool please wait...");
							close.disable();
						}

						final WApplication app = WApplication.getInstance();

						Thread t = new Thread(new Runnable() {
							public void run() {
								try {
									PanViralToolGenerator autoCreatePanViral = new PanViralToolGenerator();
									AlignmentAnalyses alignmentAnalyses = autoCreatePanViral.createAlignmentAnalyses(
											new File(upload.getSpoolFileName()));

									new BlastXmlWriter(BlastFileEditor.blastFile(BlastFileEditor.this.workDir), alignmentAnalyses);
									alignmentAnalyses.getAlignment().writeOutput(new FileOutputStream(BlastFileEditor.fastaFile(BlastFileEditor.this.workDir)),
											SequenceAlignment.FILETYPE_FASTA);

									UpdateLock lock = app.getUpdateLock();
									rereadFiles();
									info.setText("Pan viral tool was auto created. You can still make some modifications from the editor.");
									close.enable();
									dirtyHandler.increaseDirty();
									app.triggerUpdate();
									lock.release();

								} catch (ApplicationException e) {
									e.printStackTrace();
									updateInfo("Error: " + e.getMessage());
								} catch (IOException e) {
									e.printStackTrace();
									updateInfo("Error: " + e.getMessage());
								} catch (InterruptedException e) {
									e.printStackTrace();
									updateInfo("Error: " + e.getMessage());
								} catch (ParameterProblemException e) {
									e.printStackTrace();
									updateInfo("Error: " + e.getMessage());
								} catch (FileFormatException e) {
									e.printStackTrace();
									updateInfo("Error: " + e.getMessage());
								}

							}

							private void updateInfo(String text) {
								UpdateLock lock = app.getUpdateLock();
								info.setText(text);
								app.triggerUpdate();
								lock.release();
							}
						});
						t.start();

					}
				});
			}
		});

		// dirty
		dirtyHandler.connect(analysis.getDirtyHandler(), this);
		dirtyHandler.connect(referenceTaxaTable.getDirtyHandler(), this);
	}

	public boolean save(File dir) {
		try {
			analysis.save();
			referenceTaxaTable.save();

			if (!blastFile(dir).exists())
				blastFile(dir).createNewFile();

			new BlastXmlWriter(blastFile(dir), alignmentAnalyses);
			writeFastaFile(dir);
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Dialogs.infoDialog("Error", "Could not save blast.xml");
		} catch (IOException e) {
			e.printStackTrace();
			Dialogs.infoDialog("Error", "Could not save blast.fasta");
		} catch (ParameterProblemException e) {
			e.printStackTrace();
			Dialogs.infoDialog("Error", "Could not save blast.fasta");
		} 

		return false;
	}

	public static File blastFile(File dir) {
		return new File(dir.getAbsolutePath(), "blast.xml");
	}

	public static File fastaFile(File dir) {
		return new File(dir.getAbsolutePath(), "blast.fasta");
	}

	private ToolConfig toolConfig() {
		ToolManifest manifest = ToolManifest.parseJson(
				FileUtil.readFile(new File(workDir, ToolManifest.MANIFEST_FILE_NAME)));
		if (manifest == null)
			return null;

		return Settings.getInstance().getConfig().getToolConfigById(manifest.getId(), manifest.getVersion());
	}

	/**
	 * Read blast.xml file
	 */
	public static AlignmentAnalyses readBlastXml(File workDir){
		if (blastFile(workDir).exists()) {
			try {
				final File jobDir = GenotypeLib.createJobDir(
						Settings.getInstance().getBaseJobDir() + File.separator + "tmp");
				jobDir.mkdirs();
				return new AlignmentAnalyses(blastFile(workDir), null, jobDir);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParameterProblemException e) {
				e.printStackTrace();
			} catch (FileFormatException e) {
				e.printStackTrace();
			}
		}

		final File jobDir = GenotypeLib.createJobDir(
				Settings.getInstance().getBaseJobDir() + File.separator + "tmp");
		jobDir.mkdirs();
		AlignmentAnalyses alignmentAnalyses = new AlignmentAnalyses();
		alignmentAnalyses.setAlignment(new SequenceAlignment());
		alignmentAnalyses.putAnalysis("blast",
				new BlastAnalysis(alignmentAnalyses,
						"", new ArrayList<AlignmentAnalyses.Cluster>(),
						0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, "", "", jobDir));

		return alignmentAnalyses;
	}

	private void writeFastaFile(File dir) throws ParameterProblemException, FileNotFoundException, IOException {
		alignmentAnalyses.getAlignment().writeOutput(new FileOutputStream(fastaFile(dir)),
				SequenceAlignment.FILETYPE_FASTA);
	}

	class RefTableHeaderWidget extends WContainerWidget{
		WPushButton removeB = new WPushButton("X");

		public RefTableHeaderWidget(String text) {
			addWidget(new WText(text));
			WPushButton removeB = new WPushButton("X");
			addWidget(removeB);
		}
	}
	
	private void createClustersTable(final AlignmentAnalyses alignmentAnalyses) {
		List<Cluster> clusters = alignmentAnalyses.getAllClusters();
		clusterTableModel = new ClusterTableModel(clusters);
		final StandardTableView table = new StandardTableView();
		table.setModel(clusterTableModel);
		table.setSelectionMode(SelectionMode.SingleSelection);
		table.setSelectionBehavior(SelectionBehavior.SelectRows);
		table.setHeight(new WLength(200));
		table.setTableWidth();

		WPushButton addB = new WPushButton("Add");
		WPushButton editB = new WPushButton("Edit");
		WPushButton removeB = new WPushButton("Remove");

		Template clusterTableTemplate = new Template(tr("admin.config.cluster-table"));
		clusterTableTemplate.bindWidget("add", addB);
		clusterTableTemplate.bindWidget("edit", editB);
		clusterTableTemplate.bindWidget("remove", removeB);
		clusterTableTemplate.bindWidget("cluster-table", table);

		WPanel clusterPanel = new WPanel();
		clusterPanel.addStyleClass("admin-panel");
		clusterPanel.setTitle("Reference clusters");
		clusterPanel.setCentralWidget(clusterTableTemplate);

		layout.bindWidget("cluster-table", clusterPanel);

		addB.clicked().addListener(addB, new Signal.Listener() {
			public void trigger() {
				editClaster(null);
			}
		});

		editB.clicked().addListener(addB, new Signal.Listener() {
			public void trigger() {
				if (table.getSelectedIndexes().size() == 1){
					WModelIndex index = table.getSelectedIndexes().first();
					Cluster cluster = clusterTableModel.getCluster(index.getRow());
					editClaster(cluster);
				}
			}
		});

		table.doubleClicked().addListener(table, new Signal2.Listener<WModelIndex, WMouseEvent>() {
			public void trigger(WModelIndex index, WMouseEvent arg2) {
				if (index != null && stack.getCount() == 1) {
					Cluster cluster = clusterTableModel.getCluster(index.getRow());
					editClaster(cluster);
				}
			}
		});

		removeB.clicked().addListener(removeB, new Listener() {
			public void trigger() {				
				final WModelIndex[] indexs = table.getSelectedIndexes().toArray(
						new WModelIndex[table.getSelectedIndexes().size()]);

				String txt = "Are you sure that you want to remove clusters: ";

				for (int i = 0; i < indexs.length; ++i){
					int row = indexs[i].getRow();
					if (i != 0)
						txt += ", ";
					txt += clusterTableModel.getCluster(row).getName();
				}

				final WMessageBox d = new WMessageBox("Warning", txt, Icon.NoIcon,
						EnumSet.of(StandardButton.Ok, StandardButton.Cancel));
				d.show();
				d.setWidth(new WLength(300));
				d.buttonClicked().addListener(d,
						new Signal1.Listener<StandardButton>() {
					public void trigger(StandardButton e1) {
						if(e1 == StandardButton.Ok){
							for (int i = indexs.length -1; i >= 0; --i){
								int row = indexs[i].getRow();
								alignmentAnalyses.removeCluster(clusterTableModel.getCluster(row));
							}
						}
						clusterTableModel.refresh();
						d.remove();
					}
				});
			}
		});
	}

	private void editClaster(final Cluster cluster){
		final boolean isNew = cluster == null;

		final ClusterForm c = new ClusterForm(cluster, alignmentAnalyses, toolConfig());
		stack.addWidget(c);
		stack.setCurrentWidget(c);
		editingInnerXmlElement.trigger(stack.getCount());

		c.done().addListener(c, new Signal1.Listener<WDialog.DialogCode>() {
			public void trigger(DialogCode arg) {
				if (arg == DialogCode.Accepted) {
					if (isNew)
						alignmentAnalyses.getAllClusters().add(c.getCluster());
					clusterTableModel.refresh();
					dirtyHandler.increaseDirty();
				}
				stack.removeWidget(c);
				stack.setCurrentWidget(layout);
				editingInnerXmlElement.trigger(stack.getCount());
			}
		});
	}

	public void rereadFiles() {
		alignmentAnalyses = readBlastXml(workDir);
		analysis.refresh((BlastAnalysis)
				alignmentAnalyses.getAnalysis("blast"));
		clusterTableModel.refresh(
				alignmentAnalyses.getAllClusters());
	}

	public boolean validate() {
		return analysis.validate();
	}

	public Signal1<Integer> editingInnerXmlElement() {
		return editingInnerXmlElement;
	}
}
