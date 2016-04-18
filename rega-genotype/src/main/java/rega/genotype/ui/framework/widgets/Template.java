package rega.genotype.ui.framework.widgets;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.util.FileServlet;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTemplate;

/**
 * <pre>
 * WTemplate that replaces ${resource-file} with file Servlet url. 
 * Examples:
 * <img src="${resource-file}/HCV2/genome_0.png" > </img> 
 * <embed src="${resource-file}/HCV2/genome_0.png" width="400px" height="300px"></embed>
 * </pre>
 */
public class Template extends WTemplate{

	public Template() {
		this(null, null);
	}

	public Template(CharSequence text){
		this(text, null);
	}

	public Template(CharSequence text, WContainerWidget parent) {
		super(text, parent);
		addFunction("tr", WTemplate.Functions.tr);
		addFunction("id", WTemplate.Functions.id);
		addFunction("block", WTemplate.Functions.block);
	}

	@Override
	public void resolveString(String varName, List<WString> args, Writer result)
			throws IOException {
		
		
		GenotypeApplication app = GenotypeApplication.getGenotypeApplication();
		if (varName.equals("resource-file"))
			bindString(varName, FileServlet.getFileUrl(app.getToolConfig().getPath()));

		super.resolveString(varName, args, result);
	}
}
