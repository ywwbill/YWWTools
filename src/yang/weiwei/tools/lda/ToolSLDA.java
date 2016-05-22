package yang.weiwei.tools.lda;

import java.io.IOException;

import yang.weiwei.lda.slda.SLDA;
import yang.weiwei.lda.LDAParam;

public class ToolSLDA extends ToolLDA
{
	protected double sigma=1.0;
	protected double nu=1.0;
	
	protected String labelFileName="";
	protected String predFileName="";
	
	public void parseCommand(String[] args)
	{
		super.parseCommand(args);
		
		sigma=getArg("--sigma", args, 1.0);
		nu=getArg("--nu", args, 1.0);
		
		labelFileName=getArg("--label", args);
		predFileName=getArg("--pred", args);
	}
	
	protected boolean checkCommand()
	{
		if (!super.checkCommand()) return false;
		
		if (labelFileName.length()==0 && !test)
		{
			println("Label file is not specified.");
			return false;
		}
		
		if (nu<=0.0)
		{
			println("Parameter nu must be a positive real number.");
			return false;
		}
		
		if (sigma<=0.0)
		{
			println("Parameter sigma must be a positive real number.");
			return false;
		}
		
		return true;
	}
	
	protected LDAParam createParam() throws IOException
	{
		LDAParam param=super.createParam();
		param.sigma=sigma;
		param.nu=nu;
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
		SLDA lda=null;
		if (!test)
		{
			lda=new SLDA(param);
			lda.readCorpus(corpusFileName);
			lda.readLabels(labelFileName);
			lda.initialize();
			lda.sample(numIters);
			lda.writeModel(modelFileName);
			if (thetaFileName.length()>0) lda.writeDocTopicDist(thetaFileName);
			if (topicFileName.length()>0) lda.writeResult(topicFileName, numTopWords);
			if (predFileName.length()>0) lda.writePredLabels(predFileName);
		}
		else
		{
			lda=new SLDA(modelFileName, param);
			lda.readCorpus(corpusFileName);
			if (labelFileName.length()>0) lda.readLabels(labelFileName);
			lda.initialize();
			lda.sample(numIters);
			if (thetaFileName.length()>0) lda.writeDocTopicDist(thetaFileName);
			if (predFileName.length()>0) lda.writePredLabels(predFileName);
		}
	}
	
	public void printHelp()
	{
		super.printHelp();
		println("SLDA arguments:");
		println("\t--label [optional in test]: Label file.");
		println("\t--sigma [optional]: Variance for the Gaussian generation of response variable in SLDA (default: 1.0).");
		println("\t--nu [optional]: Variance of normal priors for weight vectors in SLDA and its extensions (default: 1.0).");
		println("\t--pred [optional]: Predicted label file.");
	}
}
