package yang.weiwei.tools;

import java.io.IOException;

import yang.weiwei.scc.SCC;

public class ToolSCC extends ToolInterface
{
	private int numNodes=-1;
	private String graphFileName="";
	private String outputFileName="";
	
	public void parseCommand(String[] args)
	{
		help=findArg("--help", args, false);
		numNodes=getArg("--nodes", args, -1);
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
		
		return true;
	}

	public void execute() throws IOException
	{
		if (!checkCommand())
		{
			printHelp();
			return;
		}
		
		SCC scc=new SCC(numNodes);
		scc.readGraph(graphFileName);
		scc.cluster();
		scc.writeClusters(outputFileName);
	}

	public void printHelp()
	{
		println("Arguments for SCC:");
		println("\t--help [optional]: Print help information.");
		println("\t--nodes: Number of nodes.");
		println("\t--graph: Graph file.");
		println("\t--output: Result file.");
	}
}
