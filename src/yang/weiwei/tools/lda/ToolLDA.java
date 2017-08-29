package yang.weiwei.tools.lda;

import java.io.IOException;

import yang.weiwei.lda.LDA;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.tools.ToolLDAInterface;

public class ToolLDA extends ToolLDAInterface
{
	//general
	protected boolean test=false;
	protected boolean verbose=true;
	
	//basic parameter
	protected double alpha=1.0;
	protected double beta=0.1;
	protected int numTopics=10;
	protected boolean updateAlpha=false;
	protected int updateAlphaInterval=10;
	protected int numIters=100;
	
	//basic configure
	protected String vocabFileName="";
	protected String corpusFileName="";
	protected String modelFileName="";
	
	//basic optional configure
	protected String thetaFileName="";
	protected String topicFileName="";
	protected int numTopWords=10;
	protected String topicCountFileName="";
	
	public void parseCommand(String[] args)
	{
		help=findArg("--help", args, false);
		test=findArg("--test", args, false);
		verbose=findArg("--no-verbose", args, true);
		
		alpha=getArg("--alpha", args, 1.0);
		beta=getArg("--beta", args, 0.1);
		numTopics=getArg("--topics", args, 10);
		updateAlpha=findArg("--update", args, false);
		updateAlphaInterval=getArg("--update-int", args, 10);
		numIters=getArg("--iters", args, 100);
		
		vocabFileName=getArg("--vocab", args);
		corpusFileName=getArg("--corpus", args);
		modelFileName=getArg("--trained-model", args);
		
		thetaFileName=getArg("--theta", args);
		topicFileName=getArg("--output-topic", args);
		numTopWords=getArg("--top-word", args, 10);
		topicCountFileName=getArg("--topic-count", args);
	}
	
	protected boolean checkCommand()
	{
		if (!super.checkCommand()) return false;
		
		if (help) return false;
		
		if (model.length()==0)
		{
			model="lda";
		}
		
		if (!ldaNames.contains(model))
		{
			println("Model is not supported.");
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
	
	protected LDAParam createParam() throws IOException
	{
		LDAParam param=new LDAParam(vocabFileName);
		param.alpha=alpha;
		param.beta=beta;
		param.numTopics=numTopics;
		param.verbose=verbose;
		param.updateAlpha=updateAlpha;
		param.updateAlphaInterval=updateAlphaInterval;
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
		LDA lda=null;
		if (!test)
		{
			lda=new LDA(param);
			lda.readCorpus(corpusFileName);
			lda.initialize();
			lda.sample(numIters);
			lda.writeModel(modelFileName);
		}
		else
		{
			lda=new LDA(modelFileName, param);
			lda.readCorpus(corpusFileName);
			lda.initialize();
			lda.sample(numIters);
		}
		writeFiles(lda);
	}
	
	protected void writeFiles(LDA lda) throws IOException
	{
		if (!test && topicFileName.length()>0) lda.writeResult(topicFileName, numTopWords);
		if (thetaFileName.length()>0) lda.writeDocTopicDist(thetaFileName);
		if (topicCountFileName.length()>0) lda.writeDocTopicCounts(topicCountFileName);
	}

	public void printHelp()
	{
		println("Arguments for LDA:");
		println("Basic arguments:");
		println("\t--help [optional]: Print help information.");
		println("\t--model [optional]: The topic model you want to use (default: LDA). Supported models are");
		println("\t\tLDA: Vanilla LDA");
		println("\t\tBP-LDA: LDA with block priors. Blocks are pre-computed.");
		println("\t\tST-LDA: Single topic LDA. Each document can only be assigned to one topic.");
		println("\t\tWSB-TM: LDA with block priors. Blocks are computed by WSBM.");
		println("\t\tRTM: Relational topic model.");
		println("\t\t\tLex-WSB-RTM: RTM with WSB-computed block priors and lexical weights.");
		println("\t\t\tLex-WSB-Med-RTM: Lex-WSB-RTM with hinge loss.");
		println("\t\tSLDA: Supervised LDA. Support multi-class classification.");
		println("\t\t\tBS-LDA: Binary SLDA.");
		println("\t\t\tLex-WSB-BS-LDA: BS-LDA with WSB-computed block priors and lexical weights.");
		println("\t\t\tLex-WSB-Med-LDA: Lex-WSB-BS-LDA with hinge loss.");
		println("\t--test [optional]: Use the model for test (default: false).");
		println("\t--no-verbose [optional]: Stop printing log to console.");
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
