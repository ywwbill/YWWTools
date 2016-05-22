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
import yang.weiwei.lda.util.LDAResult;
import yang.weiwei.util.IOUtil;
import cc.mallet.optimize.LimitedMemoryBFGS;
import com.google.gson.annotations.Expose;

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
			
			computeError();
			computeAvgWeight();
			if (iteration%param.showPLRInterval==0 || iteration==numIters) computePLR();
			
			if (param.verbose)
			{
				IOUtil.println("<"+iteration+">"+"\tLog-LLD: "+format(logLikelihood)+"\tPPX: "+format(perplexity)+
						"\tAvg Weight: "+format(avgWeight)+"\tError: "+format(error)+"\tPLR: "+format(PLR));
			}
			
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
	
	public void addResults(LDAResult result)
	{
		super.addResults(result);
		result.add(LDAResult.PLR, PLR);
		result.add(LDAResult.ERROR, error);
	}
	
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
	
	public void writePred(String predFileName) throws IOException
	{
		double probs[][]=new double[numDocs][numDocs];
		for (int doc1=0; doc1<numDocs; doc1++)
		{
			for (int doc2=doc1+1; doc2<numDocs; doc2++)
			{
				probs[doc1][doc2]=computeEdgeProb(doc1, doc2);
				probs[doc2][doc1]=probs[doc1][doc2];
			}
		}
		BufferedWriter bw=new BufferedWriter(new FileWriter(predFileName));
		IOUtil.writeMatrix(bw, probs);
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
	
	public double getPLR()
	{
		return PLR;
	}
	
	public double getTopicWeight(int topic)
	{
		return eta[topic];
	}
	
	public double[] getTopicWeights()
	{
		return eta;
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
		for (int topic=0; topic<param.numTopics; topic++)
		{
			eta[topic]=((RTM)LDAModel).eta[topic];
		}
	}
	
	public RTM(LDAParam parameters)
	{
		super(parameters);
		for (int topic=0; topic<param.numTopics; topic++)
		{
			eta[topic]=randoms.nextGaussian(0.0, MathUtil.sqr(param.nu));
		}
	}
	
	public RTM(RTM RTMTrain, LDAParam parameters)
	{
		super(RTMTrain, parameters);
	}
	
	public RTM(String modelFileName, LDAParam parameters) throws IOException
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
		
		RTM RTMTrain=new RTM(parameters);
		RTMTrain.readCorpus(LDACfg.trainCorpusFileName);
		RTMTrain.readGraph(LDACfg.trainGraphFileName, TRAIN_GRAPH);
		RTMTrain.readGraph(LDACfg.trainGraphFileName, TEST_GRAPH);
		RTMTrain.initialize();
		RTMTrain.sample(LDACfg.numTrainIters);
		RTMTrain.addResults(trainResults);
//		RTMTrain.writeModel(LDACfg.getModelFileName(modelName));
		
		RTM RTMTest=new RTM(RTMTrain, parameters);
//		RTM RTMTest=new RTM(LDACfg.getModelFileName(modelName), parameters);
		RTMTest.readCorpus(LDACfg.testCorpusFileName);
		RTMTest.readGraph(LDACfg.testGraphFileName, TEST_GRAPH);
		RTMTest.initialize();
		RTMTest.sample(LDACfg.numTestIters);
		RTMTest.addResults(testResults);
		
		trainResults.printResults(modelName+" Training PLR: ", LDAResult.PLR);
		testResults.printResults(modelName+" Test PLR: ", LDAResult.PLR);
	}
}
