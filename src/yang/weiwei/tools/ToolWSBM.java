package yang.weiwei.tools;

import java.io.IOException;

import yang.weiwei.wsbm.WSBM;
import yang.weiwei.wsbm.WSBMParam;

public class ToolWSBM extends ToolInterface
{
	private boolean directed=false;
	private boolean verbose=true;
	
	private int numNodes=-1;
	private int numBlocks=-1;
	private int numIters=100;
	private double a=1.0;
	private double b=1.0;
	private double gamma=1.0;
	
	private String graphFileName="";
	private String outputFileName="";
	
	public void parseCommand(String[] args)
	{
		help=findArg("--help", args, false);
		directed=findArg("--directed", args, false);
		verbose=findArg("--no-verbose", args, true);
		
		numNodes=getArg("--nodes", args, -1);
		numBlocks=getArg("--blocks", args, -1);
		numIters=getArg("--iters", args, 100);
		a=getArg("-a", args, 1.0);
		b=getArg("-b", args, 1.0);
		gamma=getArg("-g", args, 1.0);
		
		graphFileName=getArg("--graph", args);
		outputFileName=getArg("--output", args);
	}

	protected boolean checkCommand()
	{
		if (help) return false;
		
		if (numNodes<=0)
		{
			println("Number of nodes is non-positive or not specified.");
			return false;
		}
		
		if (numBlocks<=0)
		{
			println("Number of blocks is non-positive or not specified.");
			return false;
		}
		
		if (graphFileName.length()==0)
		{
			println("Graph file is not specified.");
			return false;
		}
		
		if (outputFileName.length()==0)
		{
			println("Output file is not specified.");
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
		
		if (numIters<=0)
		{
			println("Numer of iterations must be a positive integer.");
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
		
		WSBMParam param=new WSBMParam();
		param.directed=directed;
		param.numNodes=numNodes;
		param.numBlocks=numBlocks;
		param.a=a;
		param.b=b;
		param.gamma=gamma;
		param.verbose=verbose;
		WSBM wsbm=new WSBM(param);
		wsbm.readGraph(graphFileName);
		wsbm.init();
		wsbm.sample(numIters);
		wsbm.writeBlockAssign(outputFileName);
	}

	public void printHelp()
	{
		println("Arguments for WSBM:");
		println("\t--help [optional]: Print help information.");
		println("\t--nodes: Number of nodes.");
		println("\t--blocks: Number of blocks.");
		println("\t--graph: Graph file.");
		println("\t--output: Output file.");
		println("\t--directed [optional]: Set edges directed (default: undirected).");
		println("\t-a [optional]: Parameter for edge rates' Gamma prior (default: 1.0).");
		println("\t-b [optional]: Parameter for edge rates' Gamma prior (default: 1.0).");
		println("\t-g [optional]: Parameter for block distribution's Dirichlet prior (default 1.0).");
		println("\t--iters [optional]: Number of iterations (default: 100).");
		println("\t--no-verbose [optional]: Stop printing log to console.");
	}
}
