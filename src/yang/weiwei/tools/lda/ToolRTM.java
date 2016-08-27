package yang.weiwei.tools.lda;

import java.io.IOException;

import yang.weiwei.lda.rtm.RTM;
import yang.weiwei.lda.LDAParam;

public class ToolRTM extends ToolLDA
{
	protected double nu=1.0;
	protected boolean directed=false;
	protected int PLRInterval=20;
	protected boolean negEdge=false;
	protected double negEdgeRatio=1.0;
	
	protected String rtmTrainGraphFileName="";
	protected String rtmTestGraphFileName="";
	protected String predFileName="";
	protected String regFileName="";
	
	public void parseCommand(String[] args)
	{
		super.parseCommand(args);
		
		nu=getArg("--nu", args, 1.0);
		directed=findArg("--directed", args, false);
		PLRInterval=getArg("--plr-int", args, 20);
		negEdge=findArg("--neg", args, false);
		negEdgeRatio=getArg("--neg-ratio", args, 1.0);
		
		rtmTrainGraphFileName=getArg("--rtm-train-graph", args);
		rtmTestGraphFileName=getArg("--rtm-test-graph", args);
		predFileName=getArg("--pred", args);
		regFileName=getArg("--reg", args);
	}
	
	protected boolean checkCommand()
	{
		if (!super.checkCommand()) return false;
		
		if (rtmTrainGraphFileName.length()==0 && !test)
		{
			println("RTM train graph file is not specified.");
			return false;
		}
		
		if (!test && rtmTestGraphFileName.length()==0)
		{
			rtmTestGraphFileName=rtmTrainGraphFileName;
		}
		
		if (rtmTestGraphFileName.length()==0 && test)
		{
			println("RTM test graph is not specified.");
			return false;
		}
		
		if (nu<=0.0)
		{
			println("Parameter nu must be a positive real number.");
			return false;
		}
		
		if (PLRInterval<=0)
		{
			println("Interval of computing PLR must be a positive integer.");
			return false;
		}
		
		if (negEdge && negEdgeRatio<0.0)
		{
			println("Negative edge ratio must be a non-negative real number.");
			return false;
		}
		
		return true;
	}
	
	protected LDAParam createParam() throws IOException
	{
		LDAParam param=super.createParam();
		param.nu=nu;
		param.directed=directed;
		param.showPLRInterval=PLRInterval;
		param.negEdge=negEdge;
		param.negEdgeRatio=negEdgeRatio;
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
		RTM lda=null;
		if (!test)
		{
			lda=new RTM(param);
			lda.readCorpus(corpusFileName);
			lda.readGraph(rtmTrainGraphFileName, RTM.TRAIN_GRAPH);
			lda.readGraph(rtmTestGraphFileName, RTM.TEST_GRAPH);
			lda.initialize();
			lda.sample(numIters);
			lda.writeModel(modelFileName);
		}
		else
		{
			lda=new RTM(modelFileName, param);
			lda.readCorpus(corpusFileName);
			if (rtmTrainGraphFileName.length()>0) lda.readGraph(rtmTrainGraphFileName, RTM.TRAIN_GRAPH);
			lda.readGraph(rtmTestGraphFileName, RTM.TEST_GRAPH);
			lda.initialize();
			lda.sample(numIters);
		}
		writeFiles(lda);
	}
	
	protected void writeFiles(RTM lda) throws IOException
	{
		super.writeFiles(lda);
		if (predFileName.length()>0) lda.writePred(predFileName);
		if (regFileName.length()>0) lda.writeRegValues(regFileName);
	}
	
	public void printHelp()
	{
		super.printHelp();
		println("RTM arguments:");
		println("\t--rtm-train-graph [optional in test]: Link file for RTM to train.");
		println("\t--rtm-test-graph [optional in training]: Link file for RTM to evaluate. Can be the same with RTM train graph.");
		println("\t--nu [optional]: Variance of normal priors for weight vectors/matrices in RTM and its extensions (default: 1.0).");
		println("\t--plr-int [optional]: Interval of computing predictive link rank (default: 20).");
		println("\t--neg [optional]: Sample negative links (default: false).");
		println("\t--neg-ratio [optional]: The ratio of number of negative links to number of positive links (default 1.0).");
		println("\t--pred [optional]: Predicted document link probability matrix file.");
		println("\t--directed [optional]: Set all edges directed (default: false).");
	}
}
