package rega.genotype.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class StreamReaderThread extends Thread {
	private InputStream in;
	private PrintStream out;
	
	public StreamReaderThread(InputStream in){
		this(in, null);
	}
	
	public StreamReaderThread(InputStream in, PrintStream out){
		this.in = in;
		this.out = out;
	}
	
	public void run(){
		try{
			BufferedReader br = new BufferedReader(
					new InputStreamReader(in));
			
			String line;
			while((line = br.readLine()) != null){
				if(out != null)
					out.println(line);
			}
			
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}
