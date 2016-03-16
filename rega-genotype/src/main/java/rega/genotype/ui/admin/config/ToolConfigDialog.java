package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.service.ToolRepoService;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.util.FileUtil;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WCheckBox;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WFileUpload;
import eu.webtoolkit.jwt.WFormWidget;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WProgressBar;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WValidator;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.servlet.UploadedFile;

/**
 * Edit tool configuration form.
 * 
 * @author michael
 */
public class ToolConfigDialog extends WDialog {
	private enum Mode {Add, Edit}
	private Mode mode;
	private final Template template = new Template(tr("admin.config.tool-config-dialog"));
	private final WText infoT = new WText();

	public ToolConfigDialog(final ToolConfig toolConfig) {
		show();

		mode = toolConfig == null ? Mode.Add : Mode.Edit; 
		getTitleBar().addWidget(new WText(mode == Mode.Add ? "Create Tool" : "Edit Tool"));

		final WPushButton publishB = new WPushButton("Publish", getFooter());
		final WPushButton newVersionB = new WPushButton("Create new Version", getFooter());

		final WLineEdit nameLE = new WLineEdit();
		final WLineEdit idLE = new WLineEdit();
		final WLineEdit versionLE = new WLineEdit();
		final WLineEdit urlLE = new WLineEdit();
		final WCheckBox blastChB = new WCheckBox();
		final WCheckBox autoUpdateChB = new WCheckBox();
		final WCheckBox serviceChB = new WCheckBox();
		final WCheckBox uiChB = new WCheckBox();
		final WFileUpload fileUpload = new WFileUpload();
		
		// read

		if (mode == Mode.Edit) {
			ToolManifest toolMenifest = toolConfig.getToolMenifest();
			if (toolMenifest != null) {
				nameLE.setText(toolMenifest.getName());
				idLE.setText(toolMenifest.getId());
				versionLE.setText(toolMenifest.getVersion());
				blastChB.setChecked(toolMenifest.isBlastTool());
			}
			urlLE.setText(toolConfig.getPath());
			autoUpdateChB.setChecked(toolConfig.isAutoUpdate());
			serviceChB.setChecked(toolConfig.isWebService());
			uiChB.setChecked(toolConfig.isUi());

			idLE.disable();
		} 

		// validators
		
		nameLE.setValidator(new WValidator(true));// TODO: check unique name and id on repository level (when publish).
		idLE.setValidator(new WValidator(true));
		versionLE.setValidator(new WValidator(true));
		urlLE.setValidator(new WValidator(true));

		// bind
		
		getContents().addWidget(template);
		template.bindWidget("name", nameLE);
		template.bindWidget("id", idLE);
		template.bindWidget("version", versionLE);
		template.bindWidget("url", urlLE);
		template.bindWidget("blast", blastChB);
		template.bindWidget("update", autoUpdateChB);
		template.bindWidget("ui", uiChB);
		template.bindWidget("service", serviceChB);
		template.bindWidget("upload", fileUpload);
		template.bindWidget("info", infoT);

		final String baseDir = Settings.getInstance().getBaseDir() + File.separator;
		fileUpload.setMultiple(true);
		fileUpload.setProgressBar(new WProgressBar());
		
		fileUpload.fileTooLarge().addListener(fileUpload, new Signal.Listener() {
			public void trigger() {
				infoT.setText("File too large.");
			}
		});
		
		fileUpload.uploaded().addListener(this, new Signal.Listener() {
			public void trigger() {
				String toolId =  idLE.getText() + versionLE.getText();
				String xmlDir = baseDir + "xml" + File.separator + toolId + File.separator;
				String jobDir = baseDir + "job" + File.separator + toolId + File.separator;
				
				// save xml files
				for (UploadedFile f: fileUpload.getUploadedFiles()) {
					String[] split = f.getClientFileName().split(File.separator);
					String fileName = split[split.length - 1];
					FileUtil.storeFile(new File(f.getSpoolFileName()), xmlDir + fileName);
				}

				// save manifest
				ToolManifest manifest = new ToolManifest();
				manifest.setBlastTool(blastChB.isChecked());
				manifest.setName(nameLE.getText());
				manifest.setId(idLE.getText());
				manifest.setVersion(versionLE.getText());

				// save ToolConfig
				ToolConfig newTool = new ToolConfig();
				newTool.setAutoUpdate(autoUpdateChB.isChecked());
				newTool.setConfiguration(xmlDir);
				newTool.setJobDir(jobDir);
				newTool.setPath(urlLE.getText());
				newTool.setUi(uiChB.isChecked());
				newTool.setWebService(serviceChB.isChecked());

				// save cofig
				Config config = Settings.getInstance().getConfig();
				config.addTool(newTool);

				try {
					manifest.save(xmlDir);
					config.save(baseDir);
				} catch (IOException e) {
					e.printStackTrace();
					infoT.setText("Error: Config file could not be properlly updated.");
					return;
				}

				accept();
			}
		});

		newVersionB.clicked().addListener(newVersionB, new Signal.Listener() {
			public void trigger() {
				if (!validate()) {
					return;
				}
				fileUpload.upload();
			}
		});
		publishB.clicked().addListener(newVersionB, new Signal.Listener() {
			public void trigger() {
				// create zip file 
				File zip = new File("TODO!!!!"); // TODO
				publish(zip);
			}
		});
	}

	private boolean validate() {
		for(WWidget w: template.getChildren()) {
			if (w instanceof WFormWidget
					&& ((WFormWidget) w).validate() != WValidator.State.Valid) {
				infoT.setText("Some fildes have invalid values.");
				return false;
			}
		}
		return true;
	}

	private String generatePasswiord() {
		//TODO: Koen ??
		return "TODO";
	}
	
	private boolean publish(File zipFile) {
		
		HttpClient httpClient = HttpClientBuilder.create().build();
		//String body = zipFile.
		//DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPost post = new HttpPost(ToolRepoService.gerRepoServiceUrl());
		post.addHeader(ToolRepoService.REQ_TYPE_PARAM, ToolRepoService.REQ_TYPE_PUBLISH);
		post.addHeader(ToolRepoService.TOOL_PWD_PARAM, generatePasswiord());

		try {
			//post.setEntity(new ByteArrayEntity(zipFile));
			post.setEntity(new FileEntity(zipFile, "zip"));
			HttpResponse answer = httpClient.execute(post);

			return (answer.getStatusLine().getStatusCode() != 200);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
