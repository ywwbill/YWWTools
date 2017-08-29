package yang.weiwei.tools;

import java.io.IOException;

import yang.weiwei.tlda.TLDAParam;
import yang.weiwei.tlda.TLDA;

public class ToolTLDA extends ToolInterface
{
	private boolean test=false;
	private boolean verbose=true;
	
	private double alpha=0.01;
	private double beta=0.01;
	private int numTopics=10;
	private boolean updateAlpha=false;
	private int updateAlphaInterval=10;
	private int numIters=100;
	
	private String treePriorFileName="";
	private String vocabFileName="";
	private String corpusFileName="";
	private String modelFileName="";
	
	private String thetaFileName="";
	private String topicFileName="";
	private String topicCountFileName="";
	private int numTopWords=10;
	
	public void parseCommand(String[] args)
	{
		help=findArg("--help", args, false);
		test=findArg("--test", args, false);
		verbose=findArg("--no-verbose", args, true);
		
		alpha=getArg("--alpha", args, 0.01);
		beta=getArg("--beta", args, 0.01);
		numTopics=getArg("--topics", args, 10);
		updateAlpha=findArg("--update", args, false);
		updateAlphaInterval=getArg("--update-int", args, 10);
		numIters=getArg("--iters", args, 100);
		
		treePriorFileName=getArg("--tree", args);
		vocabFileName=getArg("--vocab", args);
		corpusFileName=getArg("--corpus", args);
		modelFileName=getArg("--trained-model", args);
		
		thetaFileName=getArg("--theta", args);
		topicFileName=getArg("--output-topic", args);
		topicCountFileName=getArg("--topic-count", args);
		numTopWords=getArg("--top-word", args, 10);
	}

	protected boolean checkCommand()
	{
		if (help) return false;
		
		if (treePriorFileName.length()==0)
		{
			println("Tree prior file is not specified.");
			return false;
		}
		
		if (vocabFileName.length()==0)
		{
			println("Vocabulary file is not specified.");
			return false;
		}
		
		if (corpusFileName.length()==0)
		{
			println("Corpus file is not specified.");
			return false;
		}
		
		if (modelFileName.length()==0)
		{
			println("Model file is not specified.");
			return false;
		}
		
		if (alpha<=0.0)
		{
			println("Hyperparameter alpha must be a positive real number.");
			return false;
		}
		
		if (beta<=0.0)
		{
			println("Hyperparameter beta must be a positive real number.");
			return false;
		}
		
		if (numTopics<=0)
		{
			println("Number of topics must be a positive integer.");
			return false;
		}
		
		if (numIters<=0)
		{
			println("Number of iterations must be a positive integer.");
			return false;
		}
		
		if (updateAlphaInterval<=0)
		{
			println("Interval of updating alpha must be a positive integer.");
			return false;
		}
		
		if (numTopWords<=0)
		{
			println("Number of top words must be a positive integer.");
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
		
		TLDAParam param=new TLDAParam(vocabFileName, treePriorFileName);
		param.alpha=alpha;
		param.beta=beta;
		param.numTopics=numTopics;
		param.verbose=verbose;
		param.updateAlpha=updateAlpha;
		param.updateAlphaInterval=updateAlphaInterval;
		TLDA tlda=null;
		if (!test)
		{
			tlda=new TLDA(param);
			tlda.readCorpus(corpusFileName);
			tlda.initialize();
			tlda.sample(numIters);
			tlda.writeModel(modelFileName);
			if (topicFileName.length()>0)
			{
				tlda.writeWordResult(topicFileName, numTopWords);
			}
		}
		else
		{
			tlda=new TLDA(modelFileName, param);
			tlda.readCorpus(corpusFileName);
			tlda.initialize();
			tlda.sample(numIters);
		}
		if (thetaFileName.length()>0)
		{
			tlda.writeDocTopicDist(thetaFileName);
		}
		if (topicCountFileName.length()>0)
		{
			tlda.writeDocTopicCounts(topicCountFileName);
		}
	}

	public void printHelp()
	{
		println("Arguments for tLDA:");
		println("\t--help [optional]: Print help information.");
		println("\t--test [optional]: Use the model for test (default: false).");
		println("\t--no-verbose [optional]: Stop printing log to console.");
		println("\t--tree: Tree prior file.");
		println("\t--vocab: Vocabulary file.");
		println("\t--corpus: Corpus file");
		println("\t--trained-model: Model file.");
		
		println("\t--alpha [optional]: Parameter of Dirichlet prior of document distribution over topics (default: 1.0).");
		println("\t--beta [optional]: Parameter of Dirichlet prior of topic distribution over words (default: 0.1).");
		println("\t--topics [optional]: Number of topics (default: 10).");
		println("\t--iters [optional]: Number of iterations (default: 100).");
		println("\t--update [optional]: Update alpha while sampling (default: false).");
		println("\t--update-int [optional]: Interval of updating alpha (default: 10).");
		println("\t--theta [optional]: File for document distribution over topics.");
		println("\t--output-topic [optional]: File for showing topics.");
		println("\t--top-word [optional]: Number of words to give when showing topics (default: 10).");
		println("\t--topic-count [optional]: File for document-topic counts.");
	}
}
