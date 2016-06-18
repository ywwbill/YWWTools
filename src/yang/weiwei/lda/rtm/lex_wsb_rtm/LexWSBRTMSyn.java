package yang.weiwei.lda.rtm.lex_wsb_rtm;

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
import yang.weiwei.lda.rtm.RTMSyn;
import yang.weiwei.util.IOUtil;

public class LexWSBRTMSyn extends RTMSyn
{
	protected double pi[][];
	protected double tau[];
	protected double rho[][];
	protected WSBMSyn wsbmSyn;
	
	protected void generateTau()
	{
		tau=new double[param.numVocab];
		for (int vocab=0; vocab<param.numVocab; vocab++)
		{
			tau[vocab]=randoms.nextGaussian(0.0, param.nu*param.nu);
		}
	}
	
	protected void generateRho()
	{
		int startPos;
		rho=new double[param.numBlocks][param.numBlocks];
		for (int l1=0; l1<param.numBlocks; l1++)
		{
			startPos=(param.directed? 0:l1);
			for (int l2=startPos; l2<param.numBlocks; l2++)
			{
				rho[l1][l2]=randoms.nextGaussian(0.0, param.nu*param.nu);
				if (!param.directed)
				{
					rho[l2][l1]=rho[l1][l2];
				}
			}
		}
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
	
	public void generateBlockGraph()
	{
		wsbmSyn.generateGraph();
	}
	
	public void writeBlockGraph(String blockGraphFileName) throws IOException
	{
		wsbmSyn.writeGraph(blockGraphFileName);
	}
	
	public void generateGraph()
	{
		generateTau();
		generateRho();
		super.generateGraph();
	}
	
	protected double computeWeight(int doc1, int doc2)
	{
		double weight=super.computeWeight(doc1, doc2);
		for (int token : corpus.get(doc1).keySet())
		{
			if (corpus.get(doc2).containsKey(token))
			{
				weight+=tau[token]*corpus.get(doc1).get(token)/param.docLength*corpus.get(doc2).get(token)/param.docLength;
			}
		}
		return weight;
	}
	
	public void writeParams(String paramFileName, String wsbmParamFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(paramFileName));
		IOUtil.writeMatrix(bw, theta);
		IOUtil.writeMatrix(bw, phi);
		IOUtil.writeMatrix(bw, pi);
		IOUtil.writeVector(bw, eta);
		IOUtil.writeVector(bw, tau);
		IOUtil.writeMatrix(bw, rho);
		bw.close();
		
		wsbmSyn.writeParam(wsbmParamFileName);
	}
	
	public final void writeSynModel(LexWSBRTM lda, String synModelFileName, String wsbmSynModelFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(synModelFileName));
		IOUtil.writeMatrix(bw, lda.getDocTopicDist());
		IOUtil.writeMatrix(bw, lda.getTopicVocabDist());
		IOUtil.writeMatrix(bw, lda.pi);
		IOUtil.writeVector(bw, lda.getTopicWeights());
		IOUtil.writeVector(bw, lda.tau);
		IOUtil.writeMatrix(bw, lda.rho);
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
	
	protected void compareTau(double tau[], double ldaTau[])
	{
		IOUtil.println("Tau Diff: "+MathUtil.vectorAbsDiff(tau, ldaTau));
	}
	
	protected void compareRho(double rho[][], double ldaRho[][])
	{
		double matchRho[][]=new double[param.numBlocks][param.numBlocks];
		for (int l1=0; l1<param.numBlocks; l1++)
		{
			for (int l2=0; l2<param.numBlocks; l2++)
			{
				matchRho[l1][l2]=ldaRho[wsbmSyn.getBlockMatch(l1)][wsbmSyn.getBlockMatch(l2)];
			}
		}
		IOUtil.println("Rho Diff: "+MathUtil.matrixAbsDiff(rho, matchRho));
	}
	
	public final void compareParams(LexWSBRTM lda)
	{
		wsbmSyn.compareParams(lda.wsbm);
		matchTopics(phi, lda.getTopicVocabDist());
		compareTheta(theta, lda.getDocTopicDist());
		comparePhi(phi, lda.getTopicVocabDist());
		comparePi(pi, lda.pi);
		compareEta(eta, lda.getTopicWeights());
		compareTau(tau, lda.tau);
		compareRho(rho, lda.rho);
	}
	
	public LexWSBRTMSyn readParams(String synParamFileName) throws IOException
	{
		LexWSBRTMSyn rtmSyn=new LexWSBRTMSyn(param);
		BufferedReader br=new BufferedReader(new FileReader(synParamFileName));
		IOUtil.readMatrix(br, rtmSyn.theta=new double[param.numDocs][param.numTopics]);
		IOUtil.readMatrix(br, rtmSyn.phi=new double[param.numTopics][param.numVocab]);
		IOUtil.readMatrix(br, rtmSyn.pi=new double[param.numBlocks][param.numTopics]);
		IOUtil.readVector(br, rtmSyn.eta=new double[param.numTopics]);
		IOUtil.readVector(br, rtmSyn.tau=new double[param.numVocab]);
		IOUtil.readMatrix(br, rtmSyn.rho=new double[param.numBlocks][param.numBlocks]);
		br.close();
		return rtmSyn;
	}
	
	public void compareParams(String synParamFileName, String synModelFileName,
			String wsbmSynParamFileName, String wsbmSynModelFileName) throws IOException
	{
		wsbmSyn.compareParams(wsbmSynParamFileName, wsbmSynModelFileName);
		LexWSBRTMSyn syn1=readParams(synParamFileName);
		LexWSBRTMSyn syn2=readParams(synModelFileName);
		matchTopics(syn1.phi, syn2.phi);
		compareTheta(syn1.theta, syn2.theta);
		comparePhi(syn1.phi,syn2.phi);
		comparePi(syn1.pi, syn2.pi);
		compareEta(syn1.eta, syn2.eta);
		compareTau(syn1.tau, syn2.tau);
		compareRho(syn1.rho, syn2.rho);
	}
	
	public LexWSBRTMSyn(LDASynParam parameters)
	{
		super(parameters);
		wsbmSyn=new WSBMSyn(new WSBMSynParam(parameters));
	}
	
	public static void main(String args[]) throws IOException
	{
		String seg[]=Thread.currentThread().getStackTrace()[1].getClassName().split("\\.");
		String modelName=seg[seg.length-1];
		
		LDASynParam param=new LDASynParam();
		LexWSBRTMSyn rtmSyn=new LexWSBRTMSyn(param);
		rtmSyn.generateBlockGraph();
		rtmSyn.generateCorpus();
		rtmSyn.generateGraph();
		rtmSyn.writeCorpus(LDACfg.getSynCorpusFileName(modelName));
		rtmSyn.writeBlockGraph(LDACfg.getSynBlockGraphFileName(modelName));
		rtmSyn.writeGraph(LDACfg.getSynGraphFileName(modelName), 0.8, 0.2);
		rtmSyn.writeParams(LDACfg.getSynParamFileName(modelName), LDACfg.getSynWSBMParamFileName(modelName));
		
		LexWSBRTM rtm=new LexWSBRTM(new LDAParam(param));
		rtm.readCorpus(LDACfg.getSynCorpusFileName(modelName));
		rtm.readGraph(LDACfg.getSynGraphFileName(modelName), LexWSBRTM.TRAIN_GRAPH);
		rtm.readGraph(LDACfg.getSynGraphFileName(modelName), LexWSBRTM.TEST_GRAPH);
		rtm.readBlockGraph(LDACfg.getSynBlockGraphFileName(modelName));
		rtm.initialize();
		rtm.sample(100);
		rtmSyn.writeSynModel(rtm, LDACfg.getSynModelFileName(modelName), LDACfg.getSynWSBMModelFileName(modelName));
		
		rtmSyn.compareParams(LDACfg.getSynParamFileName(modelName), LDACfg.getSynModelFileName(modelName),
				LDACfg.getSynWSBMParamFileName(modelName), LDACfg.getSynWSBMModelFileName(modelName));
	}
}
