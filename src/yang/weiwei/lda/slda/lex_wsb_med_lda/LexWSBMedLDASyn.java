package yang.weiwei.lda.slda.lex_wsb_med_lda;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.LDASynParam;
import yang.weiwei.lda.slda.lex_wsb_bs_lda.LexWSBBSLDASyn;

public class LexWSBMedLDASyn extends LexWSBBSLDASyn
{
	protected int labels[];
	
	protected double computeProb(double weight, int label)
	{
		return Math.exp(-2.0*param.c*Math.max(0.0, 1.0-label*weight));
	}
	
	public void generateLabels()
	{
		double thresh=0.55;
		generateEta();
		generateTau();
		labels=new int[param.numDocs];
		for (int doc=0; doc<param.numDocs; doc++)
		{
			double weight=computeWeight(doc);
			double posProb=computeProb(weight, 1);
			double negProb=computeProb(weight, -1);
			if (posProb>thresh && posProb>negProb)
			{
				labels[doc]=1;
			}
			if (negProb>thresh && negProb>posProb)
			{
				labels[doc]=-1;
			}
		}
	}
	
	public void writeLabels(String labelFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(labelFileName));
		for (int doc=0; doc<param.numDocs; doc++)
		{
			if (labels[doc]==1)
			{
				bw.write("1");
			}
			if (labels[doc]==-1)
			{
				bw.write("-1");
			}
			bw.newLine();
		}
		bw.close();
	}
	
	public LexWSBMedLDASyn(LDASynParam parameters)
	{
		super(parameters);
	}
	
	public static void main(String args[]) throws IOException
	{
		String seg[]=Thread.currentThread().getStackTrace()[1].getClassName().split("\\.");
		String modelName=seg[seg.length-1];
		
		LDASynParam param=new LDASynParam();
		LexWSBMedLDASyn sldaSyn=new LexWSBMedLDASyn(param);
		sldaSyn.generateBlockGraph();
		sldaSyn.generateCorpus();
		sldaSyn.generateLabels();
		sldaSyn.writeCorpus(LDACfg.getSynCorpusFileName(modelName));
		sldaSyn.writeLabels(LDACfg.getSynLabelFileName(modelName));
		sldaSyn.writeBlockGraph(LDACfg.getSynBlockGraphFileName(modelName));
		sldaSyn.writeParams(LDACfg.getSynParamFileName(modelName), LDACfg.getSynWSBMParamFileName(modelName));
		
		LexWSBMedLDA slda=new LexWSBMedLDA(new LDAParam(param));
		slda.readCorpus(LDACfg.getSynCorpusFileName(modelName));
		slda.readLabels(LDACfg.getSynLabelFileName(modelName));
		slda.readBlockGraph(LDACfg.getSynBlockGraphFileName(modelName));
		slda.initialize();
		slda.sample(100);
		
		sldaSyn.writeSynModel(slda, LDACfg.getSynModelFileName(modelName), LDACfg.getSynWSBMModelFileName(modelName));
		sldaSyn.compareParams(LDACfg.getSynParamFileName(modelName), LDACfg.getSynModelFileName(modelName),
				LDACfg.getSynWSBMParamFileName(modelName), LDACfg.getSynWSBMModelFileName(modelName));
	}
}
