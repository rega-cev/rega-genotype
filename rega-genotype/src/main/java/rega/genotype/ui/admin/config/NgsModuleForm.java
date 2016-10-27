package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;

import rega.genotype.config.Config;
import rega.genotype.python.PythonEnv;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.framework.widgets.FormTemplate;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.utils.StreamReaderRuntime;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WText;

public class NgsModuleForm extends FormTemplate {
	private File aaFile = null;
	private File taxonomyFile = null;

	public NgsModuleForm(final File workDir) {
		super(tr("admin.config.nsg-module"));

		final WPushButton createDb = new WPushButton("Create NGS database");
		createDb.disable();

		final FileUpload aaUpload = new FileUpload();
		final FileUpload taxonomyUpload = new FileUpload();
		final WText infoT = new WText();

		aaUpload.getWFileUpload().setFilters(".fasta");
		taxonomyUpload.getWFileUpload().setFilters(".tab");

		bindWidget("upload-aa-db", aaUpload);
		bindWidget("upload-taxonomy", taxonomyUpload);
		bindWidget("create-db", createDb);
		bindWidget("info", infoT);

		aaUpload.uploadedFile().addListener(aaUpload, new Signal1.Listener<File>() {
			public void trigger(File arg) {
				aaFile = arg;
				createDb.setEnabled(aaFile != null && taxonomyFile != null);
			}
		});
		taxonomyUpload.uploadedFile().addListener(taxonomyUpload, new Signal1.Listener<File>() {
			public void trigger(File arg) {
				taxonomyFile = arg;
				createDb.setEnabled(aaFile != null && taxonomyFile != null);
			}
		});
		createDb.clicked().addListener(createDb, new Signal.Listener() {
			public void trigger() {
				File unirefVirusesAA50 = new File(workDir, Config.NGS_MODULE_UNIREF_VIRUSES_AA50);

				String[] args = new String[3];
				args[0] = aaFile.getAbsolutePath();
				args[1] = taxonomyFile.getAbsolutePath();
				args[2] = unirefVirusesAA50.getAbsolutePath(); // out
				
				try {
					new PythonEnv().execPython("/rega/genotype/python/number_fasta_r.py", args);
				} catch (Exception e1) {
					e1.printStackTrace();
					return;
				}

				// ./diamond makedb --in uniref-viruses-aa50.fa -d uniref-viruses-aa50

				Process p = null;

				File aaVirusesDb = new File(workDir, Config.NGS_MODULE_AA_VIRUSES_DB);
				String diamondPath = Settings.getInstance().getConfig().getGeneralConfig().getDiamondPath();
				String cmd = diamondPath + " makedb --in " + unirefVirusesAA50.getAbsolutePath() 
						+ " -d " + aaVirusesDb.getAbsolutePath();

				try {
					p = StreamReaderRuntime.exec(cmd, null, unirefVirusesAA50.getParentFile());
					int exitResult = p.waitFor();

					if (exitResult != 0) {
						infoT.setText("Error: could not create " + Config.NGS_MODULE_AA_VIRUSES_DB);
						return;
					}
				} catch (IOException e) {
					infoT.setText("Error: could not create " + Config.NGS_MODULE_AA_VIRUSES_DB + ". " + e.getMessage());
					return;
				} catch (InterruptedException e) {
					if (p != null)
						p.destroy();
					infoT.setText("Error: could not create " + Config.NGS_MODULE_AA_VIRUSES_DB + ". " + e.getMessage());
					return;
				}

				infoT.setText("Dimond database created.");
				dirtyHandler.increaseDirty();
			}
		});

	}
}
