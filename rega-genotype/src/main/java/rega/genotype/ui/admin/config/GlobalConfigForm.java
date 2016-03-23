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
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WPushButton;

public class GlobalConfigForm extends WContainerWidget{

	public GlobalConfigForm() {
		super();
		GeneralConfig generalConfig = Settings.getInstance().getConfig().getGeneralConfig();
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
						Config config = Settings.getInstance().getConfig();
						config.save(Settings.getInstance().getBaseDir() + File.separator);
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
