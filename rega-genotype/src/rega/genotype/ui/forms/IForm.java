package rega.genotype.ui.forms;

import java.io.File;

import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WText;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.framework.GenotypeWindow;

public abstract class IForm extends WContainerWidget {
	private WText title;
	private GenotypeWindow main;

	public IForm(GenotypeWindow main, String title) {
		this.main = main;
		this.title = new WText(main.getResourceManager().getOrganismValue(title, "title"), this);
		this.title.setStyleClass("header-mainTitle");
		new WBreak(this);
	}
	
	public GenotypeWindow getMain() {
		return main;
	}
}
