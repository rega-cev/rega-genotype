package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import rega.genotype.config.Config;
import rega.genotype.ui.framework.widgets.AutoForm;
import rega.genotype.ui.framework.widgets.MsgDialog;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WApplication;

public class GlobalConfigForm extends AutoForm<Config.GeneralConfig>{
	public GlobalConfigForm(final Config config) {
		super(config.getGeneralConfig());

		setHeader("<h3>Server global configuration</h3>");
		setInfo();

		saveClicked().addListener(this, new Signal.Listener() {
			public void trigger() {
				if (save()) {
					try {
						config.save(Settings.getInstance().getBaseDir() + File.separator);
						if (Settings.getInstance().getConfig() == null){
							Settings.getInstance().setConfig(config);
							WApplication.getInstance().redirect(WApplication.getInstance().getBookmarkUrl());
						}
						new MsgDialog("Info", "Global config changes are saved.");
					} catch (IOException e) {
						e.printStackTrace();
						new MsgDialog("Info", "Global config not save, due to IO error.");
					}
				} else {
					new MsgDialog("Info", "Global config not save, see validation errors.");
				}
			}
		});
	}

	@Override
	protected Set<String> getIgnoredFields() {
		Set<String> ignore = new HashSet<String>();
		ignore.add("publisherPassword");
		return ignore;
	}

	private void setInfo() {
		setFieldToolTip("paupCmd", "<div>Paup* 4 beta10</div>" 
				+ "<div>can be purchased from http://paup.csit.fsu.edu/</div>"
				+ "<div>make sure to install this version, since older versions can give problems!</div>");

		setFieldToolTip("clustalWCmd", "<div>clustalw</div>"
				+ "<div>some unix based operating operating systems allow installation via the package manager</div>"
				+ "<div>can be installed manually by downloading the appropriate</div>"
				+ "<div>binaries or build from source code for your OS</div>"
				+ "<div>ftp://ftp.ebi.ac.uk/pub/software/clustalw2/</div>");

		setFieldToolTip("treePuzzleCmd", "<div>tree-puzzle 5.2 (only for HIV)</div>"
				+ "<div>you can download the binaries from http://www.tree-puzzle.de</div>");

		setFieldToolTip("treeGraphCmd", "<div>if you use unix based OS you should download and build the source code</div>"
				+ "<div>http://www.math.uni-bonn.de/people/jmueller/extra/treegraph/</div>"
				+ "<div>if you use a Microsoft Windows you can download a binary</div>"
				+ "<div>http://www.math.uni-bonn.de/people/jmueller/extra/treegraph/</div>");

		setFieldToolTip("blastPath", "<div>blast 2.2.11</div>"
				+ "<div>make sure to install this version, since other versions can give problems!</div>"
				+ "<div>the binaries can be downloaded from</div>"
				+ "<div>ftp://ftp.ncbi.nlm.nih.gov/blast/executables/release/</div>"
				+ "<div>select the appropriate binary for your OS</div>"
				+ "<div>these binaries can be installed by extracting the archive to a desired location</div>");

		setFieldToolTip("imageMagickConvertCmd", "<div>http://www.imagemagick.org</div>");

		setFieldToolTip("publisherName", "<div>All tools published from this server will use the publisher name.</div>");

		setFieldToolTip("repoUrl", "<div>The url of a public repository that contains all published tools.</div>"
				+ "<div>Currently this repository url is: http://typingtools.emweb.be/repository/repo-service</div>");

		setFieldToolTip("adminPassword", "<div>Will be used to login to the adin area.</div>");
	}
}
