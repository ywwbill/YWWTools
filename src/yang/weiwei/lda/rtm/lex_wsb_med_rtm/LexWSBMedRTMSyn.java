package yang.weiwei.lda.rtm.lex_wsb_med_rtm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.LDASynParam;
import yang.weiwei.lda.rtm.lex_wsb_rtm.LexWSBRTMSyn;

public class LexWSBMedRTMSyn extends LexWSBRTMSyn
{
	protected double computeProb(double weight)
	{
		return Math.exp(-2.0*param.c*Math.max(0.0, 1.0-weight));
	}
	
	public void writeGraph(String synGraphFileName, double posThresh, double negThresh) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(synGraphFileName));
		for (int d1=0; d1<param.numDocs; d1++)
		{
			for (int d2=d1+1; d2<param.numDocs; d2++)
			{
				if (graph[d1][d2]>posThresh)
				{
					bw.write(d1+"\t"+d2+"\t1");
					bw.newLine();
				}
				if (graph[d1][d2]<negThresh)
				{
					bw.write(d1+"\t"+d2+"\t-1");
					bw.newLine();
				}
			}
		}
		bw.close();
	}
	
	public LexWSBMedRTMSyn(LDASynParam parameters)
	{
		super(parameters);
	}
	
	public static void main(String args[]) throws IOException
	{
		String seg[]=Thread.currentThread().getStackTrace()[1].getClassName().split("\\.");
		String modelName=seg[seg.length-1];
		
		LDASynParam param=new LDASynParam();
		LexWSBMedRTMSyn rtmSyn=new LexWSBMedRTMSyn(param);
		rtmSyn.generateBlockGraph();
		rtmSyn.generateCorpus();
		rtmSyn.generateGraph();
		rtmSyn.writeCorpus(LDACfg.getSynCorpusFileName(modelName));
		rtmSyn.writeBlockGraph(LDACfg.getSynBlockGraphFileName(modelName));
		rtmSyn.writeGraph(LDACfg.getSynGraphFileName(modelName), 0.9, 0.01);
		rtmSyn.writeParams(LDACfg.getSynParamFileName(modelName), LDACfg.getSynWSBMParamFileName(modelName));
		
		LexWSBMedRTM rtm=new LexWSBMedRTM(new LDAParam(param));
		rtm.readCorpus(LDACfg.getSynCorpusFileName(modelName));
		rtm.readGraph(LDACfg.getSynGraphFileName(modelName), LexWSBMedRTM.TRAIN_GRAPH);
		rtm.readGraph(LDACfg.getSynGraphFileName(modelName), LexWSBMedRTM.TEST_GRAPH);
		rtm.readBlockGraph(LDACfg.getSynBlockGraphFileName(modelName));
		rtm.initialize();
		rtm.sample(100);
		rtmSyn.writeSynModel(rtm, LDACfg.getSynModelFileName(modelName), LDACfg.getSynWSBMModelFileName(modelName));
		
		rtmSyn.compareParams(LDACfg.getSynParamFileName(modelName), LDACfg.getSynModelFileName(modelName),
				LDACfg.getSynWSBMParamFileName(modelName), LDACfg.getSynWSBMModelFileName(modelName));
	}
}
