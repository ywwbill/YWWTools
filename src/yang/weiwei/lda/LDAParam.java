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
	public double alphaSum=20.0;
	public double _alphaSum=2.0;
	public double beta=0.1;
	public int numTopics=20;
	public boolean verbose=true;
	
	public boolean updateAlpha=false;
	public int updateAlphaInterval=10;
	
	public ArrayList<String> vocabList;
	public HashMap<String, Integer> vocabMap;
	public int numVocab;
	
	//for hinge loss
	public double c=1.0;
	
	//for rtm
	public double nu=1.0;
	public int showPLRInterval=50;
	public boolean negEdge=true;
	public double negEdgeRatio=1.0;
	
	//for slda
	public double sigma=1.0;
	
	//for wsbm
	public boolean directed=false;
	public double a=1.0;
	public double b=1.0;
	public double gamma=1.0;
	public int numBlocks=20;
	
	public void printBasicParam(String prefix)
	{
		IOUtil.println(prefix+"alpha: "+Fourmat.format(alphaSum/numTopics));
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
		if (negEdge) IOUtil.println(prefix+"negative edge ratio: "+Fourmat.format(negEdgeRatio));
	}
	
	public void printSLDAParam(String prefix)
	{
		IOUtil.println(prefix+"nu: "+Fourmat.format(nu));
		IOUtil.println(prefix+"sigma: "+Fourmat.format(sigma));
	}
	
	public void printBlockParam(String prefix)
	{
		IOUtil.println(prefix+"alpha': "+Fourmat.format(_alphaSum/numTopics));
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
		alphaSum=param.alpha*param.numTopics;
		_alphaSum=param._alpha*param.numTopics;
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
