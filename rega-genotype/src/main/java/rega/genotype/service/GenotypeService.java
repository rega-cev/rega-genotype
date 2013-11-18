package rega.genotype.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;

import rega.genotype.GenotypeTool;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.utils.StreamUtils;

@SuppressWarnings("serial")
public class GenotypeService extends HttpServlet {	
	private Class<? extends GenotypeTool> tool;
	private String organism;
	private Settings settings;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		File workingDir = File.createTempFile("gs-", "");
		workingDir.delete();
		workingDir.mkdir();
		
		try {
			File sequenceFile = new File(workingDir, "input.fasta");
			FileWriter f = new FileWriter(sequenceFile);
			f.append(req.getParameter("fasta-sequence"));
			f.close();
			
			File traceFile = new File(workingDir, "result.xml");

			GenotypeTool genotypeTool;
			if (organism == null)
				genotypeTool = (GenotypeTool) tool.getConstructor(File.class).newInstance(workingDir);
			else
				genotypeTool = (GenotypeTool) tool.getConstructor(String.class, File.class).newInstance(organism, workingDir);

			genotypeTool.analyze(sequenceFile.getAbsolutePath(), traceFile.getAbsolutePath());
			
			resp.setContentType("application/xml");
			StreamUtils.copy(new FileInputStream(traceFile), resp.getOutputStream());
			resp.getOutputStream().flush();			
		} catch (IllegalArgumentException e) {
			throw new ServletException(e);
		} catch (SecurityException e) {
			throw new ServletException(e);
		} catch (InstantiationException e) {
			throw new ServletException(e);
		} catch (IllegalAccessException e) {
			throw new ServletException(e);
		} catch (InvocationTargetException e) {
			throw new ServletException(e);
		} catch (NoSuchMethodException e) {
			throw new ServletException(e);
		} finally {
			FileUtils.deleteDirectory(workingDir);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void init(ServletConfig config) throws ServletException {

		String toolName = config.getInitParameter("genotypeTool");
		if (toolName != null)
			try {
				tool = (Class<? extends GenotypeTool>) Class.forName(toolName);
			} catch (ClassNotFoundException e) {
				throw new ServletException(e);
			}
		else
			throw new ServletException("Need 'genotypeTool' parameter");

		this.organism = config.getInitParameter("Organism");

		Settings.initSettings(this.settings = Settings.getInstance(config));
		
		super.init(config);
	}
}
