package yang.weiwei.tools.lda;

import java.io.IOException;

import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.slda.bs_lda.BSLDA;

public class ToolBSLDA extends ToolSLDA
{
	public void execute() throws IOException
	{
		if (!checkCommand())
		{
			printHelp();
			return;
		}
		
		LDAParam param=createParam();
		BSLDA lda=null;
		if (!test)
		{
			lda=new BSLDA(param);
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
			lda=new BSLDA(modelFileName, param);
			lda.readCorpus(corpusFileName);
			if (labelFileName.length()>0) lda.readLabels(labelFileName);
			lda.initialize();
			lda.sample(numIters);
			if (thetaFileName.length()>0) lda.writeDocTopicDist(thetaFileName);
			if (predFileName.length()>0) lda.writePredLabels(predFileName);
		}
	}
}
