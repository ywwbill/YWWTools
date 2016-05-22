package yang.weiwei.tools.lda;

import java.io.IOException;

import yang.weiwei.lda.st_lda.STLDA;
import yang.weiwei.lda.LDAParam;

public class ToolSTLDA extends ToolLDA
{
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
			lda.readCorpus(corpusFileName);
			lda.initialize();
			lda.sample(numIters);
			lda.writeModel(modelFileName);
			if (thetaFileName.length()>0) lda.writeDocTopicDist(thetaFileName);
			if (topicFileName.length()>0) lda.writeResult(topicFileName, numTopWords);
		}
		else
		{
			lda=new STLDA(modelFileName, param);
			lda.readCorpus(corpusFileName);
			lda.initialize();
			lda.sample(numIters);
			if (thetaFileName.length()>0) lda.writeDocTopicDist(thetaFileName);
		}
	}
}
