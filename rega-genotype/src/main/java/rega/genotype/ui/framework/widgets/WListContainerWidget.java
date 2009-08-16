package rega.genotype.ui.framework.widgets;

import java.util.ArrayList;

import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WWidget;

public class WListContainerWidget extends WContainerWidget{
	ArrayList<WContainerWidget> list = new ArrayList<WContainerWidget>(); 

	public WListContainerWidget(WContainerWidget parent){
		super(parent);
		setStyleClass("listContainer");
	}	
	
	public WContainerWidget addItem(WWidget widget){
		WContainerWidget cw = addItem();
		cw.addWidget(widget);
		return cw;
	}
	public WContainerWidget addItem(){
		return addItem(new WContainerWidget(this));
	}
	public WContainerWidget addItem(WContainerWidget cw){
		cw.setStyleClass("listContainerItem");
		list.add(cw);
		return cw;
	}
}
