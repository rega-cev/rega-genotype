package rega.genotype.ui.framework;

import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WText;

public class Navigation extends WContainerWidget{

	public Navigation (WContainerWidget parent){
		super(parent);
		init();
	}
	
	private void init(){
		new WText(tr("navigation.title"),this);
	}
}
