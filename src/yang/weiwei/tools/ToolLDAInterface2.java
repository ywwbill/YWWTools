package yang.weiwei.tools;

import java.util.HashSet;
import java.io.IOException;

import yang.weiwei.lda.LDA;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.bp_lda.BPLDA;
import yang.weiwei.lda.st_lda.STLDA;
import yang.weiwei.lda.wsb_tm.WSBTM;
import yang.weiwei.lda.rtm.RTM;
import yang.weiwei.lda.rtm.lex_wsb_rtm.LexWSBRTM;
import yang.weiwei.lda.rtm.lex_wsb_med_rtm.LexWSBMedRTM;
import yang.weiwei.lda.slda.SLDA;
import yang.weiwei.lda.slda.bs_lda.BSLDA;
import yang.weiwei.lda.slda.lex_wsb_bs_lda.LexWSBBSLDA;
import yang.weiwei.lda.slda.lex_wsb_med_lda.LexWSBMedLDA;

public class ToolLDAInterface2 extends ToolInterface
{
	private static HashSet<String> ldaNames;
	private static HashSet<String> sldaNames;
	private static HashSet<String> rtmNames;
	
	//general
	private String model="";
	private boolean test=false;
	private boolean verbose=true;
	
	//basic parameter
	private double alpha=1.0;
	private double _alpha=1.0;
	private double beta=0.1;
	private int numTopics=10;
	private boolean updateAlpha=false;
	private int updateAlphaInterval=10;
	private int numIters=100;
	
	//hinge loss parameter
	private double c=1.0;
	
	//RTM and SLDA parameter
	private double nu=1.0;
	
	//RTM and WSBM parameter
	private boolean directed=false;
	
	//RTM parameter
	private int PLRInterval=20;
	private boolean negEdge=false;
	private double negEdgeRatio=1.0;
	
	//SLDA parameter
	private double sigma=1.0;
	
	//WSBM parameter
	private double a=1.0;
	private double b=1.0;
	private double gamma=1.0;
	private int numBlocks=10;
	
	//basic configure
	private String vocabFileName="";
	private String corpusFileName="";
	private String modelFileName="";
	
	//BP-LDA configure
	private String blockFileName="";
	
	//SLDA configure
	private String labelFileName="";
	
	//RTM configure
	private String rtmTrainGraphFileName="";
	private String rtmTestGraphFileName="";
	
	//WSBM configure
	private String wsbmGraphFileName="";
	
	//basic optional configure
	private String thetaFileName="";
	private String topicFileName="";
	private int numTopWords=10;
	
	//RTM and SLDA optional configure
	private String predFileName="";
	
	//WSBM optional configure
	private String outputWSBMFileName=""; 
	
	public void parseCommand(String[] args)
	{
		help=findArg("--help", args, false);
		model=getArg("--model", args).toLowerCase();
		test=findArg("--test", args, false);
		verbose=findArg("--no-verbose", args, true);
		
		alpha=getArg("--alpha", args, 1.0);
		_alpha=getArg("--alpha-prime", args, 1.0);
		beta=getArg("--beta", args, 0.1);
		numTopics=getArg("--topics", args, 10);
		updateAlpha=findArg("--update", args, false);
		updateAlphaInterval=getArg("--update-int", args, 10);
		numIters=getArg("--iters", args, 100);
		
		c=getArg("-c", args, 1.0);
		nu=getArg("--nu", args, 1.0);
		directed=findArg("--directed", args, false);
		
		PLRInterval=getArg("--plr-int", args, 20);
		negEdge=findArg("--neg", args, false);
		negEdgeRatio=getArg("--neg-ratio", args, 1.0);
		
		sigma=getArg("--sigma", args, 1.0);
		
		a=getArg("-a", args, 1.0);
		b=getArg("-b", args, 1.0);
		gamma=getArg("-g", args, 1.0);
		numBlocks=getArg("--blocks", args, 10);
		
		vocabFileName=getArg("--vocab", args);
		corpusFileName=getArg("--corpus", args);
		modelFileName=getArg("--trained-model", args);
		
		blockFileName=getArg("--block-graph", args);
		labelFileName=getArg("--label", args);
		rtmTrainGraphFileName=getArg("--rtm-train-graph", args);
		rtmTestGraphFileName=getArg("--rtm-test-graph", args);
		wsbmGraphFileName=getArg("--wsbm-graph", args);
		
		thetaFileName=getArg("--theta", args);
		topicFileName=getArg("--output-topic", args);
		numTopWords=getArg("--top-word", args, 10);
		
		predFileName=getArg("--pred", args);
		outputWSBMFileName=getArg("--output-wsbm", args);
	}

	protected boolean checkCommand()
	{
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
		
		if (model.equals("bp-lda") && blockFileName.length()==0 && !test)
		{
			println("Block file is not specified.");
			return false;
		}
		
		if (sldaNames.contains(model) && labelFileName.length()==0 && !test)
		{
			println("Label file is not specified.");
			return false;
		}
		
		if (rtmNames.contains(model) && rtmTrainGraphFileName.length()==0 && !test)
		{
			println("RTM train graph file is not specified.");
			return false;
		}
		
		if (!test && rtmTestGraphFileName.length()==0)
		{
			rtmTestGraphFileName=rtmTrainGraphFileName;
		}
		
		if (rtmNames.contains(model) && rtmTestGraphFileName.length()==0 && test)
		{
			println("RTM test graph is not specified.");
			return false;
		}
		
		if (model.equals("wsb-tm") && wsbmGraphFileName.length()==0 && !test)
		{
			println("Graph file for WSBM is not specified.");
			return false;
		}
		
		return true;
	}
	
	public LDAParam createParam() throws IOException
	{
		LDAParam param=new LDAParam(vocabFileName);
		param.alpha=alpha;
		param._alpha=_alpha;
		param.beta=beta;
		param.numTopics=numTopics;
		param.verbose=verbose;
		param.updateAlpha=updateAlpha;
		param.updateAlphaInterval=updateAlphaInterval;
		param.c=c;
		param.nu=nu;
		param.showPLRInterval=PLRInterval;
		param.negEdge=negEdge;
		param.negEdgeRatio=negEdgeRatio;
		param.sigma=sigma;
		param.directed=directed;
		param.a=a;
		param.b=b;
		param.gamma=gamma;
		param.numBlocks=numBlocks;
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
			switch (model)
			{
			case "bp-lda": lda=new BPLDA(param); break;
			case "st-lda": lda=new STLDA(param); break;
			case "wsb-tm": lda=new WSBTM(param); break;
			case "rtm": lda=new RTM(param); break;
			case "lex-wsb-rtm": lda=new LexWSBRTM(param); break;
			case "lex-wsb-med-rtm": lda=new LexWSBMedRTM(param); break;
			case "slda": lda=new SLDA(param); break;
			case "bs-lda": lda=new BSLDA(param); break;
			case "lex-wsb-bs-lda": lda=new LexWSBBSLDA(param); break;
			case "lex-wsb-med-lda": lda=new LexWSBMedLDA(param); break;
			default: lda=new LDA(param); break;
			}
			
			lda.readCorpus(corpusFileName);
			
			if (lda instanceof BPLDA) ((BPLDA)lda).readBlocks(blockFileName);
			if (lda instanceof RTM)
			{
				((RTM)lda).readGraph(rtmTrainGraphFileName, RTM.TRAIN_GRAPH);
				((RTM)lda).readGraph(rtmTestGraphFileName, RTM.TEST_GRAPH);
			}
			if (lda instanceof SLDA) ((SLDA)lda).readLabels(labelFileName);
			if (lda instanceof WSBTM) ((WSBTM)lda).readGraph(wsbmGraphFileName);
			if (wsbmGraphFileName.length()>0)
			{
				if (lda instanceof LexWSBRTM) ((LexWSBRTM)lda).readBlockGraph(wsbmGraphFileName);
				if (lda instanceof LexWSBBSLDA) ((LexWSBBSLDA)lda).readBlockGraph(wsbmGraphFileName);
			}
			
			lda.initialize();
			lda.sample(numIters);
			lda.writeModel(modelFileName);
			
			if (thetaFileName.length()>0) lda.writeDocTopicDist(thetaFileName);
			if (topicFileName.length()>0) lda.writeResult(topicFileName, numTopWords);
			if (predFileName.length()>0)
			{
				if (lda instanceof RTM) ((RTM)lda).writePred(predFileName);
				if (lda instanceof SLDA) ((SLDA)lda).writePredLabels(predFileName);
			}
			if (outputWSBMFileName.length()>0)
			{
				if (lda instanceof WSBTM) ((WSBTM)lda).writeBlocks(outputWSBMFileName);
				if (lda instanceof LexWSBRTM) ((LexWSBRTM)lda).writeBlocks(outputWSBMFileName);
				if (lda instanceof LexWSBBSLDA) ((LexWSBBSLDA)lda).writeBlocks(outputWSBMFileName);
			}
		}
		else
		{
			switch (model)
			{
			case "bp-lda": lda=new BPLDA(modelFileName, param); break;
			case "st-lda": lda=new STLDA(modelFileName, param); break;
			case "wsb-tm": lda=new WSBTM(modelFileName, param); break;
			case "rtm": lda=new RTM(modelFileName, param); break;
			case "lex-wsb-rtm": lda=new LexWSBRTM(modelFileName, param); break;
			case "lex-wsb-med-rtm": lda=new LexWSBMedRTM(modelFileName, param); break;
			case "slda": lda=new SLDA(modelFileName, param); break;
			case "bs-lda": lda=new BSLDA(modelFileName, param); break;
			case "lex-wsb-bs-lda": lda=new LexWSBBSLDA(modelFileName, param); break;
			case "lex-wsb-med-lda": lda=new LexWSBMedLDA(modelFileName, param); break;
			default: lda=new LDA(modelFileName, param); break;
			}
			
			lda.readCorpus(corpusFileName);
			
			if (lda instanceof BPLDA && blockFileName.length()>0) ((BPLDA)lda).readBlocks(blockFileName);
			if (lda instanceof RTM)
			{
				if (rtmTrainGraphFileName.length()>0) ((RTM)lda).readGraph(rtmTrainGraphFileName, RTM.TRAIN_GRAPH);
				((RTM)lda).readGraph(rtmTestGraphFileName, RTM.TEST_GRAPH);
			}
			if (lda instanceof SLDA && labelFileName.length()>0) ((SLDA)lda).readLabels(labelFileName);
			if (wsbmGraphFileName.length()>0)
			{
				if (lda instanceof WSBTM) ((WSBTM)lda).readGraph(wsbmGraphFileName);
				if (lda instanceof LexWSBRTM) ((LexWSBRTM)lda).readBlockGraph(wsbmGraphFileName);
				if (lda instanceof LexWSBBSLDA) ((LexWSBBSLDA)lda).readBlockGraph(wsbmGraphFileName);
			}
			
			lda.initialize();
			lda.sample(numIters);
			
			if (thetaFileName.length()>0) lda.writeDocTopicDist(thetaFileName);
			if (predFileName.length()>0)
			{
				if (lda instanceof RTM) ((RTM)lda).writePred(predFileName);
				if (lda instanceof SLDA) ((SLDA)lda).writePredLabels(predFileName);
			}
			if (wsbmGraphFileName.length()>0 && outputWSBMFileName.length()>0)
			{
				if (lda instanceof WSBTM) ((WSBTM)lda).writeBlocks(outputWSBMFileName);
				if (lda instanceof LexWSBRTM) ((LexWSBRTM)lda).writeBlocks(outputWSBMFileName);
				if (lda instanceof LexWSBBSLDA) ((LexWSBBSLDA)lda).writeBlocks(outputWSBMFileName);
			}
		}
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
		
		println("BP-LDA arguments:");
		println("\t--block-graph: Pre-computed block file.");
		
		println("RTM arguments:");
		println("\t--rtm-train-graph [optional in test]: Link file for RTM to train.");
		println("\t--rtm-test-graph [optional in training]: Link file for RTM to evaluate. Can be the same with RTM train graph.");
		println("\t--nu [optional]: Variance of normal priors for weight vectors/matrices in RTM and its extensions (default: 1.0).");
		println("\t--plr-int [optional]: Interval of computing predictive link rank (default: 20).");
		println("\t--neg [optional]: Sample negative links (default: false).");
		println("\t--neg-ratio [optional]: The ratio of number of negative links to number of positive links (default 1.0).");
		println("\t--pred [optional]: Predicted document link probability matrix file.");
		
		println("SLDA arguments:");
		println("\t--label [optional in test]: Label file.");
		println("\t--sigma [optional]: Variance for the Gaussian generation of response variable in SLDA (default: 1.0).");
		println("\t--nu [optional]: Variance of normal priors for weight vectors in SLDA and its extensions (default: 1.0).");
		println("\t--pred [optional]: Predicted label file.");
		
		println("WSB- model arguments:");
		println("\t--wsbm-graph [optional except WSB-TM]: Link file for WSBM to find blocks.");
		println("\t-a [optional]: Parameter of Gamma prior for block link rates (default: 1.0).");
		println("\t-b [optional]: Parameter of Gamma prior for block link rates (default: 1.0).");
		println("\t-g [optional]: Parameter of Dirichlet prior for block distribution (default: 1.0).");
		println("\t--blocks [optional]: Number of blocks (default: 10).");
		println("\t--output-wsbm [optional]: File for WSBM-identified blocks.");
		
		println("Other arguments:");
		println("\t--alpha-prime [optional]: Parameter of Dirichlet prior of block distribution over topics (default: 1.0)."
				+ " Available in BP-LDA and WSB- models.");
		println("\t-c [optional]: Regularization parameter in hinge loss (default: 1.0). Available in models with \"Med\".");
		println("\t--directed [optional]: Set all edges directed (default: false). Available in RTM (and its extensions) and WSB- models.");
	}
	
	static
	{
		ldaNames=new HashSet<String>();
		ldaNames.add("lda");
		ldaNames.add("bp-lda");
		ldaNames.add("st-lda");
		ldaNames.add("wsb-tm");
		ldaNames.add("rtm");
		ldaNames.add("lex-wsb-rtm");
		ldaNames.add("lex-wsb-med-rtm");
		ldaNames.add("slda");
		ldaNames.add("bs-lda");
		ldaNames.add("lex-wsb-bs-lda");
		ldaNames.add("lex-wsb-med-lda");
		
		rtmNames=new HashSet<String>();
		rtmNames.add("rtm");
		rtmNames.add("lex-wsb-rtm");
		rtmNames.add("lex-wsb-med-rtm");
		
		sldaNames=new HashSet<String>();
		sldaNames.add("slda");
		sldaNames.add("bs-lda");
		sldaNames.add("lex-wsb-bs-lda");
		sldaNames.add("lex-wsb-med-lda");
	}
}
