package yang.weiwei.lda;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import cc.mallet.util.Randoms;
import yang.weiwei.util.MathUtil;
import yang.weiwei.util.IOUtil;

public class LDASyn
{
	protected final LDASynParam param;
	protected static Randoms randoms;
	
	protected double theta[][];
	protected double phi[][];
	protected ArrayList<HashMap<Integer, Integer>> corpus;
	protected int docTopicCounts[][];
	
	protected int topicMatch[];
	
	protected void generateTheta()
	{
		theta=new double[param.numDocs][param.numTopics];
		for (int doc=0; doc<param.numDocs; doc++)
		{
			theta[doc]=MathUtil.sampleDir(param.alpha, param.numTopics);
		}
	}
	
	protected void generatePhi()
	{
		phi=new double[param.numTopics][param.numVocab];
		for (int topic=0; topic<param.numTopics; topic++)
		{
			phi[topic]=MathUtil.sampleDir(param.beta, param.numVocab);
		}
	}
	
	public void generateCorpus()
	{
		generatePhi();
		generateTheta();
		docTopicCounts=new int[param.numDocs][param.numTopics];
		corpus=new ArrayList<HashMap<Integer, Integer>>();
		for (int doc=0; doc<param.numDocs; doc++)
		{
			HashMap<Integer, Integer> document=new HashMap<Integer, Integer>();
			for (int token=0; token<param.docLength; token++)
			{
				int topic=MathUtil.selectDiscrete(theta[doc]);
				int word=MathUtil.selectDiscrete(phi[topic]);
				if (!document.containsKey(word))
				{
					document.put(word, 0);
				}
				document.put(word, document.get(word)+1);
				docTopicCounts[doc][topic]++;
			}
			corpus.add(document);
		}
	}
	
	public final void compareParams(LDA lda)
	{
		matchTopics(phi, lda.phi);
		compareTheta(theta, lda.theta);
		comparePhi(phi, lda.phi);
	}
	
	public LDASyn readParams(String synParamFileName) throws IOException
	{
		LDASyn ldaSyn=new LDASyn(param);
		BufferedReader br=new BufferedReader(new FileReader(synParamFileName));
		IOUtil.readMatrix(br, ldaSyn.theta=new double[param.numDocs][param.numTopics]);
		IOUtil.readMatrix(br, ldaSyn.phi=new double[param.numTopics][param.numVocab]);
		br.close();
		return ldaSyn;
	}
	
	public void compareParams(String synParamFileName, String synModelFileName) throws IOException
	{
		LDASyn syn1=readParams(synParamFileName);
		LDASyn syn2=readParams(synModelFileName);
		matchTopics(syn1.phi, syn2.phi);
		compareTheta(syn1.theta, syn2.theta);
		comparePhi(syn1.phi, syn2.phi);
	}
	
	protected void compareTheta(double theta[][], double ldaTheta[][])
	{
		double matchTheta[][]=new double[param.numDocs][param.numTopics];
		for (int doc=0; doc<param.numDocs; doc++)
		{
			for (int topic=0; topic<param.numTopics; topic++)
			{
				matchTheta[doc][topic]=ldaTheta[doc][topicMatch[topic]];
			}
		}
		IOUtil.println("Theta KL-D: "+MathUtil.matrixKLDivergence(theta, matchTheta));
	}
	
	protected void comparePhi(double phi[][], double ldaPhi[][])
	{
		double matchPhi[][]=new double[param.numTopics][param.numVocab];
		for (int topic=0; topic<param.numTopics; topic++)
		{
			for (int vocab=0; vocab<param.numVocab; vocab++)
			{
				matchPhi[topic][vocab]=ldaPhi[topicMatch[topic]][vocab];
			}
		}
		IOUtil.println("Phi KL-D: "+MathUtil.matrixKLDivergence(phi, matchPhi));
	}
	
	protected void matchTopics(double phi[][], double ldaPhi[][])
	{
		boolean used[]=new boolean[param.numTopics];
		for (int topic=0; topic<param.numTopics; topic++)
		{
			topicMatch[topic]=topic;
			used[topic]=false;
		}
		
		for (int t1=0; t1<param.numTopics; t1++)
		{
			double minDis=Double.MAX_VALUE;
			int minTopic=-1;
			for (int t2=0; t2<param.numTopics; t2++)
			{
				double dis=MathUtil.vectorAbsDiff(phi[t1], ldaPhi[t2]);
				if (dis<minDis)
				{
					minDis=dis;
					minTopic=t2;
				}
			}
			assert(!used[minTopic]);
			topicMatch[t1]=minTopic;
			used[minTopic]=true;
		}
	}
	
	public void writeCorpus(String corpusFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(corpusFileName));
		for (HashMap<Integer, Integer> document : corpus)
		{
			bw.write(param.docLength+"");
			for (int v=0; v<param.numVocab; v++)
			{
				if (document.containsKey(v))
				{
					bw.write(" "+v+":"+document.get(v));
				}
			}
			bw.newLine();
		}
		bw.close();
	}
	
	public void writeParams(String paramFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(paramFileName));
		IOUtil.writeMatrix(bw, theta);
		IOUtil.writeMatrix(bw, phi);
		bw.close();
	}
	
	public final void writeSynModel(LDA lda, String synModelFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(synModelFileName));
		IOUtil.writeMatrix(bw, lda.theta);
		IOUtil.writeMatrix(bw, lda.phi);
		bw.close();
	}
	
	static
	{
		randoms=new Randoms();
	}
	
	public LDASyn(LDASynParam parameters)
	{
		param=parameters;
		topicMatch=new int[param.numTopics];
		for (int topic=0; topic<param.numTopics; topic++)
		{
			topicMatch[topic]=topic;
		}
	}
	
	public static void main(String args[]) throws IOException
	{
		String seg[]=Thread.currentThread().getStackTrace()[1].getClassName().split("\\.");
		String modelName=seg[seg.length-1];
		
		LDASynParam synParam=new LDASynParam();
		LDASyn ldaSyn=new LDASyn(synParam);
		ldaSyn.generateCorpus();
		ldaSyn.writeCorpus(LDACfg.getSynCorpusFileName(modelName));
		ldaSyn.writeParams(LDACfg.getSynParamFileName(modelName));
		
		LDAParam ldaParam=new LDAParam(synParam);
		LDA lda=new LDA(ldaParam);
		lda.readCorpus(LDACfg.getSynCorpusFileName(modelName));
		lda.initialize();
		lda.sample(100);
		
		ldaSyn.writeSynModel(lda, LDACfg.getSynModelFileName(modelName));
		ldaSyn.compareParams(LDACfg.getSynParamFileName(modelName), LDACfg.getSynModelFileName(modelName));
	}
}
