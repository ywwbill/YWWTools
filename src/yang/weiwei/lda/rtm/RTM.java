package yang.weiwei.lda.rtm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import yang.weiwei.util.MathUtil;
import yang.weiwei.lda.LDA;
import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.util.IOUtil;
import cc.mallet.optimize.LimitedMemoryBFGS;
import com.google.gson.annotations.Expose;

/**
 * Relational topic model
 * @author Weiwei Yang
 *
 */
public class RTM extends LDA
{
	public static final int TRAIN_GRAPH=0;
	public static final int TEST_GRAPH=1;
	
	@Expose protected double eta[];
	
	protected ArrayList<HashMap<Integer, Integer>> trainEdgeWeights;
	protected int numTrainEdges;
	
	protected ArrayList<HashMap<Integer, Integer>> testEdgeWeights;
	protected int numTestEdges;
	
	protected double weight[];
	
	protected double error;
	protected double PLR;
	protected double avgWeight;
	
	public void readCorpus(String corpusFileName) throws IOException
	{
		super.readCorpus(corpusFileName);
		for (int doc=0; doc<numDocs; doc++)
		{
			trainEdgeWeights.add(new HashMap<Integer, Integer>());
			testEdgeWeights.add(new HashMap<Integer, Integer>());
		}
	}
	
	/**
	 * Read document links
	 * @param graphFileName Graph file name
	 * @param graphType Graph type
	 * @throws IOException IOException
	 */
	public void readGraph(String graphFileName, int graphType) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(graphFileName));
		String line,seg[];
		int u,v,w;
		while ((line=br.readLine())!=null)
		{
			seg=line.split("\t");
			u=Integer.valueOf(seg[0]);
			v=Integer.valueOf(seg[1]);
			w=(seg.length>=3? Integer.valueOf(seg[2]):1);
			if (corpus.get(u).docLength()==0 || corpus.get(v).docLength()==0) continue;
			
			if (graphType==TRAIN_GRAPH)
			{
				trainEdgeWeights.get(u).put(v, w);
				numTrainEdges++;
				if (!param.directed)
				{
					trainEdgeWeights.get(v).put(u, w);
					numTrainEdges++;
				}
			}
			if (graphType==TEST_GRAPH && w>0)
			{
				testEdgeWeights.get(u).put(v, w);
				numTestEdges++;
				if (!param.directed)
				{
					testEdgeWeights.get(v).put(u, w);
					numTestEdges++;
				}
			}
		}
		if (graphType==TRAIN_GRAPH && param.negEdge)
		{
			sampleNegEdge();
		}
		br.close();
	}
	
	protected void sampleNegEdge()
	{
		int numNegEdges=(int)(numTrainEdges*param.negEdgeRatio),u,v;
		for (int i=0; i<numNegEdges; i++)
		{
			u=randoms.nextInt(numDocs);
			v=randoms.nextInt(numDocs);
			while (u==v || corpus.get(u).docLength()==0 || corpus.get(v).docLength()==0 || trainEdgeWeights.get(u).containsKey(v))
			{
				u=randoms.nextInt(numDocs);
				v=randoms.nextInt(numDocs);
			}
			trainEdgeWeights.get(u).put(v, 0);
			numTrainEdges++;
		}
	}
	
	protected void printParam()
	{
		super.printParam();
		param.printRTMParam("\t");
		IOUtil.println("\t#train edges: "+numTrainEdges);
		if (param.negEdge) IOUtil.println("\t#neg edges: "+(int)(numTrainEdges*param.negEdgeRatio));
	}
	
	protected void printMetrics()
	{
		super.printMetrics();
		IOUtil.println("Predictive Link Rank: "+format(PLR));
	}
	
	public void sample(int numIters)
	{
		for (int iteration=1; iteration<=numIters; iteration++)
		{
			for (int doc=0; doc<numDocs; doc++)
			{
				weight=new double[trainEdgeWeights.get(doc).size()];
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
				IOUtil.println("<"+iteration+">"+"\tLog-LLD: "+format(logLikelihood)+
						"\tPPX: "+format(perplexity));
			}
			
			computeAvgWeight();
			if (param.verbose && numTestEdges>0)
			{
				IOUtil.print("\tAvg Weight: "+format(avgWeight));
			}
			
			computeError();
			if (param.verbose && numTestEdges>0)
			{
				IOUtil.print("\tError: "+format(error));
			}
			
			if (iteration%param.showPLRInterval==0 || iteration==numIters) computePLR();
			if (param.verbose && numTestEdges>0)
			{
				IOUtil.print("\tPLR: "+format(PLR));
			}
			
			if (param.verbose) IOUtil.println();
			
			if (param.updateAlpha && iteration%param.updateAlphaInterval==0 && type==TRAIN)
			{
				updateHyperParam();
			}
		}
		
		if (type==TRAIN)
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
		int oldTopic,newTopic,i=0;
		for (int d : trainEdgeWeights.get(doc).keySet())
		{
			weight[i]=computeWeight(doc, d);
			i++;
		}
		
		int interval=getSampleInterval();
		for (int token=0; token<corpus.get(doc).docLength(); token+=interval)
		{
			oldTopic=unassignTopic(doc, token);
			i=0;
			for (int d : trainEdgeWeights.get(doc).keySet())
			{
				weight[i]-=eta[oldTopic]/corpus.get(doc).docLength()*
						corpus.get(d).getTopicCount(oldTopic)/corpus.get(d).docLength();
				i++;
			}
			
			newTopic=sampleTopic(doc, token, oldTopic);
			
			assignTopic(doc, token, newTopic);
			i=0;
			for (int d : trainEdgeWeights.get(doc).keySet())
			{
				weight[i]+=eta[newTopic]/corpus.get(doc).docLength()*
						corpus.get(d).getTopicCount(newTopic)/corpus.get(d).docLength();
				i++;
			}
		}
	}
	
	protected int sampleTopic(int doc, int token, int oldTopic)
	{
		int word=corpus.get(doc).getWord(token);
		double topicScores[]=new double[param.numTopics];
		for (int topic=0; topic<param.numTopics; topic++)
		{
			topicScores[topic]=topicUpdating(doc, topic, word);
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
		
		return newTopic;
	}
	
	protected double topicUpdating(int doc, int topic, int vocab)
	{
		double score=0.0;
		if (type==TRAIN)
		{
			score=Math.log((alpha[topic]+corpus.get(doc).getTopicCount(topic))*
					(param.beta+topics[topic].getVocabCount(vocab))/
					(param.beta*param.numVocab+topics[topic].getTotalTokens()));
		}
		else
		{
			score=Math.log((alpha[topic]+corpus.get(doc).getTopicCount(topic))*phi[topic][vocab]);
		}
		
		int i=0;
		double temp;
		for (int d : trainEdgeWeights.get(doc).keySet())
		{
			temp=MathUtil.sigmoid(weight[i]+eta[topic]/corpus.get(doc).docLength()*
					corpus.get(d).getTopicCount(topic)/corpus.get(d).docLength());
			score+=Math.log(trainEdgeWeights.get(doc).get(d)>0? temp : 1.0-temp);
			i++;
		}
		return score;
	}
	
	protected void optimize()
	{
		RTMFunction optimizable=new RTMFunction(this);
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
	
	protected double computeWeight(int doc1, int doc2)
	{
		double weight=0.0;
		for (int topic=0; topic<param.numTopics; topic++)
		{
			weight+=eta[topic]*corpus.get(doc1).getTopicCount(topic)/corpus.get(doc1).docLength()*
					corpus.get(doc2).getTopicCount(topic)/corpus.get(doc2).docLength();
		}
		return weight;
	}
	
	protected double computeEdgeProb(int doc1, int doc2)
	{
		return MathUtil.sigmoid((computeWeight(doc1, doc2)));
	}
	
	protected void computeError()
	{
		error=0.0;
		if (numTestEdges==0) return;
		for (int doc=0; doc<numDocs; doc++)
		{
			for (int d : testEdgeWeights.get(doc).keySet())
			{
				error+=1.0-computeEdgeProb(doc, d);
			}
		}
		error/=(double)numTestEdges;
	}
	
	protected void computePLR()
	{
		PLR=0.0;
		if (numTestEdges==0) return;
		ArrayList<RTMDocProb> docProbs=new ArrayList<RTMDocProb>();
		for (int doc=0; doc<numDocs; doc++)
		{
			if (testEdgeWeights.get(doc).size()==0) continue;
			docProbs.clear();
			for (int d=0; d<numDocs; d++)
			{
				if (d==doc) continue;
				docProbs.add(new RTMDocProb(d, computeEdgeProb(doc, d)));
			}
			Collections.sort(docProbs);
			for (int i=0; i<docProbs.size(); i++)
			{
				if (testEdgeWeights.get(doc).containsKey(docProbs.get(i).getDocNo()))
				{
					PLR+=i+1;
				}
			}
		}
		PLR/=(double)numTestEdges;
	}
	
	protected void computeAvgWeight()
	{
		avgWeight=0.0;
		if (numTestEdges==0) return;
		for (int doc=0; doc<numDocs; doc++)
		{
			for (int d : testEdgeWeights.get(doc).keySet())
			{
				avgWeight+=computeWeight(doc, d);
			}
		}
		avgWeight/=(double)numTestEdges;
	}
	
	/**
	 * Write predictive link rank to file
	 * @param plrFileName PLR file name
	 * @throws IOException IOException
	 */
	public void writePLR(String plrFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(plrFileName));
		ArrayList<RTMDocProb> docProbs=new ArrayList<RTMDocProb>();
		for (int doc=0; doc<numDocs; doc++)
		{
			if (testEdgeWeights.get(doc).size()==0) continue;
			docProbs.clear();
			for (int d=0; d<numDocs; d++)
			{
				if (d==doc) continue;
				docProbs.add(new RTMDocProb(d, computeEdgeProb(doc, d)));
			}
			Collections.sort(docProbs);
			for (int i=0; i<docProbs.size(); i++)
			{
				if (testEdgeWeights.get(doc).containsKey(docProbs.get(i).getDocNo()))
				{
					bw.write(doc+"\t"+docProbs.get(i).getDocNo()+"\t"+(i+1));
					bw.newLine();
				}
			}
		}
		bw.close();
	}
	
	/**
	 * Write predicted document link probabilities to file
	 * @param predFileName Prediction file name
	 * @throws IOException IOException
	 */
	public void writePred(String predFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(predFileName));
		for (int doc1=0; doc1<numDocs; doc1++)
		{
			for (int doc2=doc1+1; doc2<numDocs; doc2++)
			{
				double prob=computeEdgeProb(doc1, doc2);
				bw.write(prob+" ");
			}
			bw.newLine();
		}
		bw.close();
	}
	
	/**
	 * Write regression values to file
	 * @param regFileName Regression value file name
	 * @throws IOException IOException
	 */
	public void writeRegValues(String regFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(regFileName));
		for (int doc1=0; doc1<numDocs; doc1++)
		{
			for (int doc2=0; doc2<numDocs; doc2++)
			{
				double reg=computeWeight(doc1, doc2);
				bw.write(reg+" ");
			}
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
	 * Get predictive link rank
	 * @return PLR
	 */
	public double getPLR()
	{
		return PLR;
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
	
	public Set<Integer> getTrainLinkedDocs(int doc)
	{
		return trainEdgeWeights.get(doc).keySet();
	}
	
	public int getTrainEdgeWeight(int doc1, int doc2)
	{
		return trainEdgeWeights.get(doc1).get(doc2);
	}
	
	protected void initVariables()
	{
		super.initVariables();
		trainEdgeWeights=new ArrayList<HashMap<Integer, Integer>>();
		testEdgeWeights=new ArrayList<HashMap<Integer, Integer>>();
		eta=new double[param.numTopics];
	}
	
	protected void copyModel(LDA LDAModel)
	{
		super.copyModel(LDAModel);
		eta=((RTM)LDAModel).eta.clone();
	}
	
	/**
	 * Initialize an RTM object for training
	 * @param parameters Parameters
	 */
	public RTM(LDAParam parameters)
	{
		super(parameters);
	}
	
	/**
	 * Initialize an RTM object for test using a pre-trained RTM object
	 * @param RTMTrain Pre-trained RTM object
	 * @param parameters Parameters
	 */
	public RTM(RTM RTMTrain, LDAParam parameters)
	{
		super(RTMTrain, parameters);
	}
	
	/**
	 * Initialize an RTM object for test using a pre-trained RTM model in file
	 * @param modelFileName Model file name
	 * @param parameters Parameters
	 * @throws IOException IOException
	 */
	public RTM(String modelFileName, LDAParam parameters) throws IOException
	{
		super(modelFileName, parameters);
	}
	
	public static void main(String args[]) throws IOException
	{
		LDAParam parameters=new LDAParam(LDACfg.rtmVocabFileName);
		
		RTM RTMTrain=new RTM(parameters);
		RTMTrain.readCorpus(LDACfg.rtmTrainCorpusFileName);
		RTMTrain.readGraph(LDACfg.rtmTrainLinkFileName, TRAIN_GRAPH);
		RTMTrain.readGraph(LDACfg.rtmTrainLinkFileName, TEST_GRAPH);
		RTMTrain.initialize();
		RTMTrain.sample(LDACfg.numTrainIters);
//		RTMTrain.writeModel(LDACfg.getModelFileName(modelName));
		
		RTM RTMTest=new RTM(RTMTrain, parameters);
//		RTM RTMTest=new RTM(LDACfg.getModelFileName(modelName), parameters);
		RTMTest.readCorpus(LDACfg.rtmTestCorpusFileName);
		RTMTest.readGraph(LDACfg.rtmTestLinkFileName, TEST_GRAPH);
		RTMTest.initialize();
		RTMTest.sample(LDACfg.numTestIters);
//		RTMTest.writePred(LDACfg.rtmPredLinkFileName);
	}
}
