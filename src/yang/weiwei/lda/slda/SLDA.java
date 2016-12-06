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
import yang.weiwei.lda.util.LDAWord;
import yang.weiwei.util.IOUtil;

import com.google.gson.annotations.Expose;
import cc.mallet.optimize.LimitedMemoryBFGS;

/**
 * Supervised LDA
 * @author Weiwei Yang
 *
 */
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
	
	/**
	 * Read labels
	 * @param labelFileName Label file name
	 * @throws IOException IOException
	 */
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
	
	protected void printMetrics()
	{
		super.printMetrics();
		IOUtil.println("Root Mean Sqaured Error: "+format(error));
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
			}
			if (param.verbose)
			{
				IOUtil.print("<"+iteration+">"+"\tLog-LLD: "+format(logLikelihood)+
						"\tPPX: "+format(perplexity));
			}
			
			computeError();
			if (param.verbose && numLabels>0)
			{
				IOUtil.print("\tError: "+format(error));
			}
			
			if (param.verbose) IOUtil.println();
			
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
		
		printMetrics();
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
	
	/**
	 * Write predicted labels
	 * @param predLabelFileName Predicted label file name
	 * @throws IOException IOException
	 */
	public void writePredLabels(String predLabelFileName) throws IOException
	{
		computePredLabels();
		BufferedWriter bw=new BufferedWriter(new FileWriter(predLabelFileName));
		IOUtil.writeVector(bw, predLabels);
		bw.close();
	}
	
	/**
	 * Select words with weights higher than $posWeightThreshold or lower than $negWeightThreshold
	 * @param wordWeights Word weights
	 * @param posWeightThreshold Positive word threshold
	 * @param negWeightThreshold Negative word threshold
	 * @return Set of selected words
	 */
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
	
	/**
	 * Sort the words with weights and select $posNum in the top and $negNum in the bottom
	 * @param wordWeights Word weights
	 * @param posNum Number of words in the top
	 * @param negNum Number of words in the bottom
	 * @return Set of selected words
	 */
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
	
	/**
	 * Sort the words with absolute weights and select top $num words
	 * @param wordWeights Word weights
	 * @param num Number of words to select
	 * @return Set of selected words
	 */
	public Set<LDAWord> selectWords(double wordWeights[], int num)
	{
		double newWeights[]=new double[param.numVocab];
		for (int vocab=0; vocab<param.numVocab; vocab++)
		{
			newWeights[vocab]=Math.abs(wordWeights[vocab]);
		}
		return selectWords(newWeights, num, 0);
	}
	
	/**
	 * Select topics with weights higher than $posWeightThreshold or lower than $negWeightThreshold
	 * @param topicWeights Topic weights
	 * @param posWeightThreshold Positive weight threshold
	 * @param negWeightThreshold Negative weight threshold
	 * @return Set of selected topics
	 */
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
	
	/**
	 * Sort the topics with weights and select $posNum in the top and $negNum in the bottom
	 * @param topicWeights Topic weights
	 * @param posNum Number of topics in the top
	 * @param negNum Number of words in the bottom
	 * @return Set of selected topics
	 */
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
	
	/**
	 * Sort the topics with absolute weights and select top $num topics
	 * @param topicWeights Topic weights
	 * @param num Number of topics to select
	 * @return Set of selected topics
	 */
	public Set<SLDATopicWeight> selectTopics(double topicWeights[], int num)
	{
		double newTopicWeights[]=new double[param.numTopics];
		for (int topic=0; topic<param.numTopics; topic++)
		{
			newTopicWeights[topic]=Math.abs(topicWeights[topic]);
		}
		return selectTopics(newTopicWeights, num, 0);
	}
	
	/**
	 * Select words with weights higher than $posWeightThreshold or lower than $negWeightThreshold from selected topics
	 * @param selectedTopics Set of selected topics
	 * @param posWeightThreshold Positive weight threshold
	 * @param negWeightThreshold Negative weight threshold
	 * @return Set of selected words
	 */
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
	
	/**
	 * Sort the words in each selected topic by weight and select the top $num words
	 * @param selectedTopics Set of selected topics
	 * @param num Number of words to select
	 * @return Set of selected words
	 */
	public Set<LDAWord> selectWordsFromTopics(Set<SLDATopicWeight> selectedTopics, int num)
	{
		Set<LDAWord> wordSet=new HashSet<LDAWord>();
		for (SLDATopicWeight topic : selectedTopics)
		{
			wordSet.addAll(selectWords(topic.getWordDist(), num));
		}
		return wordSet;
	}
	
	/**
	 * Write selected words to file
	 * @param wordSet Set of selected words
	 * @param wordFileName Word file name
	 * @throws IOException IOException
	 */
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
	
	/**
	 * Write document regression values to file
	 * @param regFileName Regression value file name
	 * @throws IOException IOException
	 */
	public void writeRegValues(String regFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(regFileName));
		for (int doc=0; doc<numDocs; doc++)
		{
			double reg=computeWeight(doc);
			bw.write(reg+"");
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
	
	/**
	 * Get the weight of a topic
	 * @param topic Topic
	 * @return The weight of given topic
	 */
	public double getTopicWeight(int topic)
	{
		return eta[topic];
	}
	
	/**
	 * Get topic weights
	 * @return Topic weights
	 */
	public double[] getTopicWeights()
	{
		return eta.clone();
	}
	
	/**
	 * Get the status of a document's label (whether it's available)
	 * @param doc Document number
	 * @return Status of given document's label
	 */
	public boolean getLabelStatus(int doc)
	{
		return labelStatuses[doc];
	}
	
	/**
	 * Get a document's label
	 * @param doc Document number
	 * @return Given document's label
	 */
	public double getResponseLabel(int doc)
	{
		return labels[doc];
	}
	
	/**
	 * Get root mean squared error
	 * @return Root mean squared error
	 */
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
		eta=((SLDA)LDAModel).eta.clone();
	}
	
	/**
	 * Initialize an SLDA object for training
	 * @param parameters Parameters
	 */
	public SLDA(LDAParam parameters)
	{
		super(parameters);
	}
	
	/**
	 * Initialize an SLDA object for test using a pre-trained SLDA object
	 * @param LDATrain Pre-trained SLDA object
	 * @param parameters Parameters
	 */
	public SLDA(SLDA LDATrain, LDAParam parameters)
	{
		super(LDATrain, parameters);
	}
	
	/**
	 * Initialize an SLDA object for test using a pre-trained SLDA model in file
	 * @param modelFileName Model file name
	 * @param parameters Parameters
	 * @throws IOException IOException
	 */
	public SLDA(String modelFileName, LDAParam parameters) throws IOException
	{
		super(modelFileName, parameters);
	}
	
	public static void main(String args[]) throws IOException
	{
		LDAParam parameters=new LDAParam(LDACfg.sldaVocabFileName);
		
		SLDA LDATrain=new SLDA(parameters);
		LDATrain.readCorpus(LDACfg.sldaTrainCorpusFileName);
		LDATrain.readLabels(LDACfg.sldaTrainLabelFileName);
		LDATrain.initialize();
		LDATrain.sample(LDACfg.numTrainIters);
//		LDATrain.writeModel(LDACfg.getModelFileName(modelName));
		
		SLDA LDATest=new SLDA(LDATrain, parameters);
//		SLDA LDATest=new SLDA(LDACfg.getModelFileName(modelName), parameters);
		LDATest.readCorpus(LDACfg.sldaTestCorpusFileName);
		LDATest.readLabels(LDACfg.sldaTestLabelFileName);
		LDATest.initialize();
		LDATest.sample(LDACfg.numTestIters);
//		LDATest.writePredLabels(LDACfg.sldaPredLabelFileName);
	}
}
