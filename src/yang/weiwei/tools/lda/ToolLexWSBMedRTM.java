package yang.weiwei.tools.lda;

import java.io.IOException;

import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.rtm.lex_wsb_med_rtm.LexWSBMedRTM;

public class ToolLexWSBMedRTM extends ToolLexWSBRTM
{
	protected double c=1.0;
	
	public void parseCommand(String[] args)
	{
		super.parseCommand(args);
		c=getArg("-c", args, 1.0);
	}
	
	public boolean checkCommand()
	{
		if (!super.checkCommand()) return false;
		
		if (c<=0.0)
		{
			println("Parameter c must be a positive real number.");
			return false;
		}
		
		return true;
	}
	
	protected LDAParam createParam() throws IOException
	{
		LDAParam param=super.createParam();
		param.c=c;
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
		LexWSBMedRTM lda=null;
		if (!test)
		{
			lda=new LexWSBMedRTM(param);
			lda.readCorpus(corpusFileName);
			lda.readGraph(rtmTrainGraphFileName, LexWSBMedRTM.TRAIN_GRAPH);
			lda.readGraph(rtmTestGraphFileName, LexWSBMedRTM.TEST_GRAPH);
			if (wsbmGraphFileName.length()>0) lda.readBlockGraph(wsbmGraphFileName);
			lda.initialize();
			lda.sample(numIters);
			lda.writeModel(modelFileName);
			if (thetaFileName.length()>0) lda.writeDocTopicDist(thetaFileName);
			if (topicFileName.length()>0) lda.writeResult(topicFileName, numTopWords);
			if (predFileName.length()>0) lda.writePred(predFileName);
			if (outputWSBMFileName.length()>0) lda.writeBlocks(outputWSBMFileName);
		}
		else
		{
			lda=new LexWSBMedRTM(modelFileName, param);
			lda.readCorpus(corpusFileName);
			if (rtmTrainGraphFileName.length()>0) lda.readGraph(rtmTrainGraphFileName, LexWSBMedRTM.TRAIN_GRAPH);
			lda.readGraph(rtmTestGraphFileName, LexWSBMedRTM.TEST_GRAPH);
			if (wsbmGraphFileName.length()>0) lda.readBlockGraph(wsbmGraphFileName);
			lda.initialize();
			lda.sample(numIters);
			if (thetaFileName.length()>0) lda.writeDocTopicDist(thetaFileName);
			if (predFileName.length()>0) lda.writePred(predFileName);
			if (wsbmGraphFileName.length()>0 && outputWSBMFileName.length()>0) lda.writeBlocks(outputWSBMFileName);
		}
	}
	
	public void printHelp()
	{
		super.printHelp();
		println("Lex-WSB-Med-RTM arguments:");
		println("\t-c [optional]: Regularization parameter in hinge loss (default: 1.0).");
	}
}
