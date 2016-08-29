package yang.weiwei.tools.lda;

import java.io.IOException;

import yang.weiwei.lda.st_lda.STLDA;
import yang.weiwei.lda.LDAParam;

public class ToolSTLDA extends ToolLDA
{
	protected String shortCorpusFileName="";
	
	protected String shortThetaFileName="";
	protected String shortTopicAssignFileName="";
	
	public void parseCommand(String[] args)
	{
		super.parseCommand(args);
		shortCorpusFileName=getArg("--short-corpus", args);
		shortThetaFileName=getArg("--short-theta", args);
		shortTopicAssignFileName=getArg("--short-topic-assign", args);
	}
	
	protected boolean checkCommand()
	{
		boolean emptyCorpusFileName=false;
		if (corpusFileName.length()==0)
		{
			corpusFileName="test";
			emptyCorpusFileName=true;
		}
		if (!super.checkCommand()) return false;
		if (emptyCorpusFileName)
		{
			corpusFileName="";
		}
		
		if (corpusFileName.length()==0 && shortCorpusFileName.length()==0)
		{
			println("Corpus file name is not specified.");
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
		
		LDAParam param=createParam();
		STLDA lda=null;
		if (!test)
		{
			lda=new STLDA(param);
			if (corpusFileName.length()>0) lda.readCorpus(corpusFileName);
			if (shortCorpusFileName.length()>0) lda.readShortCorpus(shortCorpusFileName);
			lda.initialize();
			lda.sample(numIters);
			lda.writeModel(modelFileName);
		}
		else
		{
			lda=new STLDA(modelFileName, param);
			if (corpusFileName.length()>0) lda.readCorpus(corpusFileName);
			if (shortCorpusFileName.length()>0) lda.readShortCorpus(shortCorpusFileName);
			lda.initialize();
			lda.sample(numIters);
		}
		writeFiles(lda);
	}
	
	protected void writeFiles(STLDA lda) throws IOException
	{
		super.writeFiles(lda);
		if (shortThetaFileName.length()>0) lda.writeShortDocTopicDist(shortThetaFileName);
		if (shortTopicAssignFileName.length()>0) lda.writeShortDocTopicAssign(shortTopicAssignFileName);
	}
	
	public void printHelp()
	{
		super.printHelp();
		println("BP-LDA arguments:");
		println("\t--short-theta [optional]: Short documents' background topic distribution file.");
		println("\t--short-topic-assign [optional]: Short documents' topic assignment file.");
	}
}
