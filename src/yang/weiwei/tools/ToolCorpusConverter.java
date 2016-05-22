package yang.weiwei.tools;

import java.io.IOException;

import yang.weiwei.util.format.CorpusConverter;

public class ToolCorpusConverter extends ToolInterface
{
	private boolean getVocab=false;
	private boolean toIndex=false;
	private boolean toWord=false;
	
	private String wordCorpusFileName="";
	private String indexCorpusFileName="";
	private String vocabFileName="";
	
	public void parseCommand(String args[])
	{
		help=findArg("--help", args, false);
		getVocab=findArg("--get-vocab", args, false);
		toIndex=findArg("--to-index", args, false);
		toWord=findArg("--to-word", args, false);
		
		wordCorpusFileName=getArg("--word-corpus", args);
		indexCorpusFileName=getArg("--index-corpus", args);
		vocabFileName=getArg("--vocab", args);
	}
	
	protected boolean checkCommand()
	{
		if (help) return false;
		
		int numTrue=0;
		if (getVocab) numTrue++;
		if (toIndex) numTrue++;
		if (toWord) numTrue++;
		if (numTrue!=1)
		{
			println("No option or multiple options are selected.");
			return false;
		}
		
		if (wordCorpusFileName.length()==0)
		{
			println("Word corpus file is not specified.");
			return false;
		}
		
		if (vocabFileName.length()==0)
		{
			println("Vocabulary file is not specified.");
			return false;
		}
		
		if ((toIndex || toWord) && indexCorpusFileName.length()==0)
		{
			println("Index corpus file is not specified.");
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
		
		if (getVocab)
		{
			CorpusConverter.collectVocab(wordCorpusFileName, vocabFileName);
		}
		
		if (toIndex)
		{
			CorpusConverter.word2Index(wordCorpusFileName, indexCorpusFileName, vocabFileName);
		}
		
		if (toWord)
		{
			CorpusConverter.index2Word(vocabFileName, indexCorpusFileName, wordCorpusFileName);
		}
	}
	
	public void printHelp()
	{
		println("Arguments for corpus converter:");
		println("\t--help [optional]: Print help information.");
		println("\tConvert options (only one can be selected):");
		println("\t\t--get-vocab: Collect vocabulary from a given word corpus file.");
		println("\t\t--to-index: Convert a word corpus file into an indexed corpus file and collect vocabulary.");
		println("\t\t--to-word: Convert an indexed corpus file into a word corpus file given vocabulary.");
		println("\t--word-corpus: Corpus file that contains words.");
		println("\t--index-corpus: Indexed corpus file. Not required when only collecting vocabulary (i.e. --get-vocab).");
		println("\t--vocab: Vocabulary file.");
	}
}
