package crux;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;

public class Compiler {
    
	public static void main(String[] args) throws IOException, InterruptedException
	{
		//String sourceFilename = args[0];
		//crux(sourceFilename);
		for (int i=1; i<=10; ++i) {
	    	String sourceFilename = String.format("tests/private/testP%02d.crx", i);
    		crux(sourceFilename);
    		spim(sourceFilename);
    	}
    }
	
	public static void crux(String sourceFilename)
	{
		//System.out.println("Lab6 running crux on " + sourceFilename);
		
		Scanner s = null;
        try {
            s = new Scanner(new FileReader(sourceFilename));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error accessing the source file: \"" + sourceFilename + "\"");
            System.exit(-2);
        }

        Parser p = new Parser(s);
        ast.Command syntaxTree = p.parse();
        if (p.hasError()) {
        	System.out.println(p.errorReport());
        	System.exit(-3);
        }
        	
        types.TypeChecker tc = new types.TypeChecker();
        tc.check(syntaxTree);
        if (tc.hasError()) {
        	System.out.println(tc.errorReport());
        	System.exit(-4);
        }
        
        mips.CodeGen cg = new mips.CodeGen(tc);
        cg.generate(syntaxTree);
        if (cg.hasError()) {
        	System.out.println(cg.errorReport());
        	System.exit(-5);
        }
        
        String asmFilename = sourceFilename.replace(".crx", ".asm");
        try {
	        mips.Program prog = cg.getProgram();
			File asmFile = new File(asmFilename);
			PrintStream ps = new PrintStream(asmFile);
			prog.print(ps);
			ps.close();
        } catch (IOException e) {
        	e.printStackTrace();
            System.err.println("Error writing assembly file: \"" + asmFilename + "\"");
            System.exit(-6);
        }
    }
	
	public static void spim(String sourceFilename) throws IOException, InterruptedException
	{
        String asmFilename = sourceFilename.replace(".crx", ".asm");
		System.out.println("Lab6 running spim on " + asmFilename);
		
		// run spim on the output asm
		Process spim = Runtime.getRuntime().exec("spim -file " + asmFilename);
		
		// send the input to spim process
        String inputFilename = sourceFilename.replace(".crx", ".in");
        RandomAccessFile inputFile = new RandomAccessFile(inputFilename, "r");
        byte input[] = new byte[(int)inputFile.length()];
        inputFile.read(input);
        spim.getOutputStream().write(input);
        spim.getOutputStream().flush();
		inputFile.close();
		
		// wait 20 secs for spim to execute
		Thread.sleep(1000 * 20);
		
		// read back what spim produced
        String outputFilename = sourceFilename.replace(".crx", ".out");
		FileWriter outputFile = new FileWriter(outputFilename);
		BufferedReader spimOutput = new BufferedReader(new InputStreamReader(spim.getInputStream()));
		for (int i=0; i<5; ++i) // skip spim preamble
			spimOutput.readLine();
		while(spimOutput.ready()) {
			outputFile.append((char)spimOutput.read());
		}
		
		// force spim to die
		spim.destroy();
		outputFile.close();
		
	}
}
	