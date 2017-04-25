package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rega.genotype.ApplicationException;
import rega.genotype.config.Config;
import rega.genotype.service.ToolRepoServiceRequests;
import rega.genotype.singletons.Settings;
import rega.genotype.taxonomy.RegaSystemFiles;
import rega.genotype.taxonomy.TaxonomyModel;
import rega.genotype.ui.framework.widgets.AutoForm;
import rega.genotype.ui.framework.widgets.Dialogs;
import rega.genotype.ui.framework.widgets.StandardDialog;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.utils.BlastUtil;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WApplication.UpdateLock;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WProgressBar;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WText;

public class GlobalConfigForm extends AutoForm<Config.GeneralConfig>{
	public GlobalConfigForm(final Config config) {
		super(config.getGeneralConfig());

		setHeader("<h3>Server global configuration</h3>");
		setInfo();

		saveClicked().addListener(this, new Signal.Listener() {
			public void trigger() {
				if (save()) {
					try {
						config.save();
						if (Settings.getInstance().getConfig() == null){
							Settings.getInstance().setConfig(config);
							WApplication.getInstance().redirect(WApplication.getInstance().getBookmarkUrl());
						}
						// The repository may have changed.
						config.refreshToolCofigState(ToolRepoServiceRequests.getRemoteManifests());
						Dialogs.infoDialog("Info", "Global config changes are saved.");
					} catch (IOException e) {
						e.printStackTrace();
						Dialogs.infoDialog("Info", "Global config not save, due to IO error.");
					}
				} else {
					Dialogs.infoDialog("Info", "Global config not save, see validation errors.");
				}
			}
		});

		// update taxonomy data base

		WTable updateTaxonomyTable = new WTable(this);
		updateTaxonomyTable.setMargin(15, Side.Top);
		updateTaxonomyTable.addStyleClass("auto-form-table");
		updateTaxonomyTable.getElementAt(0, 0).addWidget(
				new WText("Download taxonomy "));

		final WPushButton updateTaxonomyB = new WPushButton(
				"Update taxonomy", updateTaxonomyTable.getElementAt(0, 1));
		updateTaxonomyB.clicked().addListener(updateTaxonomyB, new Signal.Listener() {
			public void trigger() {
				final StandardDialog d = new StandardDialog("Update taxonomy", false);
				d.show();
				d.setWidth(new WLength(300));

				final WText info = new WText("Download taxonomy file from uniprot and update all tools to use it.");
				d.getContents().addWidget(info);
				d.getOkB().clicked().addListener(d, new Signal.Listener() {
					public void trigger() {
						d.getOkB().disable();
						d.getCancelB().disable();
						info.setText("Downloading taxonomy file, this can take some time..");

						final WApplication app = WApplication.getInstance();

						Thread t = new Thread(new Runnable() {
							public void run() {
								File file = RegaSystemFiles.downloadTaxonomyFile();
								String infoText = file == null ? "Could not downlod taxonomy file" : "Update finished successfully";
								UpdateLock updateLock = app.getUpdateLock();
								TaxonomyModel.getInstance().read(RegaSystemFiles.taxonomyFile());
								info.setText(infoText);
								d.getCancelB().enable();
								d.getCancelB().setText("Close");
								d.getOkB().hide();
								app.triggerUpdate();
								updateLock.release();
							}
						});
						t.start();
					}
				});
			}
		});

		// update NCBI viruses DNA data base

		WTable updateNcbiVirusesTable = new WTable(this);
		updateNcbiVirusesTable.setMargin(15, Side.Top);
		updateNcbiVirusesTable.addStyleClass("auto-form-table");
		updateNcbiVirusesTable.getElementAt(0, 0).addWidget(
				new WText("Download NCBI Viruses "));

		final WPushButton updateNcbiVirusesB = new WPushButton(
				"Update NCBI Viruses file", updateNcbiVirusesTable.getElementAt(0, 1));
		updateNcbiVirusesB.clicked().addListener(updateNcbiVirusesB, new Signal.Listener() {
			final WApplication app = WApplication.getInstance();
			public void trigger() {
				final StandardDialog d = new StandardDialog("Update NCBI virueses", false);
				final WText info = new WText();
				d.setWidth(new WLength(500));
				d.getContents().addWidget(info);
				d.getCancelB().setText("Close");

				info.setText("<div>Download NCBI viruses file from NCBI and update all tools to use it. This is used by NGS module and to auto genrate pan viral tool.</div>" +
						"<p><div>You can choose to upload the file or let the system do that for you.</div></p>" +
						"<div>Can be obtained from: ftp://ftp.ncbi.nlm.nih.gov/refseq/release/viral/viral.1.1.genomic.fna.gz</div>");

				final FileUpload upload = new FileUpload();
				d.getContents().addWidget(upload);
				upload.setInline(true);
				upload.getWFileUpload().setFilters(".gz");
				upload.getWFileUpload().setProgressBar(new WProgressBar());

				upload.uploadedFile().addListener(upload, new Signal1.Listener<File>() {
					public void trigger(File arg) {
						d.addText("<div><b>Create ncbi database, that can take some time. .. </b></div>");
						final File unzipNcbiViruses = RegaSystemFiles.unzipNcbiViruses(arg);
						Thread t = new Thread(new Runnable() {
							public void run() {
								createDB(unzipNcbiViruses, d);
							}
						});
						t.start();
					}
				});

				final WPushButton downlodAutomatically = new WPushButton("Download automaticaly.", d.getContents());
				downlodAutomatically.setInline(true);
				downlodAutomatically.clicked().addListener(downlodAutomatically, new Signal.Listener() {
					public void trigger() {
						d.getOkB().disable();
						d.getCancelB().disable();
						info.setText("Downloading ncbi viruses file, this can take some time..");
						upload.hide();
						downlodAutomatically.hide();
						Thread t = new Thread(new Runnable() {
							public void run() {
								createDB(RegaSystemFiles.downloadNcbiViruses(), d);
							}
						});
						t.start();
					}
				});
				d.getOkB().hide();
			}

			private void createDB(File ncbiViruses, StandardDialog d) {
				String infoText =  "Update finished successfully";
				if (ncbiViruses == null)
					infoText = "Could not downlod NCBI viruses file" ;
				else {
					File ncbiVirusesFile = null;
					try {
						//RegaSystemFiles.removePhages(); 
						ncbiVirusesFile = RegaSystemFiles.annotateNcbiDb();
					} catch (Exception e) {
						e.printStackTrace();
						infoText = "Error: Annotate ncbi DB did not work. " + e.getMessage();
					}
					if (ncbiVirusesFile != null)
						try {
							final File baseDir = new File(Settings.getInstance().getBaseDir());
							BlastUtil.formatDB(ncbiVirusesFile,
									new File(baseDir, RegaSystemFiles.SYSTEM_FILES_DIR));
						} catch (ApplicationException e) {
							e.printStackTrace();
							infoText = "Error: format db did not work. " + e.getMessage();
						}
				}
				UpdateLock updateLock = app.getUpdateLock();
				d.getContents().clear();
				d.addText(infoText);
				d.getCancelB().enable();
				app.triggerUpdate();
				updateLock.release();
			}
		});
	}

	@Override
	protected Set<String> getIgnoredFields() {
		Set<String> ignore = new HashSet<String>();
		ignore.add("publisherPassword");
		return ignore;
	}

	@Override
	protected Map<String, String> getFieldDisplayNames() {
		Map<String, String> ans = new HashMap<String, String>();
		ans.put("paupCmd", "Paup Command");
		ans.put("clustalWCmd", "ClustalW Command");
		ans.put("treePuzzleCmd", "Tree Puzzle Command");
		ans.put("treeGraphCmd", "Tree Graph Command");
		ans.put("epsToPdfCmd", "Eps To Pdf Command");
		ans.put("imageMagickConvertCmd", "Image Magick Convert Command");
		ans.put("inkscapeCmd", "Inkscape Command");
		ans.put("sequencetool", "sequencetool Command");
		ans.put("diamondPath", "Diamond Command");

		return ans;
	}
	
	private void setInfo() {
		setHeader("diamondPath", "<h3> NGS </h3>");
		setHeader("publisherName", "<h3> Local server configuration.</h3>");

		setFieldInfo("paupCmd", "<div>Paup* 4 beta10</div>" 
				+ "<div>Can be purchased from http://paup.csit.fsu.edu/</div>"
				+ "<div>Make sure to install this version, since older versions can give problems!</div>");

		setFieldInfo("clustalWCmd", "<div>*Some unix based operating systems allow installation via the package manager.</div>"
				+ "<div>*Can be installed manually by downloading the appropriate binaries or build from source code for your OS.</div>"
				+ "<div>ftp://ftp.ebi.ac.uk/pub/software/clustalw2/</div>");

		setFieldInfo("treePuzzleCmd", "<div>tree-puzzle 5.2 (only for HIV)</div>"
				+ "<div>You can download the binaries from http://www.tree-puzzle.de</div>");

		setFieldInfo("treeGraphCmd", "<div>*If you use unix based OS you should download and build the source code</div>"
				+ "<div>http://www.math.uni-bonn.de/people/jmueller/extra/treegraph/</div>");

		setFieldInfo("blastPath", "<div>blast 2.2.11</div>"
				+ "<div>Make sure to install this version, since other versions can give problems!</div>"
				+ "<div>The binaries can be downloaded from ftp://ftp.ncbi.nlm.nih.gov/blast/executables/legacy/2.2.11/</div>"
				+ "<div>Select the appropriate binary for your OS</div>"
				+ "<div>These binaries can be installed by extracting the archive to a desired location</div>");

		setFieldInfo("diamondPath", "<div>DIAMOND v0.8.9</div>"
				+ "<div>Make sure to install this version, since other versions can give problems!</div>"
				+ "<div>The binaries can be downloaded from https://github.com/bbuchfink/diamond/releases/</div>"
				+ "<div>Select the appropriate binary for your OS</div>"
				+ "<div>These binaries can be installed by extracting the archive to a desired location</div>");

		setFieldInfo("imageMagickConvertCmd", "<div>http://www.imagemagick.org</div>");

		setFieldInfo("publisherName", "<div>All Tools published from this server will use the publisher name.</div>");

		setFieldInfo("repoUrl", "<div>The url of a public repository that contains all published tools.</div>"
				+ "<div>Currently this repository url is: http://typingtools.emweb.be/repository/repo-service</div>");

		setFieldInfo("adminPassword", "<div>Will be used to login to the adin area.</div>");
		
		setFieldInfo("epsToPdfCmd", "<div>Can be obtained from 'texlive-font-utils'</div>");

		setFieldInfo("fastqcCmd", "<div>Can be obtained from http://www.bioinformatics.babraham.ac.uk/projects/download.html</div>");
		setFieldInfo("spadesCmd", "<div>Can be obtained from http://bioinf.spbau.ru/spades</div>");
		setFieldInfo("cutAdaptCmd", "<div>Can be obtained from https://cutadapt.readthedocs.io/en/stable/installation.html</div>");
		setFieldInfo("edirectPath", "<div>Can be obtained from http://www.ncbi.nlm.nih.gov/books/NBK179288/</div>");
		setFieldInfo("sequencetool", "<div>Can be obtained from https://github.com/emweb/sequencetool.git. You download the release! </div>");
		setFieldInfo("bioPythonPath", "<div>Bio python is used only to create the NGS Module. So most user do not need that.</div>" +
				"<div>Can be obtained from http://biopython.org/</div>");
		setFieldInfo("srrToolKitPath", "<div>This is needed only to allow the users to analyze SRR files from the internet.</div>" +
				"<div>Provide the pass to the bin folder of srrToolKit (check that fastq-dump is there).</div>" +
				"<div>I advise to disable the option on most servers. To disable leave the field empty. </div>");
		setFieldInfo("srrDatabasePath", "<div>Some maywant to save a database of NGS files so one does not need to upload the same files every time.</div>");
		setFieldInfo("samtoolsCmd", "<div>Can be obtained from http://samtools.sourceforge.net/</div>");
		setFieldInfo("bwaCmd", "<div>Can be obtained from http://bio-bwa.sourceforge.net/</div>");
		setFieldInfo("jobSchedulerLogFile", "<div>It can be interesting for the admin to know all the running jobs. Currently only the NGS job queue is printed. Note: the file must have write permission.</div>");

		setFieldMandatory("jobSchedulerLogFile", false);
		setFieldMandatory("srrDatabasePath", false);
		setFieldMandatory("bioPythonPath", false);
		setFieldMandatory("srrToolKitPath", false);
		setFieldMandatory("sequencetool", false);
	}
}
