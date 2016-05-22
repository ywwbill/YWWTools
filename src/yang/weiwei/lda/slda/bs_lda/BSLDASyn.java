package yang.weiwei.lda.slda.bs_lda;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.LDASynParam;
import yang.weiwei.lda.slda.SLDASyn;
import yang.weiwei.util.MathUtil;

public class BSLDASyn extends SLDASyn
{
	public void generateLabels()
	{
		generateEta();
		labels=new double[param.numDocs];
		for (int doc=0; doc<param.numDocs; doc++)
		{
			labels[doc]=computeProb(computeWeight(doc));
		}
	}
	
	public void writeLabels(String labelFileName, double posThresh, double negThresh) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(labelFileName));
		for (int doc=0; doc<param.numDocs; doc++)
		{
			if (labels[doc]>posThresh)
			{
				bw.write("1");
			}
			if (labels[doc]<negThresh)
			{
				bw.write("0");
			}
			bw.newLine();
		}
		bw.close();
	}
	
	protected double computeProb(double weight)
	{
		return MathUtil.sigmoid(weight);
	}
	
	public BSLDASyn(LDASynParam parameters)
	{
		super(parameters);
	}
	
	public static void main(String args[]) throws IOException
	{
		String seg[]=Thread.currentThread().getStackTrace()[1].getClassName().split("\\.");
		String modelName=seg[seg.length-1];
		
		LDASynParam param=new LDASynParam();
		BSLDASyn sldaSyn=new BSLDASyn(param);
		sldaSyn.generateCorpus();
		sldaSyn.generateLabels();
		sldaSyn.writeCorpus(LDACfg.getSynCorpusFileName(modelName));
		sldaSyn.writeLabels(LDACfg.getSynLabelFileName(modelName), 0.6, 0.4);
		sldaSyn.writeParams(LDACfg.getSynParamFileName(modelName));
		
		BSLDA slda=new BSLDA(new LDAParam(param));
		slda.readCorpus(LDACfg.getSynCorpusFileName(modelName));
		slda.readLabels(LDACfg.getSynLabelFileName(modelName));
		slda.initialize();
		slda.sample(100);
		
		sldaSyn.writeSynModel(slda, LDACfg.getSynModelFileName(modelName));
		sldaSyn.compareParams(LDACfg.getSynParamFileName(modelName), LDACfg.getSynModelFileName(modelName));
	}
}
