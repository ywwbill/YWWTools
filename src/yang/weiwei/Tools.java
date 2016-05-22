package yang.weiwei;

import java.io.IOException;
import java.util.HashSet;

import yang.weiwei.tools.ToolLDAInterface;
import yang.weiwei.tools.ToolWSBM;
import yang.weiwei.tools.ToolSCC;
import yang.weiwei.tools.ToolStoplist;
import yang.weiwei.tools.ToolLemmatizer;
import yang.weiwei.tools.ToolPOSTagger;
import yang.weiwei.tools.ToolStemmer;
import yang.weiwei.tools.ToolTokenizer;
import yang.weiwei.tools.ToolCorpusConverter;
import yang.weiwei.tools.ToolInterface;
import yang.weiwei.tools.ToolICTCLAS;

public class Tools extends ToolInterface
{	
	private static HashSet<String> toolSet;
	
	private String tool="";
	private String args[];
	
	public void parseCommand(String args[])
	{
		tool=getArg("--tool", args).toLowerCase();
		help=findArg("--help", args, false);
		this.args=new String[args.length];
		for (int i=0; i<args.length; i++)
		{
			this.args[i]=args[i];
		}
	}
	
	protected boolean checkCommand()
	{
		if (tool.length()==0 || !toolSet.contains(tool))
		{
			if (!help || tool.length()>0) println("Tool name is not specified or does not exist.");
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
		
		ToolInterface toolImpl=null;
		switch (tool)
		{
		case "lda": toolImpl=new ToolLDAInterface(); break;
		case "wsbm": toolImpl=new ToolWSBM(); break;
		case "scc": toolImpl=new ToolSCC(); break;
		case "stoplist": toolImpl=new ToolStoplist(); break;
		case "lemmatizer": toolImpl=new ToolLemmatizer(); break;
		case "pos-tagger": toolImpl=new ToolPOSTagger(); break;
		case "stemmer": toolImpl=new ToolStemmer(); break;
		case "tokenizer": toolImpl=new ToolTokenizer(); break;
		case "corpus-converter": toolImpl=new ToolCorpusConverter(); break;
		case "ictclas": toolImpl=new ToolICTCLAS(); break;
		}
		if (toolImpl!=null)
		{
			toolImpl.parseCommand(args);
			toolImpl.execute();
		}
	}
	
	public void printHelp()
	{
		println("Arguments for Tools:");
		println("\t--help [optional]: Print help information.");
		println("\t--tool: Name of tool you want to use. Supported tools are");
		println("\t\tICTCLAS: Chinese POS tagger.");
		println("\t\tFile-Merger: Merge several files into a single one.");
		println("\t\tLemmatizer: Lemmatize POS-tagged corpus. Support English only, but can support other languages given dictionary.");
		println("\t\tPOS-Tagger: Tag words' POS. Support English only, but can support other languages given trained models.");
		println("\t\tStemmer: Stem words. Support English only, but can support other languages given trained models.");
		println("\t\tStoplist: Remove stop words. Support English only, but can support other languages given dictionary.");
		println("\t\tTokenizer: Tokenize corpus. Support English only, but can support other languages given trained models.");
		println("\t\tCorpus-Converter: Convert word corpus into indexed corpus (for LDA) and vice versa.");
		println("\t\tSCC: Strongly connected components.");
		println("\t\tWSBM: Weighted stochastic block model. Find blocks in a network.");
		println("\t\tLDA: Latent Dirichlet allocation. Include a variety of extensions.");
	}
	
	static
	{
		toolSet=new HashSet<String>();
		toolSet.add("ictclas");
		toolSet.add("file-merger");
		toolSet.add("lemmatizer");
		toolSet.add("pos-tagger");
		toolSet.add("stemmer");
		toolSet.add("stoplist");
		toolSet.add("tokenizer");
		toolSet.add("corpus-converter");
		toolSet.add("scc");
		toolSet.add("wsbm");
		toolSet.add("lda");
	}
	
	public static void main(String args[]) throws IOException
	{
		Tools tool=new Tools();
		tool.parseCommand(args);
		tool.execute();
	}
}
