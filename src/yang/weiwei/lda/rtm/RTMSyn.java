package yang.weiwei.lda.rtm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import yang.weiwei.util.MathUtil;
import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.LDASyn;
import yang.weiwei.lda.LDASynParam;
import yang.weiwei.util.IOUtil;

public class RTMSyn extends LDASyn
{
	protected double eta[];
	protected double graph[][];
	
	protected void generateEta()
	{
		eta=new double[param.numTopics];
		for (int topic=0; topic<param.numTopics; topic++)
		{
			eta[topic]=randoms.nextGaussian(0.0, param.nu*param.nu);
		}
	}
	
	public void generateGraph()
	{
		generateEta();
		graph=new double[param.numDocs][param.numDocs];
		for (int d1=0; d1<param.numDocs; d1++)
		{
			for (int d2=d1+1; d2<param.numDocs; d2++)
			{
				graph[d1][d2]=computeProb(computeWeight(d1, d2));
			}
		}
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
					bw.write(d1+"\t"+d2+"\t0");
					bw.newLine();
				}
			}
		}
		bw.close();
	}
	
	public final void compareParams(RTM lda)
	{
		super.compareParams(lda);
		compareEta(eta, lda.eta);
	}
	
	public RTMSyn readParams(String synParamFileName) throws IOException
	{
		RTMSyn rtmSyn=new RTMSyn(param);
		BufferedReader br=new BufferedReader(new FileReader(synParamFileName));
		IOUtil.readMatrix(br, rtmSyn.theta=new double[param.numDocs][param.numTopics]);
		IOUtil.readMatrix(br, rtmSyn.phi=new double[param.numTopics][param.numVocab]);
		IOUtil.readVector(br, rtmSyn.eta=new double[param.numTopics]);
		br.close();
		return rtmSyn;
	}
	
	public void compareParams(String synParamFileName, String synModelFileName) throws IOException
	{
		RTMSyn syn1=readParams(synParamFileName);
		RTMSyn syn2=readParams(synModelFileName);
		matchTopics(syn1.phi, syn2.phi);
		compareTheta(syn1.theta, syn2.theta);
		comparePhi(syn1.phi, syn2.phi);
		compareEta(syn1.eta, syn2.eta);
	}
	
	protected void compareEta(double eta[], double ldaEta[])
	{
		double matchEta[]=new double[param.numTopics];
		for (int topic=0; topic<param.numTopics; topic++)
		{
			matchEta[topic]=ldaEta[topicMatch[topic]];
		}
		IOUtil.println("Eta Diff: "+MathUtil.vectorAbsDiff(eta, matchEta));
	}
	
	protected double computeWeight(int doc1, int doc2)
	{
		double weight=0.0;
		for (int topic=0; topic<param.numTopics; topic++)
		{
			weight+=eta[topic]*docTopicCounts[doc1][topic]/param.docLength*docTopicCounts[doc2][topic]/param.docLength;
		}
		return weight;
	}
	
	protected double computeProb(double weight)
	{
		return MathUtil.sigmoid(weight);
	}
	
	public void writeParams(String paramFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(paramFileName));
		IOUtil.writeMatrix(bw, theta);
		IOUtil.writeMatrix(bw, phi);
		IOUtil.writeVector(bw, eta);
		bw.close();
	}
	
	public final void writeSynModel(RTM lda, String synModelFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(synModelFileName));
		IOUtil.writeMatrix(bw, lda.getDocTopicDist());
		IOUtil.writeMatrix(bw, lda.getTopicVocabDist());
		IOUtil.writeVector(bw, lda.eta);
		bw.close();
	}
	
	public RTMSyn(LDASynParam parameters)
	{
		super(parameters);
	}
	
	public static void main(String args[]) throws IOException
	{
		String seg[]=Thread.currentThread().getStackTrace()[1].getClassName().split("\\.");
		String modelName=seg[seg.length-1];
		
		LDASynParam param=new LDASynParam();
		RTMSyn rtmSyn=new RTMSyn(param);
		rtmSyn.generateCorpus();
		rtmSyn.generateGraph();
		rtmSyn.writeCorpus(LDACfg.getSynCorpusFileName(modelName));
		rtmSyn.writeGraph(LDACfg.getSynGraphFileName(modelName), 0.6, 0.4);
		rtmSyn.writeParams(LDACfg.getSynParamFileName(modelName));
		
		RTM rtm=new RTM(new LDAParam(param));
		rtm.readCorpus(LDACfg.getSynCorpusFileName(modelName));
		rtm.readGraph(LDACfg.getSynGraphFileName(modelName), RTM.TRAIN_GRAPH);
		rtm.readGraph(LDACfg.getSynGraphFileName(modelName), RTM.TEST_GRAPH);
		rtm.initialize();
		rtm.sample(50);
		
		rtmSyn.writeSynModel(rtm, LDACfg.getSynModelFileName(modelName));
		rtmSyn.compareParams(LDACfg.getSynParamFileName(modelName), LDACfg.getSynModelFileName(modelName));
	}
}
