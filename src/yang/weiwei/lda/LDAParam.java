package yang.weiwei.lda;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import yang.weiwei.util.IOUtil;
import yang.weiwei.util.format.Fourmat;

public class LDAParam
{
	//for topic model
	/** Parameter of document-topic distribution's Dirichlet prior (default: 1.0) */
	public double alpha=1.0;
	/** parameter of block-topic distribution's Dirichlet prior (default: 0.1) */
	public double _alpha=0.1;
	/** Parameter of topic-word distribution's Dirichlet prior (default: 0.1) */
	public double beta=0.1;
	/** Number of topics (default: 20) */
	public int numTopics=20;
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
	
	//for hinge loss
	/** Regularization parameter for hinge loss (default: 1.0) */
	public double c=1.0;
	
	//for rtm
	/** Variance of downstream models' weights' Gaussian prior (default: 1.0) */
	public double nu=1.0;
	/** Interval of computing predictive link rank in RTM (default: 50) */
	public int showPLRInterval=50;
	/** Sample negative link in RTM (default: true) */
	public boolean negEdge=true;
	/** Ratio of number of negative links to positive links in RTM (default: 1.0) */
	public double negEdgeRatio=1.0;
	/** Include block feature in link prediction (default: false) */
	public boolean blockFeat=false;
	
	//for slda
	/** Variance of Gaussian distribution when generating SLDA's response variable (default: 1.0) */
	public double sigma=1.0;
	
	//for wsbm
	/** Set links directed (default: false) */
	public boolean directed=false;
	/** Parameter of Gamma prior in WSBM (default 1.0) */
	public double a=1.0;
	/** Parameter of Gamma prior in WSBM (default 1.0) */
	public double b=1.0;
	/** Parameter of block distribution's Dirichlet prior  in WSBM (default 1.0) */
	public double gamma=1.0;
	/** Number of blocks in WSBM (default: 20) */
	public int numBlocks=20;
	
	public void printBasicParam(String prefix)
	{
		IOUtil.println(prefix+"alpha: "+Fourmat.format(alpha));
		IOUtil.println(prefix+"beta: "+Fourmat.format(beta));
		IOUtil.println(prefix+"#topics: "+numTopics);
		IOUtil.println(prefix+"#vocab: "+numVocab);
		IOUtil.println(prefix+"LDA verbose: "+verbose);
		IOUtil.println(prefix+"update alpha: "+updateAlpha);
		if (updateAlpha) IOUtil.println(prefix+"update alpha interval: "+updateAlphaInterval);
	}
	
	public void printRTMParam(String prefix)
	{
		IOUtil.println(prefix+"directed: "+directed);
		IOUtil.println(prefix+"nu: "+Fourmat.format(nu));
		IOUtil.println(prefix+"PLR interval: " +showPLRInterval);
		IOUtil.println(prefix+"negative edge: "+negEdge);
		IOUtil.println(prefix+"block feature: "+blockFeat);
		if (negEdge) IOUtil.println(prefix+"negative edge ratio: "+Fourmat.format(negEdgeRatio));
	}
	
	public void printSLDAParam(String prefix)
	{
		IOUtil.println(prefix+"nu: "+Fourmat.format(nu));
		IOUtil.println(prefix+"sigma: "+Fourmat.format(sigma));
	}
	
	public void printBlockParam(String prefix)
	{
		IOUtil.println(prefix+"alpha': "+Fourmat.format(_alpha));
	}
	
	public void printHingeParam(String prefix)
	{
		IOUtil.println(prefix+"c: "+Fourmat.format(c));
	}
	
	public LDAParam(int numVocab)
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
	 * Initialize a parameter object with vocabulary file
	 * @param vocabFileName Vocabulary file name
	 * @throws IOException IOException
	 */
	public LDAParam(String vocabFileName) throws IOException
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
	}
	
	public LDAParam(LDASynParam param)
	{
		alpha=param.alpha;
		_alpha=param._alpha;
		beta=param.beta;
		numTopics=param.numTopics;
		numVocab=param.numVocab;
		vocabList=new ArrayList<String>();
		vocabMap=new HashMap<String, Integer>();
		for (int i=0; i<numVocab; i++)
		{
			vocabList.add(i+"");
			vocabMap.put(i+"", i);
		}
		
		nu=param.nu;
		negEdge=param.negEdge;
		
		c=param.c;
		
		sigma=param.sigma;
		
		directed=param.directed;
		a=param.a;
		b=param.b;
		gamma=param.gamma;
		numBlocks=param.numBlocks;
	}
}
