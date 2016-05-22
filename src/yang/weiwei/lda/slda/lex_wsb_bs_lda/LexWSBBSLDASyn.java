package yang.weiwei.lda.slda.lex_wsb_bs_lda;

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
import yang.weiwei.lda.LDASynParam;
import yang.weiwei.lda.slda.bs_lda.BSLDASyn;
import yang.weiwei.util.IOUtil;

public class LexWSBBSLDASyn extends BSLDASyn
{
	protected double pi[][];
	protected WSBMSyn wsbmSyn;
	
	protected double tau[];
	
	protected void generateTau()
	{
		tau=new double[param.numVocab];
		for (int vocab=0; vocab<param.numVocab; vocab++)
		{
			tau[vocab]=randoms.nextGaussian(0.0, param.nu*param.nu);
		}
	}
	
	public void generateLabels()
	{
		generateTau();
		super.generateLabels();
	}
	
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
	
	public double computeWeight(int doc)
	{
		double weight=super.computeWeight(doc);
		for (int token : corpus.get(doc).keySet())
		{
			weight+=tau[token]*corpus.get(doc).get(token)/param.docLength;
		}
		return weight;
	}
	
	public void generateBlockGraph()
	{
		wsbmSyn.generateGraph();
	}
	
	public void writeBlockGraph(String blockGraphFileName) throws IOException
	{
		wsbmSyn.writeGraph(blockGraphFileName);
	}
	
	public void writeParams(String paramFileName, String wsbmParamFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(paramFileName));
		IOUtil.writeMatrix(bw, theta);
		IOUtil.writeMatrix(bw, phi);
		IOUtil.writeMatrix(bw, pi);
		IOUtil.writeVector(bw, eta);
		IOUtil.writeVector(bw, tau);
		bw.close();
		
		wsbmSyn.writeParam(wsbmParamFileName);
	}
	
	public final void writeSynModel(LexWSBBSLDA lda, String synModelFileName, String wsbmSynModelFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(synModelFileName));
		IOUtil.writeMatrix(bw, lda.getDocTopicDist());
		IOUtil.writeMatrix(bw, lda.getTopicVocabDist());
		IOUtil.writeMatrix(bw, lda.pi);
		IOUtil.writeVector(bw, lda.getTopicWeights());
		IOUtil.writeVector(bw, lda.tau);
		bw.close();
		
		wsbmSyn.writeSynModel(lda.wsbm, wsbmSynModelFileName);
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
		IOUtil.println("Pi KL-D: "+MathUtil.matrixKLDivergence(pi, matchPi));
	}
	
	public final void compareParams(LexWSBBSLDA lda)
	{
		wsbmSyn.compareParams(lda.wsbm);
		matchTopics(phi, lda.getTopicVocabDist());
		compareTheta(theta, lda.getDocTopicDist());
		comparePhi(phi, lda.getTopicVocabDist());
		comparePi(pi, lda.pi);
		compareEta(eta, lda.getTopicWeights());
		compareTau(tau, lda.tau);
	}
	
	protected void compareTau(double tau[], double ldaTau[])
	{
		IOUtil.println("Tau Diff: "+MathUtil.vectorAbsDiff(tau, ldaTau));
	}
	
	public LexWSBBSLDASyn readParams(String synParamFileName) throws IOException
	{
		LexWSBBSLDASyn sldaSyn=new LexWSBBSLDASyn(param);
		BufferedReader br=new BufferedReader(new FileReader(synParamFileName));
		IOUtil.readMatrix(br, sldaSyn.theta=new double[param.numDocs][param.numTopics]);
		IOUtil.readMatrix(br, sldaSyn.phi=new double[param.numTopics][param.numVocab]);
		IOUtil.readMatrix(br, sldaSyn.pi=new double[param.numBlocks][param.numTopics]);
		IOUtil.readVector(br, sldaSyn.eta=new double[param.numTopics]);
		IOUtil.readVector(br, sldaSyn.tau=new double[param.numVocab]);
		br.close();
		return sldaSyn;
	}
	
	public void compareParams(String synParamFileName, String synModelFileName,
			String wsbmSynParamFileName, String wsbmSynModelFileName) throws IOException
	{
		wsbmSyn.compareParams(wsbmSynParamFileName, wsbmSynModelFileName);
		LexWSBBSLDASyn syn1=readParams(synParamFileName);
		LexWSBBSLDASyn syn2=readParams(synModelFileName);
		matchTopics(syn1.phi, syn2.phi);
		compareTheta(syn1.theta, syn2.theta);
		comparePhi(syn1.phi, syn2.phi);
		comparePi(syn1.pi, syn2.pi);
		compareEta(syn1.eta, syn2.eta);
		compareTau(syn1.tau, syn2.tau);
	}
	
	public LexWSBBSLDASyn(LDASynParam parameters)
	{
		super(parameters);
		wsbmSyn=new WSBMSyn(new WSBMSynParam(parameters));
	}
	
	public static void main(String args[]) throws IOException
	{
		String seg[]=Thread.currentThread().getStackTrace()[1].getClassName().split("\\.");
		String modelName=seg[seg.length-1];
		
		LDASynParam param=new LDASynParam();
		LexWSBBSLDASyn sldaSyn=new LexWSBBSLDASyn(param);
		sldaSyn.generateBlockGraph();
		sldaSyn.generateCorpus();
		sldaSyn.generateLabels();
		sldaSyn.writeCorpus(LDACfg.getSynCorpusFileName(modelName));
		sldaSyn.writeLabels(LDACfg.getSynLabelFileName(modelName), 0.6, 0.4);
		sldaSyn.writeBlockGraph(LDACfg.getSynBlockGraphFileName(modelName));
		sldaSyn.writeParams(LDACfg.getSynParamFileName(modelName), LDACfg.getSynWSBMParamFileName(modelName));
		
		LexWSBBSLDA slda=new LexWSBBSLDA(new LDAParam(param));
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
