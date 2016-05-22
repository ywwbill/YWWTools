package yang.weiwei.lda.slda;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;

import yang.weiwei.util.MathUtil;
import yang.weiwei.lda.LDA;
import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.util.LDAResult;
import yang.weiwei.lda.util.LDAWord;
import yang.weiwei.util.IOUtil;

import com.google.gson.annotations.Expose;
import cc.mallet.optimize.LimitedMemoryBFGS;

public class SLDA extends LDA
{
	@Expose protected double eta[];
	
	protected int numLabels;
	protected double labels[];
	protected double predLabels[];
	protected boolean labelStatuses[];
	
	protected double weight;
	protected double error;
	
	public void readCorpus(String corpusFileName) throws IOException
	{
		super.readCorpus(corpusFileName);
		labels=new double[numDocs];
		predLabels=new double[numDocs];
		labelStatuses=new boolean[numDocs];
		numLabels=0;
		for (int doc=0; doc<numDocs; doc++)
		{
			labelStatuses[doc]=false;
		}
	}
	
	public void readLabels(String labelFileName) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(labelFileName));
		String line;
		for (int doc=0; doc<numDocs; doc++)
		{
			line=br.readLine();
			if (line==null) break;
			if (line.length()>0)
			{
				labels[doc]=Double.valueOf(line);
				labelStatuses[doc]=true;
				numLabels++;
			}
		}
		br.close();
	}
	
	protected void printParam()
	{
		super.printParam();
		param.printSLDAParam("\t");
		IOUtil.println("\t#labels: "+numLabels);
	}
	
	public void sample(int numIters)
	{
		for (int iteration=1; iteration<=numIters; iteration++)
		{
			for (int doc=0; doc<numDocs; doc++)
			{
				sampleDoc(doc);
			}
			computeLogLikelihood();
			perplexity=Math.exp(-logLikelihood/numTestWords);
			
			if (type==TRAIN)
			{
				optimize();
				computeError();
			}
			if (param.verbose)
			{
				IOUtil.println("<"+iteration+">"+"\tLog-LLD: "+format(logLikelihood)+
						"\tPPX: "+format(perplexity)+"\tError: "+format(error));
			}
			if (param.updateAlpha && iteration%param.updateAlphaInterval==0 && type==TRAIN)
			{
				updateHyperParam();
			}
		}
		
		if (type==TRAIN && param.verbose)
		{
			for (int topic=0; topic<param.numTopics; topic++)
			{
				IOUtil.println(topWordsByFreq(topic, 10));
			}
		}
	}
	
	protected void sampleDoc(int doc)
	{
		int oldTopic,newTopic,interval=getSampleInterval();
		weight=computeWeight(doc);
		for (int token=0; token<corpus.get(doc).docLength(); token+=interval)
		{			
			oldTopic=unassignTopic(doc, token);
			if (type==TRAIN && labelStatuses[doc])
			{
				weight+=eta[oldTopic]/corpus.get(doc).docLength();
			}
			
			newTopic=sampleTopic(doc, token, oldTopic);
			
			assignTopic(doc, token, newTopic);
			if (type==TRAIN && labelStatuses[doc])
			{
				weight-=eta[newTopic]/corpus.get(doc).docLength();
			}
		}
	}
	
	protected double topicUpdating(int doc, int topic, int vocab)
	{
		double score=0.0;
		if (type==TRAIN)
		{
			score=(alpha[topic]+corpus.get(doc).getTopicCount(topic))*
					(param.beta+topics[topic].getVocabCount(vocab))/
					(param.beta*param.numVocab+topics[topic].getTotalTokens());
		}
		else
		{
			score=(alpha[topic]+corpus.get(doc).getTopicCount(topic))*phi[topic][vocab];
		}
		
		if (type==TRAIN && labelStatuses[doc])
		{
			score*=Math.exp(-MathUtil.sqr(weight-eta[topic]/corpus.get(doc).docLength()) / (2.0*MathUtil.sqr(param.sigma)));
		}
		
		return score;
	}
	
	protected double computeWeight(int doc)
	{
		double weight=labels[doc];
		for (int topic=0; topic<param.numTopics; topic++)
		{
			weight-=eta[topic]*corpus.get(doc).getTopicCount(topic)/corpus.get(doc).docLength();
		}
		return weight;
	}
	
	protected void optimize()
	{
		SLDAFunction optimizable=new SLDAFunction(this);
		LimitedMemoryBFGS lbfgs=new LimitedMemoryBFGS(optimizable);
		try
		{
			lbfgs.optimize();
		}
		catch (Exception e)
		{
			return;
		}
		for (int topic=0; topic<param.numTopics; topic++)
		{
			eta[topic]=optimizable.getParameter(topic);
		}
	}
	
	protected void computeError()
	{
		error=0.0;
		if (numLabels==0) return;
		for (int doc=0; doc<numDocs; doc++)
		{
			if (!labelStatuses[doc]) continue;
			error+=MathUtil.sqr(computeWeight(doc));
		}
		error=Math.sqrt(error/(double)numLabels);
	}
	
	protected void computePredLabels()
	{
		for (int doc=0; doc<numDocs; doc++)
		{
			predLabels[doc]=computeWeight(doc);
		}
	}
	
	public void addResults(LDAResult result)
	{
		super.addResults(result);
		result.add(LDAResult.ERROR, error);
	}
	
	public void writePredLabels(String predLabelFileName) throws IOException
	{
		computePredLabels();
		BufferedWriter bw=new BufferedWriter(new FileWriter(predLabelFileName));
		IOUtil.writeVector(bw, predLabels);
		bw.close();
	}
	
	public Set<LDAWord> selectWords(double wordWeights[], double posWeightThreshold, double negWeightThreshold)
	{
		Set<LDAWord> wordSet=new HashSet<LDAWord>();
		for (int vocab=0; vocab<param.numVocab; vocab++)
		{
			if (wordWeights[vocab]>posWeightThreshold || wordWeights[vocab]<negWeightThreshold)
			{
				wordSet.add(new LDAWord(param.vocabList.get(vocab), wordWeights[vocab]));
			}
		}
		return wordSet;
	}
	
	public Set<LDAWord> selectWords(double wordWeights[], int posNum, int negNum)
	{
		LDAWord words[]=new LDAWord[param.numVocab];
		for (int vocab=0; vocab<param.numVocab; vocab++)
		{
			words[vocab]=new LDAWord(param.vocabList.get(vocab), wordWeights[vocab]);
		}
		Arrays.sort(words);
		
		Set<LDAWord> wordSet=new HashSet<LDAWord>();
		for (int vocab=0; vocab<posNum; vocab++)
		{
			wordSet.add(words[vocab]);
		}
		for (int vocab=param.numVocab-1; vocab>=param.numVocab-negNum; vocab--)
		{
			wordSet.add(words[vocab]);
		}
		return wordSet;
	}
	
	public Set<LDAWord> selectWords(double wordWeights[], int num)
	{
		double newWeights[]=new double[param.numVocab];
		for (int vocab=0; vocab<param.numVocab; vocab++)
		{
			newWeights[vocab]=Math.abs(wordWeights[vocab]);
		}
		return selectWords(newWeights, num, 0);
	}
	
	public Set<SLDATopicWeight> selectTopics(double topicWeights[], double posWeightThreshold, double negWeightThreshold)
	{
		Set<SLDATopicWeight> topicSet=new HashSet<SLDATopicWeight>();
		for (int topic=0; topic<param.numTopics; topic++)
		{
			if (topicWeights[topic]>posWeightThreshold || topicWeights[topic]<negWeightThreshold)
			{
				topicSet.add(new SLDATopicWeight(phi[topic], topicWeights[topic], topic));
			}
		}
		return topicSet;
	}
	
	public Set<SLDATopicWeight> selectTopics(double topicWeights[], int posNum, int negNum)
	{
		SLDATopicWeight SLDATopics[]=new SLDATopicWeight[param.numTopics];
		for (int topic=0; topic<param.numTopics; topic++)
		{
			SLDATopics[topic]=new SLDATopicWeight(phi[topic], topicWeights[topic], topic);
		}
		Arrays.sort(SLDATopics);
		
		Set<SLDATopicWeight> topicSet=new HashSet<SLDATopicWeight>();
		for (int topic=0; topic<posNum; topic++)
		{
			topicSet.add(SLDATopics[topic]);
		}
		for (int topic=param.numTopics-1; topic>=param.numTopics-negNum; topic--)
		{
			topicSet.add(SLDATopics[topic]);
		}
		return topicSet;
	}
	
	public Set<SLDATopicWeight> selectTopics(double topicWeights[], int num)
	{
		double newTopicWeights[]=new double[param.numTopics];
		for (int topic=0; topic<param.numTopics; topic++)
		{
			newTopicWeights[topic]=Math.abs(topicWeights[topic]);
		}
		return selectTopics(newTopicWeights, num, 0);
	}
	
	public Set<LDAWord> selectWordsFromTopics(Set<SLDATopicWeight> selectedTopics,
			double posWeightThreshold, double negWeightThreshold)
	{
		Set<LDAWord> wordSet=new HashSet<LDAWord>();
		for (SLDATopicWeight topic : selectedTopics)
		{
			wordSet.addAll(selectWords(topic.getWordDist(), posWeightThreshold, negWeightThreshold));
		}
		return wordSet;
	}
	
	public Set<LDAWord> selectWordsFromTopics(Set<SLDATopicWeight> selectedTopics, int num)
	{
		Set<LDAWord> wordSet=new HashSet<LDAWord>();
		for (SLDATopicWeight topic : selectedTopics)
		{
			wordSet.addAll(selectWords(topic.getWordDist(), num));
		}
		return wordSet;
	}
	
	public void writeSelectedWords(Set<LDAWord> wordSet, String wordFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(wordFileName));
		for (LDAWord word : wordSet)
		{
			bw.write(word.getWord());
			bw.newLine();
		}
		bw.close();
	}
	
	protected void getNumTestWords()
	{
		numTestWords=numWords;
	}
	
	protected int getStartPos()
	{
		return 0;
	}
	
	protected int getSampleSize(int docLength)
	{
		return docLength;
	}
	
	protected int getSampleInterval()
	{
		return 1;
	}
	
	public double getTopicWeight(int topic)
	{
		return eta[topic];
	}
	
	public double[] getTopicWeights()
	{
		return eta;
	}
	
	public boolean getLabelStatus(int doc)
	{
		return labelStatuses[doc];
	}
	
	public double getResponseLabel(int doc)
	{
		return labels[doc];
	}
	
	public double getError()
	{
		return error;
	}
	
	protected void initVariables()
	{
		super.initVariables();
		eta=new double[param.numTopics];
	}
	
	protected void copyModel(LDA LDAModel)
	{
		super.copyModel(LDAModel);
		for (int topic=0; topic<param.numTopics; topic++)
		{
			eta[topic]=((SLDA)LDAModel).eta[topic];
		}
	}
	
	public SLDA(LDAParam parameters)
	{
		super(parameters);
		for (int topic=0; topic<param.numTopics; topic++)
		{
			eta[topic]=randoms.nextGaussian(0.0, MathUtil.sqr(param.nu));
		}
	}
	
	public SLDA(SLDA LDATrain, LDAParam parameters)
	{
		super(LDATrain, parameters);
	}
	
	public SLDA(String modelFileName, LDAParam parameters) throws IOException
	{
		super(modelFileName, parameters);
	}
	
	public static void main(String args[]) throws IOException
	{
		String seg[]=Thread.currentThread().getStackTrace()[1].getClassName().split("\\.");
		String modelName=seg[seg.length-1];
		LDAParam parameters=new LDAParam(LDACfg.vocabFileName);
		LDAResult trainResults=new LDAResult();
		LDAResult testResults=new LDAResult();
		
		SLDA LDATrain=new SLDA(parameters);
		LDATrain.readCorpus(LDACfg.trainCorpusFileName);
		LDATrain.readLabels(LDACfg.trainLabelFileName);
		LDATrain.initialize();
		LDATrain.sample(LDACfg.numTrainIters);
		LDATrain.addResults(trainResults);
//		LDATrain.writeModel(LDACfg.getModelFileName(modelName));
		
		SLDA LDATest=new SLDA(LDATrain, parameters);
//		SLDA LDATest=new SLDA(LDACfg.getModelFileName(modelName), parameters);
		LDATest.readCorpus(LDACfg.testCorpusFileName);
		LDATest.initialize();
		LDATest.sample(LDACfg.numTestIters);
		LDATest.addResults(testResults);
		LDATest.writePredLabels(LDACfg.predLabelFileName);
		
		trainResults.printResults(modelName+" Training Error: ", LDAResult.ERROR);
		testResults.printResults(modelName+" Test Error: ", LDAResult.ERROR);
	}
}
