package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import rega.genotype.config.Config;
import rega.genotype.config.Config.GeneralConfig;
import rega.genotype.ui.framework.widgets.AutoForm;
import rega.genotype.ui.framework.widgets.MsgDialog;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WPushButton;

public class GlobalConfigForm extends WContainerWidget{

	public GlobalConfigForm() {
		super();
		final Config config;
		if (Settings.getInstance().getConfig() == null){
			config = new Config();
			config.setGeneralConfig(new GeneralConfig());
		} else 
			config = Settings.getInstance().getConfig();

		GeneralConfig generalConfig = config.getGeneralConfig();
		final AutoForm<Config.GeneralConfig> form = new AutoForm<Config.GeneralConfig>(generalConfig) {
			@Override
			protected Set<String> getIgnoredFields() {
				Set<String> ignore = new HashSet<String>();
				ignore.add("publisherPassword");
				return ignore;
			}
		};
		addWidget(form);
		
		WPushButton saveB = new WPushButton("Save", this);
		saveB.clicked().addListener(saveB, new Signal.Listener() {
			public void trigger() {
				if (form.save()) {
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
}
