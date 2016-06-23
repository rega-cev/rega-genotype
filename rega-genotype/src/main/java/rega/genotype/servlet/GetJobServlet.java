package rega.genotype.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class GetJobServlet extends HttpServlet {

	private static final long serialVersionUID = 1959942053030342948L;
	protected String dir = new File(".").getAbsolutePath();

	public void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		res.setContentType("text/html");// setting the content type

		try{
			
		String path = dir + "/base-work-dir/job/"
				+ req.getParameter("job_dir") + "/" + req.getParameter("job_id") + "/" + req.getParameter("file");
		
		PrintWriter out = res.getWriter();
		FileInputStream fis = new FileInputStream(path);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		while (true) {
			String s = br.readLine();
			if (s == null)
				break;
			out.println(s);
			out.flush();
		}
		fis.close();

		out.close();
		res.setContentType("text/xml");
		res.setHeader("Cache-Control", "no-cache");
		} catch (Exception e ) {
			res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		res.sendRedirect("/");
	}
}
