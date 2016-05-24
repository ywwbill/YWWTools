package yang.weiwei.lda.st_lda;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;

import yang.weiwei.util.MathUtil;
import yang.weiwei.lda.LDA;
import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.util.LDAResult;
import yang.weiwei.util.IOUtil;

/**
 * Sing topic LDA
 * @author Weiwei Yang
 *
 */
public class STLDA extends LDA
{
	protected int docTopicAssign[];
	protected int topicCounts[];
	protected double theta[];
	
	protected void initDocVariables()
	{
		super.initDocVariables();
		docTopicAssign=new int[numDocs];
		theta=new double[param.numTopics];
		topicCounts=new int[param.numTopics];
	}
	
	protected void initTopicAssigns()
	{
		for (int doc=0; doc<numDocs; doc++)
		{
			int topic=randoms.nextInt(param.numTopics);
			docTopicAssign[doc]=topic;
			topicCounts[topic]++;
			for (int token=0; token<corpus.get(doc).docLength(); token++)
			{
				int word=corpus.get(doc).getWord(token);
				topics[topic].addVocab(word);
			}
		}
	}
	
	protected void sampleDoc(int doc)
	{
		int oldTopic=docTopicAssign[doc];
		topicCounts[oldTopic]--;
		for (int token=0; token<corpus.get(doc).docLength(); token++)
		{
			topics[oldTopic].removeVocab(corpus.get(doc).getWord(token));
		}
		
		double topicScores[]=new double[param.numTopics];
		for (int topic=0; topic<param.numTopics; topic++)
		{
			topicScores[topic]=topicUpdating(doc, topic);
		}
		int newTopic=MathUtil.selectLogDiscrete(topicScores);
		if (newTopic==-1)
		{
			newTopic=oldTopic;
			for (int topic=0; topic<param.numTopics; topic++)
			{
				IOUtil.println(format(topicScores[topic]));
			}
		}
		
		docTopicAssign[doc]=newTopic;
		topicCounts[newTopic]++;
		for (int token=0; token<corpus.get(doc).docLength(); token++)
		{
			topics[newTopic].addVocab(corpus.get(doc).getWord(token));
		}
	}
	
	protected double topicUpdating(int doc, int topic)
	{
		if (type==TRAIN)
		{
			double score1=Math.log(topicCounts[topic]+alpha[topic]);
			double score2=0.0;
			for (int word : corpus.get(doc).getWordSet())
			{
				int count=corpus.get(doc).getWordCount(word);
				for (int i=0; i<count; i++)
				{
					score2+=Math.log(topics[topic].getVocabCount(word)+param.beta+i);
				}
			}
			for (int i=0; i<corpus.get(doc).docLength(); i++)
			{
				score2-=Math.log(topics[topic].getTotalTokens()+param.numVocab*param.beta+i);
			}
			return score1+score2;
		}
		double score1=Math.log(topicCounts[topic]+alpha[topic]);
		double score2=0.0;
		for (int token=0; token<corpus.get(doc).docLength(); token++)
		{
			int word=corpus.get(doc).getWord(token);
			score2+=Math.log(phi[topic][word]);
		}
		return score1+score2;
	}
	
	protected void computeLogLikelihood()
	{
		computeTheta();
		if (type==TRAIN)
		{
			computePhi();
		}
		
		double sum;
		logLikelihood=0.0;
		for (int doc=0; doc<numDocs; doc++)
		{
			for (int word : corpus.get(doc).getWordSet())
			{
				sum=0.0;
				for (int topic=0; topic<param.numTopics; topic++)
				{
					sum+=theta[topic]*phi[topic][word];
				}
				logLikelihood+=Math.log(sum*corpus.get(doc).getWordCount(word));
			}
		}
	}
	
	protected void computeTheta()
	{
		for (int topic=0; topic<param.numTopics; topic++)
		{
			theta[topic]=(alpha[topic]+topicCounts[topic])/(param.alpha*param.numTopics+numDocs);
		}
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
	 * Get all documents' topic assignments
	 * @return Documents' topic assignments
	 */
	public int[] getDocTopicAssign()
	{
		return docTopicAssign;
	}
	
	/**
	 * Get number of documents assigned to topics
	 * @return Number of documents assigned to topics
	 */
	public int[] getTopicCounts()
	{
		return topicCounts;
	}
	
	/**
	 * Get topic distribution
	 * @return Topic distribution
	 */
	public double[] getTopicDist()
	{
		return theta;
	}
	
	/**
	 * Write documents' topic assignments to file
	 * @param topicAssignFileName Topic assignment file name
	 * @throws IOException IOException
	 */
	public void writeDocTopicAssign(String topicAssignFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(topicAssignFileName));
		IOUtil.writeVector(bw, docTopicAssign);
		bw.close();
	}
	
	/**
	 * Initialize an ST-LDA object for training
	 * @param parameters Parameters
	 */
	public STLDA(LDAParam parameters)
	{
		super(parameters);
	}
	
	/**
	 * Initialize an ST-LDA object for test using a pre-trained ST-LDA object
	 * @param LDATrain Pre-trained ST-LDA object
	 * @param parameters Parameters
	 */
	public STLDA(STLDA LDATrain, LDAParam parameters)
	{
		super(LDATrain, parameters);
	}
	
	/**
	 * Initialize an ST-LDA object for test using a pre-trained ST-LDA model in file
	 * @param modelFileName Model file name
	 * @param parameters Parameters
	 * @throws IOException IOException
	 */
	public STLDA(String modelFileName, LDAParam parameters) throws IOException
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
		
		STLDA LDATrain=new STLDA(parameters);
		LDATrain.readCorpus(LDACfg.trainCorpusFileName);
		LDATrain.initialize();
//		LDATrain.sample(LDACfg.numTrainIters);
		LDATrain.sample(10);
		LDATrain.addResults(trainResults);
//		LDATrain.writeModel(LDACfg.getModelFileName(modelName));
		
		STLDA LDATest=new STLDA(LDATrain, parameters);
//		STLDA LDATest=new STLDA(LDACfg.getModelFileName(modelName), parameters);
		LDATest.readCorpus(LDACfg.testCorpusFileName);
		LDATest.initialize();
//		LDATest.sample(LDACfg.numTestIters);
		LDATest.sample(10);
		LDATest.addResults(testResults);
		
		trainResults.printResults(modelName+" Training Perplexity:", LDAResult.PERPLEXITY);
		testResults.printResults(modelName+" Test Perplexity:", LDAResult.PERPLEXITY);
	}
}
