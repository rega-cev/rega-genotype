package rega.genotype.ui.framework.widgets;

import net.sf.witty.wt.SignalListener;
import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WMouseEvent;
import net.sf.witty.wt.WPushButton;
import net.sf.witty.wt.WTextArea;

public abstract class SequenceInputWidget extends WContainerWidget{
	private WTextArea ta;
	private WPushButton run, clear;
	
	public SequenceInputWidget(WContainerWidget parent){
		super(parent);
		init();
	}
	
	private void init(){
		ta = new WTextArea(this);
		ta.setColumns(100);
		ta.setRows(15);
		
		new WBreak(this);
		run = new WPushButton(this);
		run.setText(lt("Run"));
		
		clear = new WPushButton(this);
		clear.setText(lt("Clear"));
		clear.clicked.addListener(new SignalListener<WMouseEvent>() {
			public void notify(WMouseEvent a) {
				ta.setText("");
			}

		});
		
		run.clicked.addListener(new SignalListener<WMouseEvent>() {
			public void notify(WMouseEvent a) {
				submit(ta.text());
			}

		});
	}
	
	public abstract void submit(String sequences);
}
