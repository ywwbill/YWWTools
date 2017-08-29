package yang.weiwei.tlda;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import yang.weiwei.util.IOUtil;
import yang.weiwei.util.format.Fourmat;
import yang.weiwei.tlda.util.TLDATopicPriorNode;

public class TLDAParam
{
	//for topic model
	/** Parameter of document-topic distribution's Dirichlet prior (default: 0.01) */
	public double alpha=0.01;
	/** Parameter of each tree node's child distribution's Dirichlet prior (default: 0.01) */
	public double beta=0.01;
	/** Number of topics (default: 10) */
	public int numTopics=10;
	/** Print log on console (default: true) */
	public boolean verbose=true;
	
	/** Update alpha (default: false) */
	public boolean updateAlpha=false;
	/** Interval of updating alpha (default: 10) */
	public int updateAlphaInterval=10;
	
	/** Vocabulary list */
	public ArrayList<String> vocabList;
	/** Word to number map */
	public HashMap<String, Integer> vocabMap;
	/** Vocabulary size */
	public int numVocab;
	/** Prior tree structure (w/o weights) */
	public TLDATopicPriorNode topicPrior;
	
	/** The paths to words */
	public ArrayList<List<List<Integer>>> vocabPaths;
	/** Total number of leaf nodes */
	public int numLeafNodes;
	
	public void printBasicParam(String prefix)
	{
		IOUtil.println(prefix+"alpha: "+Fourmat.format(alpha));
		IOUtil.println(prefix+"beta: "+Fourmat.format(beta));
		IOUtil.println(prefix+"#topics: "+numTopics);
		IOUtil.println(prefix+"#vocab: "+numVocab);
		IOUtil.println(prefix+"verbose: "+verbose);
		IOUtil.println(prefix+"update alpha: "+updateAlpha);
		if (updateAlpha) IOUtil.println(prefix+"update alpha interval: "+updateAlphaInterval);
	}
	
	public TLDAParam(int numVocab)
	{
		vocabList=new ArrayList<String>();
		vocabMap=new HashMap<String, Integer>();
		this.numVocab=numVocab;
		for (int vocab=0; vocab<numVocab; vocab++)
		{
			vocabList.add(vocab+"");
			vocabMap.put(vocab+"", vocabMap.size());
		}
	}
	
	/**
	 * Initialize a parameter object with vocabulary file and tree prior file
	 * @param vocabFileName Vocabulary file name
	 * @param vocabPriorFileName Tree prior file name
	 * @throws IOException IOException
	 */
	public TLDAParam(String vocabFileName, String vocabPriorFileName) throws IOException
	{
		vocabList=new ArrayList<String>();
		vocabMap=new HashMap<String, Integer>();
		BufferedReader br=new BufferedReader(new FileReader(vocabFileName));
		String line;
		while ((line=br.readLine())!=null)
		{
			if (vocabMap.containsKey(line)) continue;
			vocabMap.put(line, vocabMap.size());
			vocabList.add(line);
		}
		br.close();
		numVocab=vocabList.size();
		
		if (vocabPriorFileName.length()>0)
		{
			topicPrior=TLDATopicPriorNode.fromPrettyPrint(vocabPriorFileName);
		}
		else
		{
			topicPrior=new TLDATopicPriorNode();
			for (int vocab=0; vocab<numVocab; vocab++)
			{
				topicPrior.addChild(new TLDATopicPriorNode(new TLDATopicPriorNode(vocabList.get(vocab), vocab)));
			}
		}
		
		vocabPaths=new ArrayList<List<List<Integer>>>();
		for (int vocab=0; vocab<numVocab; vocab++)
		{
			vocabPaths.add(new ArrayList<List<Integer>>());
		}
		topicPrior.getVocabPathMap(vocabPaths);
		numLeafNodes=topicPrior.getNumLeafNodes();
	}
	
	/**
	 * Get the number of paths leading to a word
	 * @param vocab The word ID
	 * @return The number of paths leading to the word
	 */
	public int getNumPaths(int vocab)
	{
		return vocabPaths.get(vocab).size();
	}
	
	/**
	 * Get a node given the word and the path number
	 * @param vocab The word
	 * @param path The path number
	 * @return The node which the path leads to
	 */
	public TLDATopicPriorNode getNode(int vocab, int path)
	{
		return topicPrior.getNode(vocabPaths.get(vocab).get(path));
	}
}
