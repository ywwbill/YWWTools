package yang.weiwei.lda.slda.lex_wsb_med_lda;

import java.io.IOException;

import cc.mallet.optimize.LimitedMemoryBFGS;
import yang.weiwei.util.MathUtil;
import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.slda.lex_wsb_bs_lda.LexWSBBSLDA;
import yang.weiwei.util.IOUtil;

/**
 * Lex-WSB-BS-LDA with hinge loss
 * @author Weiwei Yang
 *
 */
public class LexWSBMedLDA extends LexWSBBSLDA
{
	protected double zeta[];
	protected double lambda[];
	
	public void readCorpus(String corpusFileName) throws IOException
	{
		super.readCorpus(corpusFileName);
		zeta=new double[numDocs];
		lambda=new double[numDocs];
		for (int doc=0; doc<numDocs; doc++)
		{
			zeta[doc]=0.0;
			lambda[doc]=1.0;
		}
	}
	
	public void readLabels(String labelFileName) throws IOException
	{
		super.readLabels(labelFileName);
		for (int doc=0; doc<numDocs; doc++)
		{
			if (labels[doc]==0)
			{
				labels[doc]=-1;
			}
		}
	}
	
	protected void printParam()
	{
		super.printParam();
		param.printHingeParam("\t");
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
			
			computeError();
			if (param.verbose && numLabels>0)
			{
				IOUtil.print("\tError: "+format(error));
			}
			
			computeAccuracy();
			if (param.verbose && numLabels>0)
			{
				IOUtil.print("\tAccuracy: "+format(accuracy));
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
			double term1=param.c*labels[doc]*(param.c+lambda[doc])*eta[topic]/(lambda[doc]*corpus.get(doc).docLength());
			double term2=MathUtil.sqr(param.c)*(MathUtil.sqr(eta[topic])+2.0*eta[topic]*weight)/
					(2.0*lambda[doc]*MathUtil.sqr(corpus.get(doc).docLength()));
			score*=Math.exp(term1-term2);
		}
		
		return score;
	}
	
	protected void optimize()
	{
		LexWSBMedLDAFunction optimizable=new LexWSBMedLDAFunction(this);
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
	
	protected void sampleLambda(int doc)
	{
		if (!labelStatuses[doc]) return;
		double newValue=MathUtil.sampleIG(1.0/(param.c*Math.abs(zeta[doc])), 1.0);
		lambda[doc]=1.0/newValue;
	}
	
	protected void computeZeta(int doc)
	{
		if (!labelStatuses[doc]) return;
		zeta[doc]=1.0-labels[doc]*computeWeight(doc);
	}
	
	protected double computeDocLabelProb(int doc)
	{
		return Math.exp(-2.0*param.c*Math.max(0.0, 1.0-labels[doc]*computeWeight(doc)));
	}
	
	protected void computeError()
	{
		error=0.0;
		if (numLabels==0) return;
		for (int doc=0; doc<numDocs; doc++)
		{
			if (!labelStatuses[doc]) continue;
			error+=MathUtil.sqr(1.0-computeDocLabelProb(doc));
		}
		error=Math.sqrt(error/(double)numLabels);
	}
	
	protected void computePredLabels()
	{
		for (int doc=0; doc<numDocs; doc++)
		{
			predLabels[doc]=(computeWeight(doc)>0.0? 1 : -1);
		}
	}
	
	/**
	 * Initialize an Lex-WSB-Med-LDA object for training
	 * @param parameters Parameters
	 */
	public LexWSBMedLDA(LDAParam parameters)
	{
		super(parameters);
	}
	
	/**
	 * Initialize an Lex-WSB-Med-LDA object for test using a pre-trained Lex-WSB-Med-LDA object
	 * @param LDATrain Pre-trained Lex-WSB-Med-LDA object
	 * @param parameters Parameters
	 */
	public LexWSBMedLDA(LexWSBMedLDA LDATrain, LDAParam parameters)
	{
		super(LDATrain, parameters);
	}
	
	/**
	 * Initialize an Lex-WSB-Med-LDA object for test using a pre-trained Lex-WSB-Med-LDA model in file
	 * @param modelFileName Model file name
	 * @param parameters Parameters
	 * @throws IOException IOException
	 */
	public LexWSBMedLDA(String modelFileName, LDAParam parameters) throws IOException
	{
		super(modelFileName, parameters);
	}
	
	public static void main(String args[]) throws IOException
	{
		LDAParam parameters=new LDAParam(LDACfg.sldaVocabFileName);
		
		LexWSBMedLDA LDATrain=new LexWSBMedLDA(parameters);
		LDATrain.readCorpus(LDACfg.sldaTrainCorpusFileName);
		LDATrain.readLabels(LDACfg.sldaTrainLabelFileName);
//		LDATrain.readBlockGraph(LDACfg.sldaTrainGraphFileName);
		LDATrain.initialize();
		LDATrain.sample(LDACfg.numTrainIters);
//		LDATrain.writeModel(LDACfg.getModelFileName(modelName));
		
		LexWSBMedLDA LDATest=new LexWSBMedLDA(LDATrain, parameters);
//		LexWSBMedLDA LDATest=new LexWSBMedLDA(LDACfg.getModelFileName(modelName), parameters);
		LDATest.readCorpus(LDACfg.sldaTestCorpusFileName);
		LDATest.readLabels(LDACfg.sldaTestLabelFileName);
//		LDATest.readBlockGraph(LDACfg.sldaTestGraphFileName);
		LDATest.initialize();
		LDATest.sample(LDACfg.numTestIters);
//		LDATest.writePredLabels(LDACfg.sldaPredLabelFileName);
	}
}
