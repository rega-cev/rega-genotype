package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import rega.genotype.ApplicationException;
import rega.genotype.config.NgsModule;
import rega.genotype.python.PythonEnv;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.framework.widgets.FormTemplate;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.utils.BlastUtil;
import rega.genotype.utils.StreamReaderRuntime;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WText;

public class NgsModuleForm extends FormTemplate {
	private File aaFile = null;
	private File taxonomyFile = null;
	private File ncbiVirusesFile = null;

	public NgsModuleForm(final File workDir) {
		super(tr("admin.config.nsg-module"));

		final WPushButton createDb = new WPushButton("Create NGS database");
		createDb.disable();

		final FileUpload aaUpload = new FileUpload();
		final FileUpload taxonomyUpload = new FileUpload();
		final FileUpload ncbiVirusesUpload = new FileUpload();
		final WText infoT = new WText();

		aaUpload.setInline(true);
		taxonomyUpload.setInline(true);
		ncbiVirusesUpload.setInline(true);

		bindEmpty("current-aa-db");
		bindEmpty("current-taxonomy");
		bindEmpty("current-ncbi-viruses");

		NgsModule ngsModule = NgsModule.read(workDir);
		if (ngsModule != null) {
			if (ngsModule.getAaFileName() != null) {
				aaFile = new File(workDir, ngsModule.getAaFileName());
				bindString("current-aa-db", aaFile.getName());
			}
			if (ngsModule.getTaxonomyFileName() != null) {
				taxonomyFile = new File(workDir, ngsModule.getTaxonomyFileName());
				bindString("current-taxonomy", taxonomyFile.getName());
			}
			if (ngsModule.getNcbiVirusesFileName() != null) {
				ncbiVirusesFile = new File(workDir, ngsModule.getNcbiVirusesFileName());
				bindString("current-ncbi-viruses", ncbiVirusesFile.getName());
			}

			createDb.setEnabled(isReady());
		}


		aaUpload.getWFileUpload().setFilters(".fasta");
		taxonomyUpload.getWFileUpload().setFilters(".tab");

		bindWidget("upload-ncbi-viruses", ncbiVirusesUpload);
		bindWidget("upload-aa-db", aaUpload);
		bindWidget("upload-taxonomy", taxonomyUpload);
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
		taxonomyUpload.uploadedFile().addListener(taxonomyUpload, new Signal1.Listener<File>() {
			public void trigger(File arg) {
				taxonomyFile = new File(workDir, taxonomyUpload.getWFileUpload().getClientFileName());
				try {
					FileUtils.copyFile(arg, taxonomyFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
				bindString("current-taxonomy", taxonomyUpload.getWFileUpload().getClientFileName());
				createDb.setEnabled(isReady());
			}
		});
		ncbiVirusesUpload.uploadedFile().addListener(ncbiVirusesUpload, new Signal1.Listener<File>() {
			public void trigger(File arg) {
				ncbiVirusesFile = new File(workDir, ncbiVirusesUpload.getWFileUpload().getClientFileName());
				try {
					FileUtils.copyFile(arg, ncbiVirusesFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
				bindString("current-ncbi-viruses", ncbiVirusesUpload.getWFileUpload().getClientFileName());
				try {
					BlastUtil.formatDB(ncbiVirusesFile, workDir);
				} catch (ApplicationException e) {
					e.printStackTrace();
					bindString("current-ncbi-viruses", ncbiVirusesUpload.getWFileUpload().getClientFileName()
							+ " <div>Error format bd did not work!</div>");

				}
				createDb.setEnabled(isReady());
			}
		});
		createDb.clicked().addListener(createDb, new Signal.Listener() {
			public void trigger() {
				File unirefVirusesAA50 = new File(workDir, NgsModule.NGS_MODULE_UNIREF_VIRUSES_AA50);

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

				File aaVirusesDb = new File(workDir, NgsModule.NGS_MODULE_AA_VIRUSES_DB);
				String diamondPath = Settings.getInstance().getConfig().getGeneralConfig().getDiamondPath();
				String cmd = diamondPath + " makedb --in " + unirefVirusesAA50.getAbsolutePath() 
						+ " -d " + aaVirusesDb.getAbsolutePath();

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

				NgsModule ngsModule = new NgsModule();
				ngsModule.setAaFileName(aaUpload.getWFileUpload().getClientFileName());
				ngsModule.setNcbiVirusesFileName(ncbiVirusesUpload.getWFileUpload().getClientFileName());
				ngsModule.setTaxonomyFileName(taxonomyUpload.getWFileUpload().getClientFileName());
				ngsModule.save(workDir);
				
				infoT.setText("Dimond database created.");
				dirtyHandler.increaseDirty();
			}
		});
	}

	private boolean isReady() { 
		return aaFile != null && taxonomyFile != null && ncbiVirusesFile != null;
	}
}
