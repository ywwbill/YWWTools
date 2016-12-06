package yang.weiwei.lda.rtm.lex_wsb_rtm;

import java.io.IOException;

import cc.mallet.optimize.LimitedMemoryBFGS;
import yang.weiwei.util.MathUtil;
import yang.weiwei.wsbm.WSBM;
import yang.weiwei.wsbm.WSBMParam;
import yang.weiwei.lda.LDA;
import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.rtm.RTM;
import yang.weiwei.util.IOUtil;

import com.google.gson.annotations.Expose;

/**
 * RTM with lexical weights and weighted stochastic block priors
 * @author Weiwei Yang
 *
 */
public class LexWSBRTM extends RTM
{	
	@Expose protected double rho[][];
	@Expose protected double tau[];
	
	protected int blockTopicCounts[][];
	protected int blockTokenCounts[];
	protected double pi[][];
	
	protected WSBM wsbm;
	
	/**
	 * Read document links for WSBM
	 * @param blockGraphFileName Document link file name
	 * @throws IOException IOException
	 */
	public void readBlockGraph(String blockGraphFileName) throws IOException
	{
		if (wsbm==null)
		{
			WSBMParam wsbmParam=new WSBMParam(param, numDocs);
			wsbm=new WSBM(wsbmParam);
			
			blockTopicCounts=new int[param.numBlocks][param.numTopics];
			blockTokenCounts=new int[param.numBlocks];
			if (param.blockFeat) rho=new double[param.numBlocks][param.numBlocks];
			pi=new double[param.numBlocks][param.numTopics];
		}
		wsbm.readGraph(blockGraphFileName);
	}
	
	protected void printParam()
	{
		super.printParam();
		param.printBlockParam("\t");
		if (wsbm!=null) wsbm.param.printParam("\t");
	}
	
	public void initialize()
	{
		super.initialize();
		if (wsbm!=null) wsbm.init();
		initBlockAssigns();
	}
	
	public void initialize(String topicAssignFileName) throws IOException
	{
		super.initialize(topicAssignFileName);
		if (wsbm!=null) wsbm.init();
		initBlockAssigns();
	}
	
	protected void initBlockAssigns()
	{
		if (wsbm==null) return;
		for (int doc=0; doc<numDocs; doc++)
		{
			for (int topic=0; topic<param.numTopics; topic++)
			{
				blockTopicCounts[wsbm.getBlockAssign(doc)][topic]+=corpus.get(doc).getTopicCount(topic);
				blockTokenCounts[wsbm.getBlockAssign(doc)]+=corpus.get(doc).getTopicCount(topic);
			}
		}
	}
	
	protected void printMetrics()
	{
		super.printMetrics();
		if (wsbm==null) return;
		IOUtil.println("Block Log Likelihood: "+format(wsbm.getLogLikelihood()));
	}
	
	public void sample(int numIters)
	{
		for (int iteration=1; iteration<=numIters; iteration++)
		{
			for (int doc=0; doc<numDocs; doc++)
			{
				weight=new double[trainEdgeWeights.get(doc).size()];
				sampleBlock(doc);
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
				if (wsbm!=null)
				{
					IOUtil.print("\tBlock Log-LLD: "+format(wsbm.getLogLikelihood()));
				}
				IOUtil.println();
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
	
	protected void sampleBlock(int doc)
	{
		if (wsbm==null) return;
		int oldBlock=wsbm.getBlockAssign(doc);
		wsbm.sampleNode(doc);
		int newBlock=wsbm.getBlockAssign(doc);
		for (int topic=0; topic<param.numTopics; topic++)
		{
			blockTopicCounts[oldBlock][topic]-=corpus.get(doc).getTopicCount(topic);
			blockTokenCounts[oldBlock]-=corpus.get(doc).getTopicCount(topic);
			blockTopicCounts[newBlock][topic]+=corpus.get(doc).getTopicCount(topic);
			blockTokenCounts[newBlock]+=corpus.get(doc).getTopicCount(topic);
		}
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
			if (wsbm!=null)
			{
				blockTopicCounts[wsbm.getBlockAssign(doc)][oldTopic]--;
				blockTokenCounts[wsbm.getBlockAssign(doc)]--;
			}
			i=0;
			for (int d : trainEdgeWeights.get(doc).keySet())
			{
				weight[i]-=eta[oldTopic]/corpus.get(doc).docLength()*
						corpus.get(d).getTopicCount(oldTopic)/corpus.get(d).docLength();
				i++;
			}
			
			newTopic=sampleTopic(doc, token, oldTopic);
			
			assignTopic(doc, token, newTopic);
			if (wsbm!=null)
			{
				blockTopicCounts[wsbm.getBlockAssign(doc)][newTopic]++;
				blockTokenCounts[wsbm.getBlockAssign(doc)]++;
			}
			i=0;
			for (int d : trainEdgeWeights.get(doc).keySet())
			{
				weight[i]+=eta[newTopic]/corpus.get(doc).docLength()*
						corpus.get(d).getTopicCount(newTopic)/corpus.get(d).docLength();
				i++;
			}
		}
	}
	
	protected double topicUpdating(int doc, int topic, int vocab)
	{
		double score=0.0;
		double ratio=1.0/param.numTopics;
		if (wsbm!=null)
		{
			ratio=(blockTopicCounts[wsbm.getBlockAssign(doc)][topic]+param._alpha)/
					(blockTokenCounts[wsbm.getBlockAssign(doc)]+param._alpha*param.numTopics);
		}
		if (type==TRAIN)
		{
			score=Math.log((param.alpha*param.numTopics*ratio+corpus.get(doc).getTopicCount(topic))*
					(param.beta+topics[topic].getVocabCount(vocab))/
					(param.beta*param.numVocab+topics[topic].getTotalTokens()));
		}
		else
		{
			score=Math.log((param.alpha*param.numTopics*ratio+corpus.get(doc).getTopicCount(topic))*phi[topic][vocab]);
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
		LexWSBRTMFunction optimizable=new LexWSBRTMFunction(this);
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
		for (int vocab=0; vocab<param.numVocab; vocab++)
		{
			tau[vocab]=optimizable.getParameter(vocab+param.numTopics);
		}
		if (param.blockFeat && wsbm!=null)
		{
			for (int b1=0; b1<param.numBlocks; b1++)
			{
				for (int b2=0; b2<param.numBlocks; b2++)
				{
					int pos=b1*param.numBlocks+b2+param.numTopics+param.numVocab;
					rho[b1][b2]=optimizable.getParameter(pos);
				}
			}
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
		for (int token : corpus.get(doc1).getWordSet())
		{
			if (corpus.get(doc2).containsWord(token))
			{
				weight+=tau[token]*corpus.get(doc1).getWordCount(token)/corpus.get(doc1).docLength()*
						corpus.get(doc2).getWordCount(token)/corpus.get(doc2).docLength();
			}
		}
		if (param.blockFeat && wsbm!=null)
		{
			int b1=wsbm.getBlockAssign(doc1),b2=wsbm.getBlockAssign(doc2);
			weight+=rho[b1][b2]*wsbm.getBlockEdgeRate(b1, b2);
		}
		return weight;
	}
	
	protected void computeLogLikelihood()
	{
		super.computeLogLikelihood();
		if (wsbm!=null) wsbm.computeLogLikelihood();
	}
	
	protected void computePi()
	{
		if (wsbm==null) return;
		for (int l=0; l<param.numBlocks; l++)
		{
			for (int topic=0; topic<param.numTopics; topic++)
			{
				pi[l][topic]=(blockTopicCounts[l][topic]+param._alpha)/(blockTokenCounts[l]+param._alpha*param.numTopics);
			}
		}
	}
	
	protected void computeTheta()
	{
		if (wsbm==null)
		{
			super.computeTheta();
			return;
		}
		computePi();
		for (int doc=0; doc<numDocs; doc++)
		{
			for (int topic=0; topic<param.numTopics; topic++)
			{
				theta[doc][topic]=(param.alpha*param.numTopics*pi[wsbm.getBlockAssign(doc)][topic]+corpus.get(doc).getTopicCount(topic))/
						(param.alpha*param.numTopics+getSampleSize(corpus.get(doc).docLength()));
			}
		}
	}
	
	/**
	 * Write blocks to file
	 * @param blockFileName Block file name
	 * @throws IOException IOException
	 */
	public void writeBlocks(String blockFileName) throws IOException
	{
		if (wsbm==null) return;
		wsbm.writeBlockAssign(blockFileName);
	}
	
	/**
	 * Print blocks on console
	 */
	public void printBlocks()
	{
		if (wsbm==null) return;
		wsbm.printResults();
	}
	
	/**
	 * Get a word's weight
	 * @param vocab Word
	 * @return Word's weight
	 */
	public double getLexWeight(int vocab)
	{
		return tau[vocab];
	}
	
	/**
	 * Get words' weights
	 * @return Words' weights
	 */
	public double[] getLexWeights()
	{
		return tau.clone();
	}
	
	public double getBlockEdgeRate(int block1, int block2)
	{
		if (wsbm==null) return 0.0;
		return wsbm.getBlockEdgeRate(block1, block2);
	}
	
	public double[][] getBlockEdgeRates()
	{
		if (wsbm==null) return null;
		return wsbm.getBlockEdgeRates();
	}
	
	/**
	 * Get block distribution
	 * @return Block distribution
	 */
	public double[] getBlockDist()
	{
		if (wsbm==null) return null;
		return wsbm.getBlockDist();
	}
	
	/**
	 * Get block assignment of the given document
	 * @param doc Document number
	 * @return Given document's block assignment
	 */
	public int getBlockAssign(int doc)
	{
		if (wsbm==null) return -1;
		return wsbm.getBlockAssign(doc);
	}
	
	/**
	 * Get block distribution over topics
	 * @return Block distribution over topics
	 */
	public double[][] getBlockTopicDist()
	{
		if (wsbm==null) return null;
		return pi.clone();
	}
	
	/**
	 * Get two blocks' weight
	 * @param b1 Block 1
	 * @param b2 Block 2
	 * @return Given blocks' weight
	 */
	public double getBlockWeight(int b1, int b2)
	{
		if (wsbm==null) return 0.0;
		return rho[b1][b2];
	}
	
	/**
	 * Get block weights
	 * @return Block weights
	 */
	public double[][] getBlockWeights()
	{
		if (wsbm==null) return null;
		return rho.clone();
	}
	
	protected WSBM getWSBM()
	{
		return wsbm;
	}
	
	protected void initVariables()
	{
		super.initVariables();
		tau=new double[param.numVocab];
	}
	
	protected void copyModel(LDA LDAModel)
	{
		super.copyModel(LDAModel);
		tau=((LexWSBRTM)LDAModel).tau.clone();
		if (param.blockFeat)
		{
			rho=((LexWSBRTM)LDAModel).rho.clone();
		}
	}
	
	/**
	 * Initialize an Lex-WSB-RTM object for training
	 * @param parameters Parameters
	 */
	public LexWSBRTM(LDAParam parameters)
	{
		super(parameters);
	}
	
	/**
	 * Initialize an Lex-WSB-RTM object for test using a pre-trained Lex-WSB-RTM object
	 * @param RTMTrain Pre-trained Lex-WSB-RTM object
	 * @param parameters Parameters
	 */
	public LexWSBRTM(LexWSBRTM RTMTrain, LDAParam parameters)
	{
		super(RTMTrain, parameters);
	}
	
	/**
	 * Initialize an Lex-WSB-RTM object for test using a pre-trained Lex-WSB-RTM model in file
	 * @param modelFileName Model file name
	 * @param parameters Parameters
	 * @throws IOException IOException
	 */
	public LexWSBRTM(String modelFileName, LDAParam parameters) throws IOException
	{
		super(modelFileName, parameters);
	}
	
	public static void main(String args[]) throws IOException
	{
		LDAParam parameters=new LDAParam(LDACfg.rtmVocabFileName);
		parameters.updateAlpha=false;
		
		LexWSBRTM RTMTrain=new LexWSBRTM(parameters);
		RTMTrain.readCorpus(LDACfg.rtmTrainCorpusFileName);
		RTMTrain.readGraph(LDACfg.rtmTrainLinkFileName, TRAIN_GRAPH);
		RTMTrain.readGraph(LDACfg.rtmTrainLinkFileName, TEST_GRAPH);
		RTMTrain.readBlockGraph(LDACfg.rtmTrainLinkFileName);
		RTMTrain.initialize();
		RTMTrain.sample(LDACfg.numTrainIters);
//		RTMTrain.writeModel(LDACfg.getModelFileName(modelName));
		
		LexWSBRTM RTMTest=new LexWSBRTM(RTMTrain, parameters);
//		LexWSBRTM RTMTest=new LexWSBRTM(LDACfg.getModelFileName(modelName), parameters);
		RTMTest.readCorpus(LDACfg.rtmTestCorpusFileName);
		RTMTest.readGraph(LDACfg.rtmTestLinkFileName, TEST_GRAPH);
		RTMTest.initialize();
		RTMTest.sample(LDACfg.numTestIters);
//		RTMTest.writePred(LDACfg.rtmPredLinkFileName);
	}
}
