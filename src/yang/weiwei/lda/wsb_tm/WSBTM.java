package yang.weiwei.lda.wsb_tm;

import java.io.IOException;

import yang.weiwei.lda.LDA;
import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.util.IOUtil;
import yang.weiwei.wsbm.WSBM;
import yang.weiwei.wsbm.WSBMParam;

/**
 * Weighted stochastic block topic model
 * @author Weiwei Yang
 *
 */
public class WSBTM extends LDA
{	
	protected double pi[][];
	protected int blockTopicCounts[][];
	protected int blockTokenCounts[];
	
	protected WSBM wsbm;
	
	/**
	 * Read graph for WSBM
	 * @param graphFileName Graph file name
	 * @throws IOException IOException
	 */
	public void readGraph(String graphFileName) throws IOException
	{
		if (wsbm==null)
		{
			WSBMParam wsbmParam=new WSBMParam(param, numDocs);
			wsbm=new WSBM(wsbmParam);
			
			blockTopicCounts=new int[param.numBlocks][param.numTopics];
			blockTokenCounts=new int[param.numBlocks];
			pi=new double[param.numBlocks][param.numTopics];
		}
		wsbm.readGraph(graphFileName);
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
				sampleBlock(doc);
				sampleDoc(doc);
			}
			computeLogLikelihood();
			perplexity=Math.exp(-logLikelihood/numTestWords);
			if (param.verbose)
			{
				IOUtil.print("<"+iteration+">"+"\tLog-LLD: "+format(logLikelihood)+"\tPPX: "+format(perplexity));
				if (wsbm!=null)
				{
					IOUtil.print("\tBlock Log-LLD: "+format(wsbm.getLogLikelihood()));
				}
				IOUtil.println();
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
		int oldTopic,newTopic,interval=getSampleInterval();
		for (int token=0; token<corpus.get(doc).docLength(); token+=interval)
		{
			oldTopic=unassignTopic(doc, token);
			if (wsbm!=null)
			{
				blockTopicCounts[wsbm.getBlockAssign(doc)][oldTopic]--;
				blockTokenCounts[wsbm.getBlockAssign(doc)]--;
			}
			
			newTopic=sampleTopic(doc, token, oldTopic);
			
			assignTopic(doc, token, newTopic);
			if (wsbm!=null)
			{
				blockTopicCounts[wsbm.getBlockAssign(doc)][newTopic]++;
				blockTokenCounts[wsbm.getBlockAssign(doc)]++;
			}
		}
	}
	
	protected double topicUpdating(int doc, int topic, int vocab)
	{
		double ratio=1.0/param.numTopics;
		if (wsbm!=null)
		{
			ratio=(blockTopicCounts[wsbm.getBlockAssign(doc)][topic]+param._alpha)/
					(blockTokenCounts[wsbm.getBlockAssign(doc)]+param._alpha*param.numTopics);
		}
		if (type==TRAIN)
		{
			return (param.alpha*param.numTopics*ratio+corpus.get(doc).getTopicCount(topic))*
					(param.beta+topics[topic].getVocabCount(vocab))/
					(param.beta*param.numVocab+topics[topic].getTotalTokens());
		}
		return (param.alpha*param.numTopics*ratio+corpus.get(doc).getTopicCount(topic))*phi[topic][vocab];
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
				pi[l][topic]=(param._alpha+blockTopicCounts[l][topic])/(blockTokenCounts[l]+param._alpha*param.numTopics);
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
	 * Get block distribution over topics
	 * @return Block distribution over topics
	 */
	public double[][] getBlockTopicDist()
	{
		if (wsbm==null) return null;
		return pi.clone();
	}
	
	/**
	 * Write blocks to file
	 * @param blockFileName Block file name
	 * @throws IOException IOException
	 */
	public void writeBlocks(String blockFileName) throws IOException
	{
		if (wsbm==null) return;
		wsbm.writeBlocks(blockFileName);
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
	 * Initialize an WSB-TM object for training
	 * @param parameters Parameters
	 */
	public WSBTM(LDAParam parameters)
	{
		super(parameters);
	}
	
	/**
	 * Initialize an WSB-TM object for test using a pre-trained WSB-TM object
	 * @param LDATrain Pre-trained WSB-TM object
	 * @param parameters Parameters
	 */
	public WSBTM(WSBTM LDATrain, LDAParam parameters)
	{
		super(LDATrain, parameters);
	}
	
	/**
	 * Initialize an WSB-TM object for test using a pre-trained WSB-TM model in file
	 * @param modelFileName Model file name
	 * @param parameters Parameters
	 * @throws IOException IOException
	 */
	public WSBTM(String modelFileName, LDAParam parameters) throws IOException
	{
		super(modelFileName, parameters);
	}
	
	public static void main(String args[]) throws IOException
	{	
		LDAParam parameters=new LDAParam(LDACfg.rtmVocabFileName);
		parameters.updateAlpha=false;
		
		WSBTM LDATrain=new WSBTM(parameters);
		LDATrain.readCorpus(LDACfg.rtmTrainCorpusFileName);
		LDATrain.readGraph(LDACfg.rtmTrainLinkFileName);
		LDATrain.initialize();
		LDATrain.sample(LDACfg.numTrainIters);
//		LDATrain.writeModel(LDACfg.getModelFileName(modelName));
		
		WSBTM LDATest=new WSBTM(LDATrain, parameters);
//		WSBTM LDATest=new WSBTM(LDACfg.getModelFileName(modelName), parameters);
		LDATest.readCorpus(LDACfg.rtmTestCorpusFileName);
		LDATest.readGraph(LDACfg.rtmTestLinkFileName);
		LDATest.initialize();
		LDATest.sample(LDACfg.numTestIters);
	}
}
