package yang.weiwei.lda.slda.lex_wsb_bs_lda;

import java.io.IOException;

import com.google.gson.annotations.Expose;
import cc.mallet.optimize.LimitedMemoryBFGS;
import yang.weiwei.util.MathUtil;
import yang.weiwei.wsbm.WSBM;
import yang.weiwei.wsbm.WSBMParam;
import yang.weiwei.lda.LDA;
import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.slda.bs_lda.BSLDA;
import yang.weiwei.lda.util.LDAResult;
import yang.weiwei.util.IOUtil;

/**
 * BS-LDA with lexical weights and weighted stochastic block priors
 * @author Weiwei Yang
 *
 */
public class LexWSBBSLDA extends BSLDA
{
	@Expose protected double tau[];
	
	protected int blockTopicCounts[][];
	protected int blockTokenCounts[];
	protected double pi[][];
	
	protected WSBM wsbm;
	
	public void readCorpus(String corpusFileName) throws IOException
	{
		super.readCorpus(corpusFileName);
		WSBMParam wsbmParam=new WSBMParam(param, numDocs);
		wsbm=new WSBM(wsbmParam);
	}
	
	/**
	 * Read document links for WSBM
	 * @param blockGraphFileName Document link file name
	 * @throws IOException IOException
	 */
	public void readBlockGraph(String blockGraphFileName) throws IOException
	{
		wsbm.readGraph(blockGraphFileName);
		wsbm.init();
	}
	
	protected void printParam()
	{
		super.printParam();
		param.printBlockParam("\t");
		wsbm.param.printParam("\t");
	}
	
	public void initialize()
	{
		super.initialize();
		initBlockAssigns();
	}
	
	public void initialize(String topicAssignFileName) throws IOException
	{
		super.initialize(topicAssignFileName);
		initBlockAssigns();
	}
	
	protected void initBlockAssigns()
	{
		if (wsbm.getNumEdges()==0) return;
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
			
			if (type==TRAIN)
			{
				optimize();
				computeError();
				computeAccuracy();
			}
			if (param.verbose)
			{
				IOUtil.println("<"+iteration+">"+"\tLog-LLD: "+format(logLikelihood)+"\tPPX: "+format(perplexity)+
						"\tError: "+format(error)+"\tBlock Log-LLD: "+format(wsbm.getLogLikelihood())+"\tAccuracy: "+accuracy);
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
		
		printMetrics();
	}
	
	protected void sampleBlock(int doc)
	{
		if (wsbm.getNumEdges()==0) return;
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
		weight=computeWeight(doc);
		for (int token=0; token<corpus.get(doc).docLength(); token+=interval)
		{			
			oldTopic=unassignTopic(doc, token);
			if (type==TRAIN && labelStatuses[doc])
			{
				weight-=eta[oldTopic]/corpus.get(doc).docLength();
			}
			if (wsbm.getNumEdges()>0)
			{
				blockTopicCounts[wsbm.getBlockAssign(doc)][oldTopic]--;
				blockTokenCounts[wsbm.getBlockAssign(doc)]--;
			}
			
			newTopic=sampleTopic(doc, token, oldTopic);
			
			assignTopic(doc, token, newTopic);
			if (type==TRAIN && labelStatuses[doc])
			{
				weight+=eta[newTopic]/corpus.get(doc).docLength();
			}
			if (wsbm.getNumEdges()>0)
			{
				blockTopicCounts[wsbm.getBlockAssign(doc)][newTopic]++;
				blockTokenCounts[wsbm.getBlockAssign(doc)]++;
			}
		}
	}
	
	protected double topicUpdating(int doc, int topic, int vocab)
	{
		double score=0.0;
		double ratio=(blockTopicCounts[wsbm.getBlockAssign(doc)][topic]+param._alpha)/
				(blockTokenCounts[wsbm.getBlockAssign(doc)]+param._alpha*param.numTopics);
		if (wsbm.getNumEdges()==0) ratio=1.0/param.numTopics;
		if (type==TRAIN)
		{
			score=(param.alpha*param.numTopics*ratio+corpus.get(doc).getTopicCount(topic))*
					(param.beta+topics[topic].getVocabCount(vocab))/
					(param.beta*param.numVocab+topics[topic].getTotalTokens());
		}
		else
		{
			score=(param.alpha*param.numTopics*ratio+corpus.get(doc).getTopicCount(topic))*phi[topic][vocab];
		}
		
		if (type==TRAIN && labelStatuses[doc])
		{
			double temp=MathUtil.sigmoid(weight+eta[topic]/corpus.get(doc).docLength());
			score*=(labels[doc]>0? temp : 1.0-temp);
		}
		
		return score;
	}
	
	protected void optimize()
	{
		LexWSBBSLDAFunction optimizable=new LexWSBBSLDAFunction(this);
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
	}
	
	protected double computeWeight(int doc)
	{
		double weight=super.computeWeight(doc);
		for (int token : corpus.get(doc).getWordSet())
		{
			weight+=tau[token]*corpus.get(doc).getWordCount(token)/corpus.get(doc).docLength();
		}
		return weight;
	}
	
	protected void computeLogLikelihood()
	{
		super.computeLogLikelihood();
		if (wsbm.getNumEdges()>0) wsbm.computeLogLikelihood();
	}
	
	protected void computePi()
	{
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
		wsbm.writeBlocks(blockFileName);
	}
	
	/**
	 * Print blocks on console
	 */
	public void printBlocks()
	{
		wsbm.printResults();
	}
	
	public void addResults(LDAResult result)
	{
		super.addResults(result);
		result.add(LDAResult.BLOCKLOGLIKELIHOOD, wsbm.getLogLikelihood());
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
		return tau;
	}
	
	public double getBlockEdgeRate(int block1, int block2)
	{
		return wsbm.getBlockEdgeRate(block1, block2);
	}
	
	public double[][] getBlockEdgeRates()
	{
		return wsbm.getBlockEdgeRates();
	}
	
	/**
	 * Get block distribution
	 * @return Block distribution
	 */
	public double[] getBlockDist()
	{
		return wsbm.getBlockDist();
	}
	
	/**
	 * Get block assignment of the given document
	 * @param doc Document number
	 * @return Given document's block assignment
	 */
	public int getBlockAssign(int doc)
	{
		return wsbm.getBlockAssign(doc);
	}
	
	
	/**
	 * Get block distribution over topics
	 * @return Block distribution over topics
	 */
	public double[][] getBlockTopicDist()
	{
		return pi;
	}
	
	protected WSBM getWSBM()
	{
		return wsbm;
	}
	
	protected void initVariables()
	{
		super.initVariables();
		blockTopicCounts=new int[param.numBlocks][param.numTopics];
		blockTokenCounts=new int[param.numBlocks];
		pi=new double[param.numBlocks][param.numTopics];
		tau=new double[param.numVocab];
	}
	
	public void copyModel(LDA LDAModel)
	{
		super.copyModel(LDAModel);
		for (int vocab=0; vocab<param.numVocab; vocab++)
		{
			tau[vocab]=((LexWSBBSLDA)LDAModel).tau[vocab];
		}
	}
	
	/**
	 * Initialize an Lex-WSB-BS-LDA object for training
	 * @param parameters Parameters
	 */
	public LexWSBBSLDA(LDAParam parameters)
	{
		super(parameters);
		for (int vocab=0; vocab<param.numVocab; vocab++)
		{
			tau[vocab]=randoms.nextGaussian(0.0, MathUtil.sqr(param.nu));
		}
	}
	
	/**
	 * Initialize an Lex-WSB-BS-LDA object for test using a pre-trained Lex-WSB-BS-LDA object
	 * @param LDATrain Pre-trained Lex-WSB-BS-LDA object
	 * @param parameters Parameters
	 */
	public LexWSBBSLDA(LexWSBBSLDA LDATrain, LDAParam parameters)
	{
		super(LDATrain, parameters);
	}
	
	/**
	 * Initialize an Lex-WSB-BS-LDA object for test using a pre-trained Lex-WSB-BS-LDA model in file
	 * @param modelFileName Model file name
	 * @param parameters Parameters
	 * @throws IOException IOException
	 */
	public LexWSBBSLDA(String modelFileName, LDAParam parameters) throws IOException
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
		
		LexWSBBSLDA LDATrain=new LexWSBBSLDA(parameters);
		LDATrain.readCorpus(LDACfg.trainCorpusFileName);
		LDATrain.readLabels(LDACfg.trainLabelFileName);
		LDATrain.readBlockGraph(LDACfg.trainGraphFileName);
		LDATrain.initialize();
		LDATrain.sample(LDACfg.numTrainIters);
		LDATrain.addResults(trainResults);
//		LDATrain.writeModel(LDACfg.getModelFileName(modelName));
		
		LexWSBBSLDA LDATest=new LexWSBBSLDA(LDATrain, parameters);
//		LexWSBBSLDA LDATest=new LexWSBBSLDA(LDACfg.getModelFileName(modelName), parameters);
		LDATest.readCorpus(LDACfg.testCorpusFileName);
		LDATest.readBlockGraph(LDACfg.testGraphFileName);
		LDATest.initialize();
		LDATest.sample(LDACfg.numTestIters);
		LDATest.addResults(testResults);
		LDATest.writePredLabels(LDACfg.predLabelFileName);
		
		trainResults.printResults(modelName+" Training Error: ", LDAResult.ERROR);
		testResults.printResults(modelName+" Test Error: ", LDAResult.ERROR);
	}
}
