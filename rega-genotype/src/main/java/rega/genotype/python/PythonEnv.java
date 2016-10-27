package rega.genotype.python;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import rega.genotype.singletons.Settings;
import rega.genotype.utils.FileUtil;

public class PythonEnv {
	public PythonEnv(){}

	public void execPython(String pythonClass ,String[] args) throws Exception {
		InputStream in = getClass().getResourceAsStream(pythonClass); 
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String number_fasta;

		number_fasta = FileUtil.toString(reader);

		PySystemState sys = Py.getSystemState();
		String bioPythonPath = Settings.getInstance().getConfig().getGeneralConfig().getBioPythonPath();
		if (!bioPythonPath.isEmpty())
			sys.path.append(new PyString(bioPythonPath));

        for (String arg: args)
        	sys.argv.append(new PyString(arg));
		
		PythonInterpreter python = new PythonInterpreter();
		python.exec(number_fasta);

		python.close();
	}
}
