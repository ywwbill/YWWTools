package yang.weiwei.tools;

import java.io.IOException;

import yang.weiwei.preprocess.Lemmatizer;

public class ToolLemmatizer extends ToolInterface
{
	private String dictFileName="";
	private String corpusFileName="";
	private String outputFileName="";
	
	public void parseCommand(String[] args)
	{
		help=findArg("--help", args, false);
		dictFileName=getArg("--dict", args);
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
		
		Lemmatizer lemmatizer=(dictFileName.length()>0? new Lemmatizer(dictFileName) : new Lemmatizer());
		lemmatizer.lemmatizeFile(corpusFileName, outputFileName);
	}

	public void printHelp()
	{
		println("Arguments for Lemmatizer:");
		println("\t--help [optional]: Print help information.");
		println("\t--dict [optional]: Use user's model to lemmatize documents.");
		println("\t--corpus: Unlemmatized, tokenized, and POS-tagged corpus file.");
		println("\t--output: Lemmatized corpus file.");
	}
}
