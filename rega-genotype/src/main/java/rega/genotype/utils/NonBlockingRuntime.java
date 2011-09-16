package rega.genotype.utils;

import java.io.File;
import java.io.IOException;

public class NonBlockingRuntime {

	public static Process exec(String command, String[] envp, File dir) throws IOException{
		Runtime rt = Runtime.getRuntime();
		
		Process ps = rt.exec(command, envp, dir);

		StreamReaderThread stdout = new StreamReaderThread(ps.getInputStream());
		stdout.start();
		
		StreamReaderThread stderr = new StreamReaderThread(ps.getErrorStream());
		stderr.start();

		return ps;
	}
}
