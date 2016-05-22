package yang.weiwei.lda.wsb_tm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import yang.weiwei.util.MathUtil;
import yang.weiwei.wsbm.WSBMSyn;
import yang.weiwei.wsbm.WSBMSynParam;
import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.LDASyn;
import yang.weiwei.lda.LDASynParam;
import yang.weiwei.util.IOUtil;

public class WSBTMSyn extends LDASyn
{
	protected double pi[][];
	protected WSBMSyn wsbmSyn;
	
	protected void generatePi()
	{
		pi=new double[param.numBlocks][param.numTopics];
		for (int l=0; l<param.numBlocks; l++)
		{
			pi[l]=MathUtil.sampleDir(param._alpha, param.numTopics);
		}
	}
	
	protected void generateTheta()
	{
		generatePi();
		theta=new double[param.numDocs][param.numTopics];
		double alphaVector[]=new double[param.numTopics];
		for (int doc=0; doc<param.numDocs; doc++)
		{
			for (int topic=0; topic<param.numTopics; topic++)
			{
				alphaVector[topic]=param.alpha*param.numTopics*pi[wsbmSyn.getBlockAssign(doc)][topic];
			}
			theta[doc]=MathUtil.sampleDir(alphaVector);
		}
	}
	
	public void generateGraph()
	{
		wsbmSyn.generateGraph();
	}
	
	public void writeGraph(String graphFileName) throws IOException
	{
		wsbmSyn.writeGraph(graphFileName);
	}
	
	public final void compareParams(WSBTM lda)
	{
		super.compareParams(lda);
		wsbmSyn.compareParams(lda.wsbm);
		comparePi(pi, lda.pi);
	}
	
	public WSBTMSyn readParams(String synParamFileName) throws IOException
	{
		WSBTMSyn wsbtmSyn=new WSBTMSyn(param);
		BufferedReader br=new BufferedReader(new FileReader(synParamFileName));
		IOUtil.readMatrix(br, wsbtmSyn.theta=new double[param.numDocs][param.numTopics]);
		IOUtil.readMatrix(br, wsbtmSyn.phi=new double[param.numTopics][param.numVocab]);
		IOUtil.readMatrix(br, wsbtmSyn.pi=new double[param.numBlocks][param.numTopics]);
		br.close();
		return wsbtmSyn;
	}
	
	public void compareParams(String synParamFileName, String synModelFileName,
			String wsbmSynParamFileName, String wsbmSynModelFileName) throws IOException
	{
		wsbmSyn.compareParams(wsbmSynParamFileName, wsbmSynModelFileName);
		WSBTMSyn syn1=readParams(synParamFileName);
		WSBTMSyn syn2=readParams(synModelFileName);
		matchTopics(syn1.phi, syn2.phi);
		compareTheta(syn1.theta, syn2.theta);
		comparePhi(syn1.phi, syn2.phi);
		comparePi(syn1.pi, syn2.pi);
	}
	
	protected void comparePi(double pi[][], double ldaPi[][])
	{
		double matchPi[][]=new double[param.numBlocks][param.numTopics];
		for (int l=0; l<param.numBlocks; l++)
		{
			for (int topic=0; topic<param.numTopics; topic++)
			{
				matchPi[l][topic]=ldaPi[wsbmSyn.getBlockMatch(l)][topicMatch[topic]];
			}
		}
		IOUtil.println("Pi KL-D: "+MathUtil.matrixKLDivergence(pi, ldaPi));
	}
	
	public void writeParams(String paramFileName, String wsbmParamFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(paramFileName));
		IOUtil.writeMatrix(bw, theta);
		IOUtil.writeMatrix(bw, phi);
		IOUtil.writeMatrix(bw, pi);
		bw.close();
		
		wsbmSyn.writeParam(wsbmParamFileName);
	}
	
	public final void writeSynModel(WSBTM lda, String synModelFileName, String wsbmSynModelFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(synModelFileName));
		IOUtil.writeMatrix(bw, lda.getDocTopicDist());
		IOUtil.writeMatrix(bw, lda.getTopicVocabDist());
		IOUtil.writeMatrix(bw, lda.pi);
		bw.close();
		
		wsbmSyn.writeSynModel(lda.wsbm, wsbmSynModelFileName);
	}
	
	public WSBTMSyn(LDASynParam parameters)
	{
		super(parameters);
		wsbmSyn=new WSBMSyn(new WSBMSynParam(parameters));
	}
	
	public static void main(String args[]) throws IOException
	{
		String seg[]=Thread.currentThread().getStackTrace()[1].getClassName().split("\\.");
		String modelName=seg[seg.length-1];
		
		LDASynParam synParam=new LDASynParam();
		WSBTMSyn ldaSyn=new WSBTMSyn(synParam);
		ldaSyn.generateGraph();
		ldaSyn.generateCorpus();
		ldaSyn.writeCorpus(LDACfg.getSynCorpusFileName(modelName));
		ldaSyn.writeGraph(LDACfg.getSynGraphFileName(modelName));
		ldaSyn.writeParams(LDACfg.getSynParamFileName(modelName), LDACfg.getSynWSBMParamFileName(modelName));
		
		WSBTM lda=new WSBTM(new LDAParam(synParam));
		lda.readCorpus(LDACfg.getSynCorpusFileName(modelName));
		lda.readGraph(LDACfg.getSynGraphFileName(modelName));
		lda.initialize();
		lda.sample(100);
		ldaSyn.writeSynModel(lda, LDACfg.getSynModelFileName(modelName), LDACfg.getSynWSBMModelFileName(modelName));
		
		ldaSyn.compareParams(LDACfg.getSynParamFileName(modelName), LDACfg.getSynModelFileName(modelName),
				LDACfg.getSynWSBMParamFileName(modelName), LDACfg.getSynWSBMModelFileName(modelName));
	}
}
