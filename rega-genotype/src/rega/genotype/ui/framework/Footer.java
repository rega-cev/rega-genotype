package rega.genotype.ui.framework;

import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WText;

public class Footer extends WContainerWidget{
	public Footer(WContainerWidget parent){
		super(parent);
		init();
	}
	
	private void init(){
		new WText(tr("footer.title"),this);
	}
}
