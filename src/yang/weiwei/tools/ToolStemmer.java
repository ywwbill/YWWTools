package yang.weiwei.tools;

import java.io.IOException;

import yang.weiwei.preprocess.Stemmer;

public class ToolStemmer extends ToolInterface
{
	private String corpusFileName="";
	private String outputFileName="";
	
	public void parseCommand(String[] args)
	{
		help=findArg("--help", args, false);
		corpusFileName=getArg("--corpus", args);
		outputFileName=getArg("--output", args);
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
		
		Stemmer stemmer=new Stemmer();
		stemmer.stemFile(corpusFileName, outputFileName);
	}

	public void printHelp()
	{
		println("Arguments for Stemmer:");
		println("\t--help [optional]: Print help information.");
		println("\t--corpus: Unstemmed corpus file.");
		println("\t--output: Stemmed corpus file.");
	}
}
