package yang.weiwei.tools;

import java.io.IOException;

import yang.weiwei.preprocess.Tokenizer;

public class ToolTokenizer extends ToolInterface
{
	private String modelFileName="";
	private String corpusFileName="";
	private String outputFileName="";
	
	public void parseCommand(String[] args)
	{
		help=findArg("--help", args, false);
		modelFileName=getArg("--model", args);
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
		
		Tokenizer tokenizer=(modelFileName.length()>0? new Tokenizer(modelFileName) : new Tokenizer());
		tokenizer.tokenizeFile(corpusFileName, outputFileName);
	}

	public void printHelp()
	{
		println("Arguments for Tokenizer:");
		println("\t--help [optional]: Print help information.");
		println("\t--model [optional]: Use user's model to tokenize documents.");
		println("\t--corpus: Untokenized corpus file.");
		println("\t--output: Tokenized corpus file.");
	}
}
