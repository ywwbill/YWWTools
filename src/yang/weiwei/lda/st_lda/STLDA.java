package yang.weiwei.lda.st_lda;

import java.io.IOException;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import yang.weiwei.util.MathUtil;
import yang.weiwei.lda.LDA;
import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.util.IOUtil;

/**
 * Sing topic LDA
 * @author Weiwei Yang
 *
 */
public class STLDA extends LDA
{
	protected int numShortDocs;
	protected ArrayList<ShortLDADoc> shortCorpus;
	protected double shortTheta[];
	protected int shortDocTopicCounts[];
	
	/**
	 * Read short corpus
	 * @param shortCorpusFileName Short corpus file name
	 * @throws IOException IOException
	 */
	public void readShortCorpus(String shortCorpusFileName) throws IOException
	{
		readShortCorpus(shortCorpusFileName, true);
	}
	
	public void readShortCorpus(String shortCorpusFileName, boolean indexed) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(shortCorpusFileName));
		String line;
		while ((line=br.readLine())!=null)
		{
			if (indexed)
			{
				shortCorpus.add(new ShortLDADoc(line, param.numTopics, param.numVocab));
			}
			else
			{
				shortCorpus.add(new ShortLDADoc(line, param.numTopics, param.vocabMap));
			}
		}
		br.close();
		numShortDocs=shortCorpus.size();
	}
	
	protected void printParam()
	{
		super.printParam();
		IOUtil.println("\t#short docs: "+numShortDocs);
	}
	
	protected void initDocVariables()
	{
		super.initDocVariables();
		for (int doc=0; doc<numShortDocs; doc++)
		{
			numWords+=shortCorpus.get(doc).docLength();
			int sampleSize=getSampleSize(shortCorpus.get(doc).docLength());
			updateDenom+=(double)(sampleSize)/(double)(sampleSize+param.alpha*param.numTopics);
		}
		getNumTestWords();
	}
	
	protected void initTopicAssigns()
	{
		super.initTopicAssigns();
		for (ShortLDADoc doc : shortCorpus)
		{
			int topic=randoms.nextInt(param.numTopics);
			doc.assignTopic(topic);
			shortDocTopicCounts[topic]++;
			
			int interval=getSampleInterval();
			for (int token=0; token<doc.docLength(); token+=interval)
			{
				int word=doc.getWord(token);
				topics[topic].addVocab(word);
			}
		}
	}
	
	public void initialize(String topicAssignFileName,
			String shortTopicAssignFileName) throws IOException
	{
		super.initialize(shortTopicAssignFileName);
		initShortTopicAssigns(shortTopicAssignFileName);
	}
	
	protected void initShortTopicAssigns(String shortTopicAssignFileName) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(shortTopicAssignFileName));
		String line;
		for (int doc=0; doc<numShortDocs; doc++)
		{
			line=br.readLine();
			int topic=randoms.nextInt(param.numTopics);
			if (line!=null && line.length()>0)
			{
				topic=Integer.valueOf(line);
			}
			
			shortCorpus.get(doc).assignTopic(topic);
			int interval=getSampleInterval();
			for (int token=0; token<shortCorpus.get(doc).docLength(); token+=interval)
			{
				int word=shortCorpus.get(doc).getWord(token);
				topics[topic].addVocab(word);
			}
		}
		br.close();
	}
	
	public void sample(int numIters)
	{
		for (int iteration=1; iteration<=numIters; iteration++)
		{
			for (int doc=0; doc<numDocs; doc++)
			{
				sampleDoc(doc);
			}
			for (int doc=0; doc<numShortDocs; doc++)
			{
				sampleShortDoc(doc);
			}
			computeLogLikelihood();
			perplexity=Math.exp(-logLikelihood/numTestWords);
			if (param.verbose)
			{
				IOUtil.println("<"+iteration+">"+"\tLog-LLD: "+format(logLikelihood)+"\tPPX: "+format(perplexity));
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
	
	protected void sampleShortDoc(int doc)
	{
		int oldTopic,newTopic,interval=getSampleInterval();;
		double topicScores[]=new double[param.numTopics];
		
		oldTopic=shortCorpus.get(doc).getTopicAssign();
		shortCorpus.get(doc).unassignTopic();
		shortDocTopicCounts[oldTopic]--;
		for (int token=0; token<shortCorpus.get(doc).docLength(); token+=interval)
		{
			int word=shortCorpus.get(doc).getWord(token);
			topics[oldTopic].removeVocab(word);
		}
		
		for (int topic=0; topic<param.numTopics; topic++)
		{
			topicScores[topic]=shortTopicUpdating(doc, topic);
		}
		newTopic=MathUtil.selectLogDiscrete(topicScores);
		if (newTopic==-1)
		{
			newTopic=oldTopic;
			for (int topic=0; topic<param.numTopics; topic++)
			{
				IOUtil.println(format(topicScores[topic]));
			}
		}
		
		shortCorpus.get(doc).assignTopic(newTopic);
		shortDocTopicCounts[newTopic]++;
		for (int token=0; token<shortCorpus.get(doc).docLength(); token+=interval)
		{
			int word=shortCorpus.get(doc).getWord(token);
			topics[newTopic].addVocab(word);
		}
	}
	
	protected double shortTopicUpdating(int doc, int topic)
	{
		double score1=Math.log(shortDocTopicCounts[topic]+alpha[topic]);
		double score2=0.0;
		if (type==TRAIN)
		{
			for (int word : shortCorpus.get(doc).getWordSet())
			{
				int count=shortCorpus.get(doc).getWordCount(word);
				for (int i=0; i<count; i++)
				{
					score2+=Math.log(topics[topic].getVocabCount(word)+param.beta+i);
				}
			}
			for (int i=0; i<shortCorpus.get(doc).docLength(); i++)
			{
				score2-=Math.log(topics[topic].getTotalTokens()+param.numVocab*param.beta+i);
			}
		}
		else
		{
			for (int word : shortCorpus.get(doc).getWordSet())
			{
				int count=shortCorpus.get(doc).getWordCount(word);
				score2+=Math.log(phi[topic][word])*count;
			}
		}
		return score1+score2;
	}
	
	protected void computeLogLikelihood()
	{
		super.computeLogLikelihood();
		computeShortTheta();
		
		double sum;
		for (int doc=0; doc<numShortDocs; doc++)
		{
			for (int word : shortCorpus.get(doc).getWordSet())
			{
				sum=0.0;
				for (int topic=0; topic<param.numTopics; topic++)
				{
					sum+=shortTheta[topic]*phi[topic][word];
				}
				logLikelihood+=Math.log(sum)*shortCorpus.get(doc).getWordCount(word);
			}
		}
	}
	
	protected void computeShortTheta()
	{
		for (int topic=0; topic<param.numTopics; topic++)
		{
			shortTheta[topic]=(alpha[topic]+shortDocTopicCounts[topic])/
					(param.alpha*param.numTopics+numShortDocs);
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
	 * Get short documents' topic assignments
	 * @return Short documents' topic assignments
	 */
	public int[] getShortDocTopicAssign()
	{
		int shortDocTopicAssign[]=new int[numShortDocs];
		for (int doc=0; doc<numShortDocs; doc++)
		{
			shortDocTopicAssign[doc]=shortCorpus.get(doc).getTopicAssign();
		}
		return shortDocTopicAssign;
	}
	
	/**
	 * Get short documents' background topic distribution
	 * @return Short documents' background topic distribution
	 */
	public double[] getShortDocTopicDist()
	{
		return shortTheta.clone();
	}
	
	/**
	 * Get number of short documents
	 * @return Number of short documents
	 */
	public int getNumShortDocs()
	{
		return numShortDocs;
	}
	
	/**
	 * Get a specific short document
	 * @param doc Short document number
	 * @return Corresponding short document object
	 */
	public ShortLDADoc getShortDoc(int doc)
	{
		return shortCorpus.get(doc);
	}
	
	/**
	 * Write short documents' background topic distribution to file
	 * @param shortDocTopicDistFileName Short documents' background topic distribution file name
	 * @throws IOException IOException
	 */
	public void writeShortDocTopicDist(String shortDocTopicDistFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(shortDocTopicDistFileName));
		IOUtil.writeVector(bw, shortTheta);
		bw.close();
	}
	
	/**
	 * Write short documents' topic assignments to file
	 * @param topicAssignFileName Short documents' topic assignment file name
	 * @throws IOException IOException
	 */
	public void writeShortDocTopicAssign(String topicAssignFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(topicAssignFileName));
		IOUtil.writeVector(bw, getShortDocTopicAssign());
		bw.close();
	}
	
	protected void initVariables()
	{
		super.initVariables();
		shortCorpus=new ArrayList<ShortLDADoc>();
		shortTheta=new double[param.numTopics];
		shortDocTopicCounts=new int[param.numTopics];
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
		LDAParam parameters=new LDAParam(LDACfg.stldaVocabFileName);
		
		STLDA LDATrain=new STLDA(parameters);
		LDATrain.readCorpus(LDACfg.stldaLongCorpusFileName);
		LDATrain.readShortCorpus(LDACfg.stldaShortCorpusFileName);
		LDATrain.initialize();
		LDATrain.sample(LDACfg.numTrainIters);
//		LDATrain.writeModel(LDACfg.getModelFileName(modelName));
		
		STLDA LDATest=new STLDA(LDATrain, parameters);
//		STLDA LDATest=new STLDA(LDACfg.getModelFileName(modelName), parameters);
		LDATest.readCorpus(LDACfg.stldaLongCorpusFileName);
		LDATest.readShortCorpus(LDACfg.stldaShortCorpusFileName);
		LDATest.initialize();
		LDATest.sample(LDACfg.numTestIters);
	}
}
