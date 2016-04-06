package rega.genotype.ui.framework.widgets;

import eu.webtoolkit.jwt.WFormWidget;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WValidator;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.WValidator.Result;

/**
 * Standard form.
 * 
 * @author michael
 */
public class FormTemplate extends Template {
	public FormTemplate(CharSequence text) {
		super(text);
	}

	public void initInfoFields() {
		for(int i = getChildren().size() - 1; i  >= 0; --i) {
			WWidget w =  getChildren().get(i);
			if (w instanceof WFormWidget){
				String var = varName(w);
				bindWidget(var + "-info", new WText());
			}
		}
	}
	
	public boolean validate() {
		boolean ans = true;
	
		for(WWidget w: getChildren()) {
			if (w instanceof WFormWidget){
				WFormWidget fw  = (WFormWidget) w;

				if (fw.validate() != WValidator.State.Valid) 
					ans = false;

				String var = varName(w);
				WText info = (WText) resolveWidget(var + "-info");
				if (info != null && fw.getValidator() != null) {
					if (fw.getValueText() == null)
						info.setText("");
					else {
						Result r = fw.getValidator().validate(fw.getValueText());
						info.setText(r == null ? "" :r.getMessage());
						info.addStyleClass("form-error");
					}
				}
			}
		}
		return ans;
	}
}
