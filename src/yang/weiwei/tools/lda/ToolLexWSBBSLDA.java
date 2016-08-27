package yang.weiwei.tools.lda;

import java.io.IOException;

import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.slda.lex_wsb_bs_lda.LexWSBBSLDA;

public class ToolLexWSBBSLDA extends ToolBSLDA
{
	protected double _alpha=1.0;
	protected double a=1.0;
	protected double b=1.0;
	protected double gamma=1.0;
	protected int numBlocks=10;
	protected boolean directed=false;
	
	protected String wsbmGraphFileName="";
	protected String outputWSBMFileName=""; 
	
	public void parseCommand(String[] args)
	{
		super.parseCommand(args);
		
		_alpha=getArg("--alpha-prime", args, 1.0);
		a=getArg("-a", args, 1.0);
		b=getArg("-b", args, 1.0);
		gamma=getArg("-g", args, 1.0);
		numBlocks=getArg("--blocks", args, 10);
		directed=findArg("--directed", args, false);
		
		wsbmGraphFileName=getArg("--wsbm-graph", args);
		outputWSBMFileName=getArg("--output-wsbm", args);
	}
	
	public boolean checkCommand()
	{
		if (!super.checkCommand()) return false;
		
		if (_alpha<=0.0)
		{
			println("Hyperparameter alpha' must be a positive real number.");
			return false;
		}
		
		if (a<=0.0)
		{
			println("Hyperparameter a must be a positive real number.");
			return false;
		}
		
		if (b<=0.0)
		{
			println("Hyperparameter b must be a positive real number.");
			return false;
		}
		
		if (gamma<=0.0)
		{
			println("Hyperparameter gamma must be a positive real number.");
			return false;
		}
		
		if (numBlocks<=0)
		{
			println("Number of blocks must be a positive integer.");
			return false;
		}
		
		return true;
	}
	
	protected LDAParam createParam() throws IOException
	{
		LDAParam param=super.createParam();
		param._alpha=_alpha;
		param.a=a;
		param.b=b;
		param.gamma=gamma;
		param.numBlocks=numBlocks;
		param.directed=directed;
		return param;
	}
	
	public void execute() throws IOException
	{
		if (!checkCommand())
		{
			printHelp();
			return;
		}
		
		LDAParam param=createParam();
		LexWSBBSLDA lda=null;
		if (!test)
		{
			lda=new LexWSBBSLDA(param);
			lda.readCorpus(corpusFileName);
			lda.readLabels(labelFileName);
			if (wsbmGraphFileName.length()>0) lda.readBlockGraph(wsbmGraphFileName);
			lda.initialize();
			lda.sample(numIters);
			lda.writeModel(modelFileName);
		}
		else
		{
			lda=new LexWSBBSLDA(modelFileName, param);
			lda.readCorpus(corpusFileName);
			if (labelFileName.length()>0) lda.readLabels(labelFileName);
			if (wsbmGraphFileName.length()>0) lda.readBlockGraph(wsbmGraphFileName);
			lda.initialize();
			lda.sample(numIters);
		}
		writeFiles(lda);
	}
	
	protected void writeFiles(LexWSBBSLDA lda) throws IOException
	{
		super.writeFiles(lda);
		if (wsbmGraphFileName.length()>0 && outputWSBMFileName.length()>0)
		{
			lda.writeBlocks(outputWSBMFileName);
		}
	}
	
	public void printHelp()
	{
		super.printHelp();
		println("Lex-WSB-BS-LDA arguments:");
		println("\t--wsbm-graph [optional]: Link file for WSBM to find blocks.");
		println("\t--alpha-prime [optional]: Parameter of Dirichlet prior of block distribution over topics (default: 1.0).");
		println("\t-a [optional]: Parameter of Gamma prior for block link rates (default: 1.0).");
		println("\t-b [optional]: Parameter of Gamma prior for block link rates (default: 1.0).");
		println("\t-g [optional]: Parameter of Dirichlet prior for block distribution (default: 1.0).");
		println("\t--blocks [optional]: Number of blocks (default: 10).");
		println("\t--directed [optional]: Set all edges directed (default: false).");
		println("\t--output-wsbm [optional]: File for WSBM-identified blocks.");
	}
}
