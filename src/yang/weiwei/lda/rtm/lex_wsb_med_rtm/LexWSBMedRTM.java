package yang.weiwei.lda.rtm.lex_wsb_med_rtm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import cc.mallet.optimize.LimitedMemoryBFGS;
import yang.weiwei.util.MathUtil;
import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.rtm.lex_wsb_rtm.LexWSBRTM;
import yang.weiwei.util.IOUtil;
import yang.weiwei.wsbm.WSBM;

/**
 * Lex-WSB-RTM with hinge loss
 * @author Weiwei Yang
 *
 */
public class LexWSBMedRTM extends LexWSBRTM
{
	protected ArrayList<HashMap<Integer, Double>> zeta;
	protected ArrayList<HashMap<Integer, Double>> lambda;
	
	public void readCorpus(String corpusFileName) throws IOException
	{
		super.readCorpus(corpusFileName);
		for (int doc=0; doc<numDocs; doc++)
		{
			zeta.add(new HashMap<Integer, Double>());
			lambda.add(new HashMap<Integer, Double>());
		}
	}
	
	public void readGraph(String graphFileName, int graphType) throws IOException
	{
		super.readGraph(graphFileName, graphType);
		if (graphType!=TRAIN_GRAPH) return;
		for (int doc=0; doc<numDocs; doc++)
		{
			for (int d : trainEdgeWeights.get(doc).keySet())
			{
				zeta.get(doc).put(d, 0.0);
				lambda.get(doc).put(d, 1.0);
			}
		}
	}
	
	protected void printParam()
	{
		super.printParam();
		param.printHingeParam("\t");
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
			trainEdgeWeights.get(u).put(v, -1);
		}
	}
	
	public void sample(int numIters)
	{
		for (int iteration=1; iteration<=numIters; iteration++)
		{
			if (type==TRAIN)
			{
				optimize();
			}
			
			for (int doc=0; doc<numDocs; doc++)
			{
				weight=new double[trainEdgeWeights.get(doc).size()];
				sampleBlock(doc);
				sampleDoc(doc);
				computeZeta(doc);
				sampleLambda(doc);
			}
			
			computeLogLikelihood();
			perplexity=Math.exp(-logLikelihood/numTestWords);
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
		
		if (type==TRAIN && param.verbose)
		{
			for (int topic=0; topic<param.numTopics; topic++)
			{
				IOUtil.println(topWordsByFreq(topic, 10));
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
		for (int d : trainEdgeWeights.get(doc).keySet())
		{
			double term1=(param.c*trainEdgeWeights.get(doc).get(d)*(param.c+lambda.get(doc).get(d))*
					eta[topic]*corpus.get(d).getTopicCount(topic))/
					(lambda.get(doc).get(d)*corpus.get(doc).docLength()*corpus.get(d).docLength());
			double term2=MathUtil.sqr(param.c)*(MathUtil.sqr(eta[topic]*corpus.get(d).getTopicCount(topic))+
					2.0*eta[topic]*corpus.get(d).getTopicCount(topic)*weight[i])/
					(2.0*lambda.get(doc).get(d)*MathUtil.sqr(corpus.get(doc).docLength())*MathUtil.sqr(corpus.get(d).docLength()));
			score+=term1-term2;
			i++;
		}
		return score;
	}
	
	protected void optimize()
	{
		LexWSBMedRTMFunction optimizable=new LexWSBMedRTMFunction(this);
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
	
	protected void sampleLambda(int doc)
	{
		for (int d : trainEdgeWeights.get(doc).keySet())
		{
			double newValue=MathUtil.sampleIG(1.0/(param.c*Math.abs(zeta.get(doc).get(d))), 1.0);
			lambda.get(doc).put(d, 1.0/newValue);
		}
	}
	
	protected void computeZeta(int doc)
	{
		for (int d : trainEdgeWeights.get(doc).keySet())
		{
			double w=computeWeight(doc, d);
			zeta.get(doc).put(d, 1.0-trainEdgeWeights.get(doc).get(d)*w);
		}
	}
	
	protected double computeEdgeProb(int doc1, int doc2)
	{
		return Math.exp(-2.0*param.c*Math.max(0.0, 1.0-computeWeight(doc1, doc2)));
	}
	
	protected void initVariables()
	{
		super.initVariables();
		zeta=new ArrayList<HashMap<Integer, Double>>();
		lambda=new ArrayList<HashMap<Integer, Double>>();
	}
	
	protected WSBM getWSBM()
	{
		return wsbm;
	}
	
	/**
	 * Initialize an Lex-WSB-Med-RTM object for training
	 * @param parameters Parameters
	 */
	public LexWSBMedRTM(LDAParam parameters)
	{
		super(parameters);
	}
	
	/**
	 * Initialize an Lex-WSB-Med-RTM object for test using a pre-trained Lex-WSB-Med-RTM object
	 * @param RTMTrain Pre-trained Lex-WSB-Med-RTM object
	 * @param parameters Parameters
	 */
	public LexWSBMedRTM(LexWSBMedRTM RTMTrain, LDAParam parameters)
	{
		super(RTMTrain, parameters);
	}
	
	/**
	 * Initialize an Lex-WSB-Med-RTM object for test using a pre-trained Lex-WSB-Med-RTM model in file
	 * @param modelFileName Model file name
	 * @param parameters Parameters
	 * @throws IOException IOException
	 */
	public LexWSBMedRTM(String modelFileName, LDAParam parameters) throws IOException
	{
		super(modelFileName, parameters);
	}
	
	public static void main(String args[]) throws IOException
	{
		LDAParam parameters=new LDAParam(LDACfg.rtmVocabFileName);
		parameters.updateAlpha=false;
		parameters.showPLRInterval=10;
		
		LexWSBMedRTM RTMTrain=new LexWSBMedRTM(parameters);
		RTMTrain.readCorpus(LDACfg.rtmTrainCorpusFileName);
		RTMTrain.readGraph(LDACfg.rtmTrainLinkFileName, TRAIN_GRAPH);
		RTMTrain.readGraph(LDACfg.rtmTrainLinkFileName, TEST_GRAPH);
		RTMTrain.readBlockGraph(LDACfg.rtmTrainLinkFileName);
		RTMTrain.initialize();
		RTMTrain.sample(LDACfg.numTrainIters);
//		RTMTrain.writeModel(LDACfg.getModelFileName(modelName));
		
		LexWSBMedRTM RTMTest=new LexWSBMedRTM(RTMTrain, parameters);
//		LexWSBMedRTM RTMTest=new LexWSBMedRTM(LDACfg.getModelFileName(modelName), parameters);
		RTMTest.readCorpus(LDACfg.rtmTestCorpusFileName);
		RTMTest.readGraph(LDACfg.rtmTestLinkFileName, TEST_GRAPH);
		RTMTest.initialize();
		RTMTest.sample(LDACfg.numTestIters);
//		RTMTest.writePred(LDACfg.rtmPredLinkFileName);
	}
}
