package yang.weiwei.lda.slda;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import yang.weiwei.util.MathUtil;
import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.LDASyn;
import yang.weiwei.lda.LDASynParam;
import yang.weiwei.util.IOUtil;

public class SLDASyn extends LDASyn
{
	protected double eta[];
	protected double labels[];
	
	protected void generateEta()
	{
		eta=new double[param.numTopics];
		for (int topic=0; topic<param.numTopics; topic++)
		{
			eta[topic]=randoms.nextGaussian(0.0, param.nu*param.nu);
		}
	}
	
	public void generateLabels()
	{
		generateEta();
		labels=new double[param.numDocs];
		for (int doc=0; doc<param.numDocs; doc++)
		{
			labels[doc]=computeWeight(doc);
		}
	}
	
	public void writeLabels(String labelFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(labelFileName));
		for (int doc=0; doc<param.numDocs; doc++)
		{
			bw.write(labels[doc]+"");
			bw.newLine();
		}
		bw.close();
	}
	
	protected double computeWeight(int doc)
	{
		double weight=0.0;
		for (int topic=0; topic<param.numTopics; topic++)
		{
			weight+=eta[topic]*docTopicCounts[doc][topic]/param.docLength;
		}
		return weight;
	}
	
	public final void compareParams(SLDA lda)
	{
		super.compareParams(lda);
		compareEta(eta, lda.eta);
	}
	
	public SLDASyn readParams(String synParamFileName) throws IOException
	{
		SLDASyn sldaSyn=new SLDASyn(param);
		BufferedReader br=new BufferedReader(new FileReader(synParamFileName));
		IOUtil.readMatrix(br, sldaSyn.theta=new double[param.numDocs][param.numTopics]);
		IOUtil.readMatrix(br, sldaSyn.phi=new double[param.numTopics][param.numVocab]);
		IOUtil.readVector(br, sldaSyn.eta=new double[param.numTopics]);
		br.close();
		return sldaSyn;
	}
	
	public void compareParams(String synParamFileName, String synModelFileName) throws IOException
	{
		SLDASyn syn1=readParams(synParamFileName);
		SLDASyn syn2=readParams(synModelFileName);
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
	
	public void writeParams(String paramFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(paramFileName));
		IOUtil.writeMatrix(bw, theta);
		IOUtil.writeMatrix(bw, phi);
		IOUtil.writeVector(bw, eta);
		bw.close();
	}
	
	public final void writeSynModel(SLDA lda, String synModelFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(synModelFileName));
		IOUtil.writeMatrix(bw, lda.getDocTopicDist());
		IOUtil.writeMatrix(bw, lda.getTopicVocabDist());
		IOUtil.writeVector(bw, lda.eta);
		bw.close();
	}
	
	public SLDASyn(LDASynParam parameters)
	{
		super(parameters);
	}
	
	public static void main(String args[]) throws IOException
	{
		String seg[]=Thread.currentThread().getStackTrace()[1].getClassName().split("\\.");
		String modelName=seg[seg.length-1];
		
		LDASynParam param=new LDASynParam();
		SLDASyn sldaSyn=new SLDASyn(param);
		sldaSyn.generateCorpus();
		sldaSyn.generateLabels();
		sldaSyn.writeCorpus(LDACfg.getSynCorpusFileName(modelName));
		sldaSyn.writeLabels(LDACfg.getSynLabelFileName(modelName));
		sldaSyn.writeParams(LDACfg.getSynParamFileName(modelName));
		
		SLDA slda=new SLDA(new LDAParam(param));
		slda.readCorpus(LDACfg.getSynCorpusFileName(modelName));
		slda.readLabels(LDACfg.getSynLabelFileName(modelName));
		slda.initialize();
		slda.sample(100);
		
		sldaSyn.writeSynModel(slda, LDACfg.getSynModelFileName(modelName));
		sldaSyn.compareParams(LDACfg.getSynParamFileName(modelName), LDACfg.getSynModelFileName(modelName));
	}
}
