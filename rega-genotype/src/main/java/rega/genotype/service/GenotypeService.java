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
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.data.table.AbstractDataTableGenerator;
import rega.genotype.data.table.SequenceFilter;
import rega.genotype.util.CsvDataTable;
import rega.genotype.util.DataTable;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.utils.StreamUtils;

@SuppressWarnings("serial")
public class GenotypeService extends HttpServlet {	
	private enum Output {
		XML,
		CSV
	}
	
	private Class<? extends GenotypeTool> tool;
	private Class<? extends AbstractDataTableGenerator> tableGenerator;
	private String hivOrganism;
	private Settings settings;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		performRequest(req, resp);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		performRequest(req, resp);
	}
	
	private void performRequest(HttpServletRequest req, HttpServletResponse resp)  throws ServletException, IOException {

		String path = null;
		String toolId = null;
		if (hivOrganism == null) {		
		    if (req.getPathInfo() == null) {
		    	// url component (organism) could not be determined.
		    	resp.setStatus(404);
		    	return;
		    } else {
				path = req.getPathInfo().replace(File.separator, "");
		    	toolId = settings.getConfig().getToolConfigByUrlPath(path).getToolId();
		    	if (toolId == null) {
		    		resp.setStatus(404); // invalid tool config.
			    	return;
		    	}
		    }
		}

		if (req.getParameter("fasta-sequence") == null || toolId == null) 
			return; // no need to analize.
		
		File workingDir = File.createTempFile("gs-", "");
		workingDir.delete();
		workingDir.mkdir();
		
		try {
			File sequenceFile = new File(workingDir, "input.fasta");
			FileWriter f = new FileWriter(sequenceFile);
			f.append(req.getParameter("fasta-sequence"));
			f.close();
			
			Output output = Output.XML;
			String outputS = req.getParameter("output");
			if (outputS != null) {
				if (outputS.toLowerCase().equals("csv")) {
					output = Output.CSV;
				} else if (outputS.toLowerCase().equals("xml")) {
					output = Output.XML;
				} else {
					throw new RuntimeException("Illegal output format: " + outputS);
				}
			}
			
			File traceFile = new File(workingDir, "result.xml");

			GenotypeTool genotypeTool = (GenotypeTool) tool.getConstructor(String.class, File.class).newInstance(path, workingDir);;

			genotypeTool.analyze(sequenceFile.getAbsolutePath(), traceFile.getAbsolutePath());
			
			if (output == Output.XML) { 
				resp.setContentType("application/xml");
				StreamUtils.copy(new FileInputStream(traceFile), resp.getOutputStream());
				resp.getOutputStream().flush();			
			} else if (output == Output.CSV) { 
				DataTable dt = new CsvDataTable(resp.getOutputStream(), ',', '"');

				if (tableGenerator == null) {
					throw new ServletException("No tablegenerator was configured!");
				} else {
					SequenceFilter passAll = new SequenceFilter(){
						public boolean excludeSequence(GenotypeResultParser parser) {
							return false;
						}
					};
					
					AbstractDataTableGenerator tg = tableGenerator.getConstructor(SequenceFilter.class, DataTable.class).newInstance(passAll, dt);
					tg.parseFile(traceFile.getParentFile());
				}
			}
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

		this.hivOrganism = config.getInitParameter("Organism");

		String tableGeneratorName = config.getInitParameter("genotypeTool.table-generator");
		if (tableGeneratorName != null) {
			try {
				tableGenerator = (Class<? extends AbstractDataTableGenerator>) Class.forName(tableGeneratorName);
			} catch (ClassNotFoundException e) {
				throw new ServletException(e);
			}
		}
		
		Settings.initSettings(this.settings = Settings.getInstance(config.getServletContext()));
		
		super.init(config);
	}
}
