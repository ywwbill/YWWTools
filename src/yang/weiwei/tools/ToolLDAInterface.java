package yang.weiwei.tools;

import java.util.HashSet;
import java.io.IOException;

import yang.weiwei.tools.lda.ToolLDA;
import yang.weiwei.tools.lda.ToolSTLDA;
import yang.weiwei.tools.lda.ToolBPLDA;
import yang.weiwei.tools.lda.ToolWSBTM;
import yang.weiwei.tools.lda.ToolRTM;
import yang.weiwei.tools.lda.ToolLexWSBRTM;
import yang.weiwei.tools.lda.ToolLexWSBMedRTM;
import yang.weiwei.tools.lda.ToolSLDA;
import yang.weiwei.tools.lda.ToolBSLDA;
import yang.weiwei.tools.lda.ToolLexWSBBSLDA;
import yang.weiwei.tools.lda.ToolLexWSBMedLDA;

public class ToolLDAInterface extends ToolInterface
{
	protected static HashSet<String> ldaNames;
	
	protected String model="";
	protected String args[];
	
	public void parseCommand(String[] args)
	{
		model=getArg("--model", args).toLowerCase();
		this.args=new String[args.length];
		for (int i=0; i<args.length; i++)
		{
			this.args[i]=args[i];
		}
	}

	protected boolean checkCommand()
	{
		if (model.length()==0)
		{
			model="lda";
		}
		
		if (!ldaNames.contains(model))
		{
			println("Model is not supported.");
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
		
		ToolLDAInterface toolLDA;
		switch (model)
		{
		case "wsb-tm": toolLDA=new ToolWSBTM(); break;
		case "bp-lda": toolLDA=new ToolBPLDA(); break;
		case "st-lda": toolLDA=new ToolSTLDA(); break;
		case "rtm": toolLDA=new ToolRTM(); break;
		case "lex-wsb-rtm": toolLDA=new ToolLexWSBRTM(); break;
		case "lex-wsb-med-rtm": toolLDA=new ToolLexWSBMedRTM(); break;
		case "slda": toolLDA=new ToolSLDA(); break;
		case "bs-lda": toolLDA=new ToolBSLDA(); break;
		case "lex-wsb-bs-lda": toolLDA=new ToolLexWSBBSLDA(); break;
		case "lex-wsb-med-lda": toolLDA=new ToolLexWSBMedLDA(); break;
		default: toolLDA=new ToolLDA(); break;
		}
		toolLDA.parseCommand(args);
		toolLDA.execute();
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
	}
}
