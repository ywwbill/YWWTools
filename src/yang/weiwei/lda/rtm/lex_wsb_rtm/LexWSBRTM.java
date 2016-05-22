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
import yang.weiwei.lda.util.LDAResult;
import yang.weiwei.util.IOUtil;

import com.google.gson.annotations.Expose;

public class LexWSBRTM extends RTM
{
	protected double _alpha;
	
	@Expose protected double rho[][];
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
			computeError();
			computeAvgWeight();
			if (iteration%param.showPLRInterval==0 || iteration==numIters) computePLR();
			
			if (param.verbose)
			{
				IOUtil.println("<"+iteration+">"+"\tLog-LLD: "+format(logLikelihood)+"\tPPX: "+format(perplexity)+
						"\tBlock Log-LLD: "+format(wsbm.getLogLikelihood())+"\n\tAvg Weight: "+format(avgWeight)+
						"\tError: "+format(error)+"\tPLR: "+format(PLR));
			}
		}
		
		if (type==TRAIN)
		{
			for (int topic=0; topic<param.numTopics; topic++)
			{
				IOUtil.println(topWordsByFreq(topic, 10));
			}
		}
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
			if (wsbm.getNumEdges()>0)
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
			if (wsbm.getNumEdges()>0)
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
		double ratio=(blockTopicCounts[wsbm.getBlockAssign(doc)][topic]+_alpha)/
				(blockTokenCounts[wsbm.getBlockAssign(doc)]+_alpha*param.numTopics);
		if (wsbm.getNumEdges()==0) ratio=1.0/param.numTopics;
		if (type==TRAIN)
		{
			score=(param.alphaSum*ratio+corpus.get(doc).getTopicCount(topic))*
					(param.beta+topics[topic].getVocabCount(vocab))/
					(param.beta*param.numVocab+topics[topic].getTotalTokens());
		}
		else
		{
			score=(param.alphaSum*ratio+corpus.get(doc).getTopicCount(topic))*phi[topic][vocab];
		}
		int i=0;
		double temp;
		for (int d : trainEdgeWeights.get(doc).keySet())
		{
			temp=MathUtil.sigmoid(weight[i]+eta[topic]/corpus.get(doc).docLength()*
					corpus.get(d).getTopicCount(topic)/corpus.get(d).docLength());
			score*=(trainEdgeWeights.get(doc).get(d)>0? temp : 1.0-temp);
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
		for (int b1=0; b1<param.numBlocks; b1++)
		{
			for (int b2=0; b2<param.numBlocks; b2++)
			{
				int pos=b1*param.numBlocks+b2+param.numTopics+param.numVocab;
				rho[b1][b2]=optimizable.getParameter(pos);
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
		int b1=wsbm.getBlockAssign(doc1),b2=wsbm.getBlockAssign(doc2);
		weight+=rho[b1][b2]*wsbm.getBlockEdgeRate(b1, b2);
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
				pi[l][topic]=(blockTopicCounts[l][topic]+_alpha)/(blockTokenCounts[l]+_alpha*param.numTopics);
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
				theta[doc][topic]=(param.alphaSum*pi[wsbm.getBlockAssign(doc)][topic]+corpus.get(doc).getTopicCount(topic))/
						(param.alphaSum+getSampleSize(corpus.get(doc).docLength()));
			}
		}
	}
	
	public void writeBlocks(String blockFileName) throws IOException
	{
		wsbm.writeBlocks(blockFileName);
	}
	
	public void printBlocks()
	{
		wsbm.printResults();
	}
	
	public void addResults(LDAResult result)
	{
		super.addResults(result);
		result.add(LDAResult.BLOCKLOGLIKELIHOOD, wsbm.getLogLikelihood());
	}
	
	public double getLexWeight(int vocab)
	{
		return tau[vocab];
	}
	
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
	
	public double[] getBlockDist()
	{
		return wsbm.getBlockDist();
	}
	
	public int getBlockAssign(int doc)
	{
		return wsbm.getBlockAssign(doc);
	}
	
	public double[][] getBlockTopicDist()
	{
		return pi;
	}
	
	public double getBlockWeight(int b1, int b2)
	{
		return rho[b1][b2];
	}
	
	public double[][] getBlockWeights()
	{
		return rho;
	}
	
	protected WSBM getWSBM()
	{
		return wsbm;
	}
	
	protected void initVariables()
	{
		super.initVariables();
		_alpha=param._alphaSum/param.numTopics;
		blockTopicCounts=new int[param.numBlocks][param.numTopics];
		blockTokenCounts=new int[param.numBlocks];
		tau=new double[param.numVocab];
		rho=new double[param.numBlocks][param.numBlocks];
		pi=new double[param.numBlocks][param.numTopics];
	}
	
	protected void copyModel(LDA LDAModel)
	{
		super.copyModel(LDAModel);
		for (int vocab=0; vocab<param.numVocab; vocab++)
		{
			tau[vocab]=((LexWSBRTM)LDAModel).tau[vocab];
		}
		for (int b1=0; b1<param.numBlocks; b1++)
		{
			for (int b2=0; b2<param.numBlocks; b2++)
			{
				rho[b1][b2]=((LexWSBRTM)LDAModel).rho[b1][b2];
			}
		}
	}
	
	public LexWSBRTM(LDAParam parameters)
	{
		super(parameters);
		for (int vocab=0; vocab<param.numVocab; vocab++)
		{
			tau[vocab]=randoms.nextGaussian(0.0, MathUtil.sqr(param.nu));
		}
		for (int b1=0; b1<param.numBlocks; b1++)
		{
			for (int b2=0; b2<param.numBlocks; b2++)
			{
				rho[b1][b2]=randoms.nextGaussian(0.0, MathUtil.sqr(param.nu));
			}
		}
	}
	
	public LexWSBRTM(LexWSBRTM RTMTrain, LDAParam parameters)
	{
		super(RTMTrain, parameters);
	}
	
	public LexWSBRTM(String modelFileName, LDAParam parameters) throws IOException
	{
		super(modelFileName, parameters);
	}
	
	public static void main(String args[]) throws IOException
	{
		String seg[]=Thread.currentThread().getStackTrace()[1].getClassName().split("\\.");
		String modelName=seg[seg.length-1];
		LDAParam parameters=new LDAParam(LDACfg.vocabFileName);
		parameters.updateAlpha=false;
		LDAResult trainResult=new LDAResult();
		LDAResult testResult=new LDAResult();
		
		LexWSBRTM RTMTrain=new LexWSBRTM(parameters);
		RTMTrain.readCorpus(LDACfg.trainCorpusFileName);
		RTMTrain.readGraph(LDACfg.trainGraphFileName, TRAIN_GRAPH);
		RTMTrain.readGraph(LDACfg.trainGraphFileName, TEST_GRAPH);
		RTMTrain.readBlockGraph(LDACfg.trainGraphFileName);
		RTMTrain.initialize();
		RTMTrain.sample(LDACfg.numTrainIters);
		RTMTrain.addResults(trainResult);
//		RTMTrain.writeModel(LDACfg.getModelFileName(modelName));
		
		LexWSBRTM RTMTest=new LexWSBRTM(RTMTrain, parameters);
//		LexWSBRTM RTMTest=new LexWSBRTM(LDACfg.getModelFileName(modelName), parameters);
		RTMTest.readCorpus(LDACfg.testCorpusFileName);
		RTMTest.readGraph(LDACfg.testGraphFileName, TEST_GRAPH);
		RTMTest.initialize();
		RTMTest.sample(LDACfg.numTestIters);
		RTMTest.addResults(testResult);
		
		trainResult.printResults(modelName+" Training PLR: ", LDAResult.PLR);
		testResult.printResults(modelName+" Test PLR: ", LDAResult.PLR);
	}
}
