package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;

import rega.genotype.singletons.Settings;
import rega.genotype.ui.framework.widgets.FormTemplate;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.utils.StreamReaderRuntime;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WText;

public class NgsModuleForm extends FormTemplate {
	public static final String  UNIREF_VIRUSES_AA50 = "uniref-viruses-aa50.fasta";
	public static final String  AA_VIRUSES_DB = "aa-virus.dmnd";
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
				File unirefVirusesAA50 = new File(workDir, UNIREF_VIRUSES_AA50);

				//python ./number_fasta_r.py uniref-uniprot-viruses-aa50.fasta taxonomy-uniprot.tab > uniref-viruses-aa50.fasta
				// TODO
				String number_fasta_r_path = "/home/michael/Downloads/number_fasta_r.py";
				String cmd = "python " + number_fasta_r_path + " "
						+ aaFile.getAbsolutePath() + " " + taxonomyFile.getAbsolutePath() 
						+ " " + unirefVirusesAA50.getAbsolutePath();

				System.err.println(cmd);
				Process p = null;

				try {
					p = StreamReaderRuntime.exec(cmd, null, unirefVirusesAA50.getParentFile());
					int exitResult = p.waitFor();

					if (exitResult != 0) {
						infoT.setText("Error: could not create " + UNIREF_VIRUSES_AA50);
						return;
					}
				} catch (IOException e) {
					infoT.setText("Error: could not create " + UNIREF_VIRUSES_AA50 + ". " + e.getMessage());
					return;
				} catch (InterruptedException e) {
					if (p != null)
						p.destroy();
					infoT.setText("Error: could not create " + UNIREF_VIRUSES_AA50 + ". " + e.getMessage());
					return;
				}

				// ./diamond makedb --in uniref-viruses-aa50.fa -d uniref-viruses-aa50

				File aaVirusesDb = new File(workDir, AA_VIRUSES_DB);
				String diamondPath = Settings.getInstance().getConfig().getGeneralConfig().getDiamondPath();
				cmd = diamondPath + " makedb --in " + unirefVirusesAA50.getAbsolutePath() 
						+ " -d " + aaVirusesDb.getAbsolutePath();

				try {
					p = StreamReaderRuntime.exec(cmd, null, unirefVirusesAA50.getParentFile());
					int exitResult = p.waitFor();

					if (exitResult != 0) {
						infoT.setText("Error: could not create " + AA_VIRUSES_DB);
						return;
					}
				} catch (IOException e) {
					infoT.setText("Error: could not create " + AA_VIRUSES_DB + ". " + e.getMessage());
					return;
				} catch (InterruptedException e) {
					if (p != null)
						p.destroy();
					infoT.setText("Error: could not create " + AA_VIRUSES_DB + ". " + e.getMessage());
					return;
				}

				infoT.setText("Dimond database created.");
			}
		});

	}
}
