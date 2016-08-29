package yang.weiwei.lda.st_lda;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import yang.weiwei.util.MathUtil;
import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.LDASyn;
import yang.weiwei.lda.LDASynParam;
import yang.weiwei.util.IOUtil;

public class STLDASyn extends LDASyn
{
	protected double theta[];
	
	protected void generateTheta()
	{
		theta=new double[param.numTopics];
		theta=MathUtil.sampleDir(param.alpha, param.numTopics);
	}
	
	public void generateCorpus()
	{
		generatePhi();
		generateTheta();
		corpus=new ArrayList<HashMap<Integer, Integer>>();
		for (int doc=0; doc<param.numDocs; doc++)
		{
			HashMap<Integer, Integer> document=new HashMap<Integer, Integer>();
			int topic=MathUtil.selectDiscrete(theta);
			for (int token=0; token<param.docLength; token++)
			{
				int word=MathUtil.selectDiscrete(phi[topic]);
				if (!document.containsKey(word))
				{
					document.put(word, 0);
				}
				document.put(word, document.get(word)+1);
			}
			corpus.add(document);
		}
	}
	
	public void compareParams(STLDA lda)
	{
		matchTopics(phi, lda.getTopicVocabDist());
		compareTheta(theta, lda.getShortDocTopicDist());
		comparePhi(phi, lda.getTopicVocabDist());
	}
	
	public STLDASyn readParams(String synParamFileName) throws IOException
	{
		STLDASyn ldaSyn=new STLDASyn(param);
		BufferedReader br=new BufferedReader(new FileReader(synParamFileName));
		IOUtil.readVector(br, ldaSyn.theta=new double[param.numTopics]);
		IOUtil.readMatrix(br, ldaSyn.phi=new double[param.numTopics][param.numVocab]);
		br.close();
		return ldaSyn;
	}
	
	public void compareParams(String synParamFileName, String synModelFileName) throws IOException
	{		
		STLDASyn syn1=readParams(synParamFileName);
		STLDASyn syn2=readParams(synModelFileName);
		matchTopics(syn1.phi, syn2.phi);
		compareTheta(syn1.theta, syn2.theta);
		comparePhi(syn1.phi, syn2.phi);
	}
	
	protected void compareTheta(double theta[], double ldaTheta[])
	{
		double matchTheta[]=new double[param.numTopics];
		for (int topic=0; topic<param.numTopics; topic++)
		{
			matchTheta[topic]=ldaTheta[topicMatch[topic]];
		}
		IOUtil.println("Theta KL-D: "+MathUtil.vectorKLDivergence(theta, matchTheta));
	}
	
	public void writeParams(String paramFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(paramFileName));
		IOUtil.writeVector(bw, theta);
		IOUtil.writeMatrix(bw, phi);
		bw.close();
	}
	
	public void writeSynModel(STLDA lda, String synModelFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(synModelFileName));
		IOUtil.writeVector(bw, lda.getShortDocTopicDist());
		IOUtil.writeMatrix(bw, lda.getTopicVocabDist());
		bw.close();
	}
	
	public STLDASyn(LDASynParam parameters)
	{
		super(parameters);
	}
	
	public static void main(String args[]) throws IOException
	{
		String seg[]=Thread.currentThread().getStackTrace()[1].getClassName().split("\\.");
		String modelName=seg[seg.length-1];
		
		LDASynParam synParam=new LDASynParam();
		STLDASyn ldaSyn=new STLDASyn(synParam);
		ldaSyn.generateCorpus();
		ldaSyn.writeCorpus(LDACfg.getSynCorpusFileName(modelName));
		ldaSyn.writeParams(LDACfg.getSynParamFileName(modelName));
		
		LDAParam ldaParam=new LDAParam(synParam);
		STLDA lda=new STLDA(ldaParam);
		lda.readCorpus(LDACfg.getSynCorpusFileName(modelName));
		lda.initialize();
		lda.sample(20);
		
		ldaSyn.writeSynModel(lda, LDACfg.getSynModelFileName(modelName));
		ldaSyn.compareParams(LDACfg.getSynParamFileName(modelName), LDACfg.getSynModelFileName(modelName));
	}
}
