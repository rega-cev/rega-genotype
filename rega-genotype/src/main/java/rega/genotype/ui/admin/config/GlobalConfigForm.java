package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rega.genotype.config.Config;
import rega.genotype.service.ToolRepoServiceRequests;
import rega.genotype.singletons.Settings;
import rega.genotype.taxonomy.TaxonomyModel;
import rega.genotype.taxonomy.UpdateTaxonomyFileService;
import rega.genotype.ui.framework.widgets.AutoForm;
import rega.genotype.ui.framework.widgets.Dialogs;
import rega.genotype.ui.framework.widgets.StandardDialog;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WApplication.UpdateLock;
import eu.webtoolkit.jwt.WLength;
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
								File file = UpdateTaxonomyFileService.download();
								String infoText = file == null ? "Could not downlod taxonomy file" : "Update finished";
								UpdateLock updateLock = app.getUpdateLock();
								TaxonomyModel.read(UpdateTaxonomyFileService.taxonomyFile());
								info.setText(infoText);
								d.getCancelB().enable();
								app.triggerUpdate();
								updateLock.release();
							}
						});
						t.start();
					}
				});
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

		return ans;
	}
	
	private void setInfo() {
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
		setFieldInfo("ncbiVirusesDbPath", "<div>Can be obtained from https://www.ncbi.nlm.nih.gov/nuccore </div>" +
				"<div> Select top combobox -> nucleotide, Left menu: Species -> Viruses; Source databases -> RefSeq </div>" +
				"<div>download fasta</div>");
		setFieldInfo("sequencetoolPath", "<div>Can be obtained from https://github.com/emweb/sequencetool.git, you will also need to install libseq: https://github.com/rega-cev/libseq </div>");
	}
}
