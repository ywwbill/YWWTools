package yang.weiwei.tools;

import java.io.IOException;

import yang.weiwei.tlda.TreeBuilder;

public class ToolTreeBuilder extends ToolInterface
{
	private String vocabFileName="";
	private String scoreFileName="";
	private String treeFileName="";
	private int treeType=1;
	private int numChild=10;
	private double threshold=0.0;
	
	public void parseCommand(String[] args)
	{
		help=findArg("--help", args, false);
		vocabFileName=getArg("--vocab", args);
		scoreFileName=getArg("--score", args);
		treeFileName=getArg("--tree", args);
		treeType=getArg("--type", args, 1);
		numChild=getArg("--child", args, 10);
		threshold=getArg("--thresh", args, 0.0);
	}

	protected boolean checkCommand()
	{
		if (help) return false;
		
		if (vocabFileName.length()==0)
		{
			println("Vocabulary file is not given.");
			return false;
		}
		
		if (scoreFileName.length()==0)
		{
			println("Word association file is not given.");
			return false;
		}
		
		if (treeFileName.length()==0)
		{
			println("Tree prior file is not given.");
			return false;
		}
		
		if (treeType<1 || treeType>3)
		{
			println("Tree type must be 1, 2, or 3.");
			return false;
		}
		
		if (numChild<0)
		{
			println("Number of child nodes must be a positive integer.");
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
		
		TreeBuilder tBuilder=new TreeBuilder();
		switch (treeType)
		{
		case 1: tBuilder.build2LevelTree(scoreFileName, vocabFileName, treeFileName, numChild); break;
		case 2: tBuilder.hac(scoreFileName, vocabFileName, treeFileName, threshold); break;
		case 3: tBuilder.hacWithLeafDup(scoreFileName, vocabFileName, treeFileName, threshold); break;
		}
	}

	public void printHelp()
	{
		println("Arguments for tree builder tool:");
		println("\t--help [optional]: Print help information.");
		println("\t--vocab: Vocabulary file.");
		println("\t--score: Word association file.");
		println("\t--tree: Tree prior file.");
		println("\t--type [optional]: Tree prior type. 1 for two-level tree; 2 for hierarchical agglomerative clustering (HAC) tree; 3 for HAC tree with leaf duplication. (default 1)");
		println("\t--child [optional]: Number of child nodes per internal node for a two-level tree. (default 10)");
		println("\t--thresh [optional]: The confidence threshold for HAC. (default 0.0)");
	}
}
