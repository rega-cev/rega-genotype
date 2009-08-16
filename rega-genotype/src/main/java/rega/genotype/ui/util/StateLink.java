package rega.genotype.ui.util;

import net.sf.witty.wt.SignalListener;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WMouseEvent;
import net.sf.witty.wt.WText;
import net.sf.witty.wt.i8n.WArgMessage;

public abstract class StateLink extends WText {
	String varName;
	
	public StateLink(String text, WContainerWidget parent, String varName) {
		super(new WArgMessage(text), parent);
		this.varName = varName;
		
		((WArgMessage)text()).addArgument(varName, "");
		
		this.setStyleClass("non-link");
		
		this.clicked.addListener(new SignalListener<WMouseEvent>(){
			public void notify(WMouseEvent a) {
				String value = ((WArgMessage)text()).getArgument(StateLink.this.varName);
				if(!value.equals("")) {
					clickAction(value);
				}
			}
		});
	}
	
	public void setVarValue(String value) {
		WArgMessage m = ((WArgMessage)text());
		m.changeArgument(varName, value);
		
		if(value.equals("")) {
			this.setStyleClass("non-link");
		} else {
			this.setStyleClass("link");
		}
		
		refresh();
	}
	
	public abstract void clickAction(String value);
}
