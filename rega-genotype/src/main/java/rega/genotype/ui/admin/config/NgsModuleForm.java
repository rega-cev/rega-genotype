package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import rega.genotype.config.Config;
import rega.genotype.config.NgsModule;
import rega.genotype.python.PythonEnv;
import rega.genotype.singletons.Settings;
import rega.genotype.taxonomy.RegaSystemFiles;
import rega.genotype.ui.framework.widgets.FormTemplate;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.utils.StreamReaderRuntime;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WText;

public class NgsModuleForm extends FormTemplate {
	private File aaFile = null;

	public NgsModuleForm(final File workDir) {
		super(tr("admin.config.nsg-module"));

		final WPushButton createDb = new WPushButton("Create NGS database");
		createDb.disable();

		final FileUpload aaUpload = new FileUpload();
		final WText infoT = new WText();

		aaUpload.setInline(true);

		bindEmpty("current-aa-db");

		final NgsModule ngsModule = NgsModule.read(workDir);
		if (ngsModule != null) {
			if (ngsModule.getAaFileName() != null) {
				aaFile = new File(workDir, ngsModule.getAaFileName());
				bindString("current-aa-db", aaFile.getName());
			}

			createDb.setEnabled(isReady());
		}


		aaUpload.getWFileUpload().setFilters(".fasta");

		bindWidget("upload-aa-db", aaUpload);
		bindWidget("create-db", createDb);
		bindWidget("info", infoT);

		aaUpload.uploadedFile().addListener(aaUpload, new Signal1.Listener<File>() {
			public void trigger(File arg) {
				aaFile = new File(workDir, aaUpload.getWFileUpload().getClientFileName());
				try {
					FileUtils.copyFile(arg, aaFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
				bindString("current-aa-db", aaUpload.getWFileUpload().getClientFileName());
				createDb.setEnabled(isReady());
			}
		});

		createDb.clicked().addListener(createDb, new Signal.Listener() {
			public void trigger() {
				new File(workDir, Config.TRANSIENT_DATABASES_FOLDER_NAME).mkdirs();

				File unirefVirusesAA50 = new File(workDir, NgsModule.NGS_MODULE_AA_VIRUSES_FASTA);

				String[] args = new String[3];
				args[0] = aaFile.getAbsolutePath();
				args[1] = RegaSystemFiles.taxonomyFile().getAbsolutePath();
				args[2] = unirefVirusesAA50.getAbsolutePath(); // out

				try {
					new PythonEnv().execPython("/rega/genotype/python/number_fasta_r.py", args);
				} catch (Exception e1) {
					e1.printStackTrace();
					return;
				}

				// ./diamond makedb --in uniref-viruses-aa50.fa -d uniref-viruses-aa50

				Process p = null;

				File aaVirusesDb = new File(workDir, NgsModule.NGS_MODULE_AA_VIRUSES_DB);
				String diamondPath = Settings.getInstance().getConfig().getGeneralConfig().getDiamondPath();
				String cmd = diamondPath + " makedb --in " + unirefVirusesAA50.getAbsolutePath() 
						+ " -d " + aaVirusesDb.getAbsolutePath();

				System.err.println(cmd);
				
				try {
					p = StreamReaderRuntime.exec(cmd, null, unirefVirusesAA50.getParentFile());
					int exitResult = p.waitFor();

					if (exitResult != 0) {
						infoT.setText("Error: could not create " + NgsModule.NGS_MODULE_AA_VIRUSES_DB);
						return;
					}
				} catch (IOException e) {
					infoT.setText("Error: could not create " + NgsModule.NGS_MODULE_AA_VIRUSES_DB + ". " + e.getMessage());
					return;
				} catch (InterruptedException e) {
					if (p != null)
						p.destroy();
					infoT.setText("Error: could not create " + NgsModule.NGS_MODULE_AA_VIRUSES_DB + ". " + e.getMessage());
					return;
				}

				NgsModule ngsModule = NgsModule.read(workDir);
				if (ngsModule == null)
					ngsModule = new NgsModule();
				if (!aaUpload.getWFileUpload().isEmpty())
					ngsModule.setAaFileName(aaUpload.getWFileUpload().getClientFileName());
				ngsModule.save(workDir);

				infoT.setText("Dimond database created.");
				dirtyHandler.increaseDirty();
			}
		});
	}

	private boolean isReady() { 
		return aaFile != null;
	}
}
