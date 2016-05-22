package yang.weiwei.tools;

import java.io.IOException;

import yang.weiwei.preprocess.StopList;

public class ToolStoplist extends ToolInterface
{
	private String dictFileName="";
	private String corpusFileName="";
	private String outputFileName="";
	
	public void parseCommand(String[] args)
	{
		help=findArg("--help", args, false);
		dictFileName=getArg("--model", args);
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
		
		StopList stoplist=(dictFileName.length()>0? new StopList(dictFileName) : new StopList());
		stoplist.removeStopWords(corpusFileName, outputFileName);
	}

	public void printHelp()
	{
		println("Arguments for Stoplist:");
		println("\t--help [optional]: Print help information.");
		println("\t--dict [optional]: Use user's stop word dictionary to remove stop words.");
		println("\t--corpus: Corpus file with stop words.");
		println("\t--output: Corpus file without stop words.");
	}
}
