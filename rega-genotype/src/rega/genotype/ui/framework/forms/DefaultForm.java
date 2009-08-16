package rega.genotype.ui.framework.forms;

import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WText;

public class DefaultForm extends WContainerWidget{
	public DefaultForm(WContainerWidget parent){
		super(parent);
		init();
	}
	
	private void init(){
		new WText(tr("form.default.title"),this);
	}
}
