package yang.weiwei.lda.slda.bs_lda;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import cc.mallet.optimize.LimitedMemoryBFGS;
import yang.weiwei.lda.LDACfg;
import yang.weiwei.lda.LDAParam;
import yang.weiwei.lda.slda.SLDA;
import yang.weiwei.util.IOUtil;
import yang.weiwei.util.MathUtil;

/**
 * Binary SLDA
 * @author Weiwei Yang
 *
 */
public class BSLDA extends SLDA
{
	protected int labels[];
	protected int predLabels[];
	protected double accuracy;
	
	public void readCorpus(String corpusFileName) throws IOException
	{
		super.readCorpus(corpusFileName);
		labels=new int[numDocs];
		predLabels=new int[numDocs];
	}
	
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
				labels[doc]=Integer.valueOf(line);
				labelStatuses[doc]=true;
				numLabels++;
			}
		}
		br.close();
	}
	
	protected void printMetrics()
	{
		super.printMetrics();
		IOUtil.println("Accuracy: "+format(accuracy));
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
				IOUtil.println("<"+iteration+">"+"\tLog-LLD: "+format(logLikelihood)+
						"\tPPX: "+format(perplexity));
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
			
			newTopic=sampleTopic(doc, token, oldTopic);
			
			assignTopic(doc, token, newTopic);
			if (type==TRAIN && labelStatuses[doc])
			{
				weight+=eta[newTopic]/corpus.get(doc).docLength();
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
			double temp=MathUtil.sigmoid(weight+eta[topic]/corpus.get(doc).docLength());
			score*=(labels[doc]>0? temp : 1.0-temp);
		}
		
		return score;
	}
	
	protected double computeWeight(int doc)
	{
		double weight=0.0;
		for (int topic=0; topic<param.numTopics; topic++)
		{
			weight+=eta[topic]*corpus.get(doc).getTopicCount(topic)/corpus.get(doc).docLength();
		}
		return weight;
	}
	
	protected void optimize()
	{
		BSLDAFunction optimizable=new BSLDAFunction(this);
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
			error+=MathUtil.sqr(labels[doc]-computeDocLabelProb(doc));
		}
		error=Math.sqrt(error/(double)numLabels);
	}
	
	protected double computeDocLabelProb(int doc)
	{
		return MathUtil.sigmoid(computeWeight(doc));
	}
	
	protected void computePredLabels()
	{
		for (int doc=0; doc<numDocs; doc++)
		{
			predLabels[doc]=(computeDocLabelProb(doc)>0.5? 1 : 0);
		}
	}
	
	protected void computeAccuracy()
	{
		accuracy=0.0;
		if (numLabels==0) return;
		computePredLabels();
		int correctCount=0;
		for (int doc=0; doc<numDocs; doc++)
		{
			if (labelStatuses[doc] && labels[doc]==predLabels[doc])
			{
				correctCount++;
			}
		}
		accuracy=(double)correctCount/(double)numLabels;
	}
	
	/**
	 * Get accuracy
	 * @return Accuracy
	 */
	public double getAccuracy()
	{
		return accuracy;
	}
	
	public void writePredLabels(String predLabelFileName) throws IOException
	{
		computePredLabels();
		BufferedWriter bw=new BufferedWriter(new FileWriter(predLabelFileName));
		IOUtil.writeVector(bw, predLabels);
		bw.close();
	}
	
	/**
	 * Get label of a document
	 * @param doc Document number
	 * @return Given document's label
	 */
	public int getLabel(int doc)
	{
		return labels[doc];
	}
	
	/**
	 * Initialize an BS-LDA object for training
	 * @param parameters Parameters
	 */
	public BSLDA(LDAParam parameters)
	{
		super(parameters);
	}
	
	/**
	 * Initialize an BS-LDA object for test using a pre-trained BS-LDA object
	 * @param LDATrain Pre-trained BS-LDA object
	 * @param parameters Parameters
	 */
	public BSLDA(BSLDA LDATrain, LDAParam parameters)
	{
		super(LDATrain, parameters);
	}
	
	/**
	 * Initialize an BS-LDA object for test using a pre-trained BS-LDA model in file
	 * @param modelFileName Model file name
	 * @param parameters Parameters
	 * @throws IOException IOException
	 */
	public BSLDA(String modelFileName, LDAParam parameters) throws IOException
	{
		super(modelFileName, parameters);
	}
	
	public static void main(String args[]) throws IOException
	{
		LDAParam parameters=new LDAParam(LDACfg.sldaVocabFileName);
		
		BSLDA LDATrain=new BSLDA(parameters);
		LDATrain.readCorpus(LDACfg.sldaTrainCorpusFileName);
		LDATrain.readLabels(LDACfg.sldaTrainLabelFileName);
		LDATrain.initialize();
		LDATrain.sample(LDACfg.numTrainIters);
//		LDATrain.writeModel(LDACfg.getModelFileName(modelName));
		
		BSLDA LDATest=new BSLDA(LDATrain, parameters);
//		BSLDA LDATest=new BSLDA(LDACfg.getModelFileName(modelName), parameters);
		LDATest.readCorpus(LDACfg.sldaTestCorpusFileName);
		LDATest.readLabels(LDACfg.sldaTestLabelFileName);
		LDATest.initialize();
		LDATest.sample(LDACfg.numTestIters);
//		LDATest.writePredLabels(LDACfg.sldaPredLabelFileName);
	}
}
