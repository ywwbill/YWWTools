package yang.weiwei.tools;

import java.io.IOException;

import yang.weiwei.ICTCLAS.ICTCLAS;

public class ToolICTCLAS extends ToolInterface
{
	private String dictFileName="";
	private String corpusFileName="";
	private String outputFileName="";
	
	public void parseCommand(String args[])
	{
		dictFileName=getArg("--dict", args);
		corpusFileName=getArg("--corpus", args);
		outputFileName=getArg("--output", args);
		help=findArg("--help", args, false);
	}
	
	protected boolean checkCommand()
	{
		if (help) return false;
		
		if (corpusFileName.length()==0)
		{
			println("Corpus file is not specified.");
			return false;
		}
		
		if (outputFileName.length()==0)
		{
			println("Output file is not specified.");
			return false;
		}
		
		return true;
	}
	
	public void execute() throws IOException
	{
		if (!checkCommand())
		{
			printHelp();
			return;
		}
		
		ICTCLAS ictclas=new ICTCLAS();
		if (dictFileName.length()>0)
		{
			ictclas.importUserDictFile(dictFileName);
		}
		ictclas.fileProcess(corpusFileName, outputFileName);
	}
	
	public void printHelp()
	{
		println("Arguments for ICTCLAS:");
		println("\t--help [optional]: Print help information.");
		println("\t--dict [optional]: Dictionary file.");
		println("\t--corpus: Tokenized corpus file.");
		println("\t--output: Result file.");
	}
}
