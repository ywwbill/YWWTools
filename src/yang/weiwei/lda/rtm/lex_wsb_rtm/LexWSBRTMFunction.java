package yang.weiwei.lda.rtm.lex_wsb_rtm;

import cc.mallet.optimize.Optimizable.ByGradientValue;
import yang.weiwei.util.MathUtil;

public class LexWSBRTMFunction implements ByGradientValue
{
	private double parameters[];
	private LexWSBRTM rtm;
	
	public LexWSBRTMFunction(LexWSBRTM RTMInst)
	{
		this.rtm=RTMInst;
		if (rtm.param.blockFeat && rtm.getWSBM()!=null)
		{
			parameters=new double[rtm.param.numTopics+rtm.param.numVocab+rtm.param.numBlocks*rtm.param.numBlocks];
		}
		else
		{
			parameters=new double[rtm.param.numTopics+rtm.param.numVocab];
		}
		for (int topic=0; topic<rtm.param.numTopics; topic++)
		{
			parameters[topic]=rtm.getTopicWeight(topic);
		}
		for (int vocab=0; vocab<rtm.param.numVocab; vocab++)
		{
			parameters[vocab+rtm.param.numTopics]=rtm.tau[vocab];
		}
		if (rtm.param.blockFeat && rtm.getWSBM()!=null)
		{
			for (int b1=0; b1<rtm.param.numBlocks; b1++)
			{
				for (int b2=0; b2<rtm.param.numBlocks; b2++)
				{
					parameters[getPosition(b1, b2)]=rtm.getBlockWeight(b1, b2);
				}
			}
		}
	}
	
	public double getValue()
	{
		double value=0.0,weight;
		for (int doc=0; doc<rtm.getNumDocs(); doc++)
		{
			for (int d : rtm.getTrainLinkedDocs(doc))
			{
				weight=computeWeight(doc, d);
				value-=Math.log(1.0+Math.exp(-weight));
				if (rtm.getTrainEdgeWeight(doc, d)==0)
				{
					value-=weight;
				}
			}
		}
		for (int topic=0; topic<rtm.param.numTopics; topic++)
		{
			value-=MathUtil.sqr(parameters[topic]/rtm.param.nu)/2.0;
		}
		for (int vocab=0; vocab<rtm.param.numVocab; vocab++)
		{
			value-=MathUtil.sqr(parameters[vocab+rtm.param.numTopics]/rtm.param.nu)/2.0;
		}
		if (rtm.param.blockFeat && rtm.getWSBM()!=null)
		{
			for (int b1=0; b1<rtm.param.numBlocks; b1++)
			{
				for (int b2=0; b2<rtm.param.numBlocks; b2++)
				{
					int pos=getPosition(b1, b2);
					value-=MathUtil.sqr(parameters[pos]/rtm.param.nu)/2.0;
				}
			}
		}
		return value;
	}
	
	public void getValueGradient(double gradient[])
	{
		for (int i=0; i<gradient.length; i++)
		{
			gradient[i]=0.0;
		}
		for (int doc=0; doc<rtm.getNumDocs(); doc++)
		{
			for (int d : rtm.getTrainLinkedDocs(doc))
			{
				double weight=computeWeight(doc, d);
				double commonTerm=Math.exp(-weight)/(1.0+Math.exp(-weight));
				for (int topic=0; topic<rtm.param.numTopics; topic++)
				{
					gradient[topic]+=commonTerm*rtm.getDoc(doc).getTopicCount(topic)/rtm.getDoc(doc).docLength()*
							rtm.getDoc(d).getTopicCount(topic)/rtm.getDoc(d).docLength();
				}
				for (int token : rtm.getDoc(doc).getWordSet())
				{
					if (rtm.getDoc(d).containsWord(token))
					{
						gradient[token+rtm.param.numTopics]+=commonTerm*rtm.getDoc(doc).getWordCount(token)/
								rtm.getDoc(doc).docLength()*rtm.getDoc(d).getWordCount(token)/rtm.getDoc(d).docLength();
					}
				}
				if (rtm.param.blockFeat && rtm.getWSBM()!=null)
				{
					int b1=rtm.getBlockAssign(doc),b2=rtm.getBlockAssign(d),pos=getPosition(b1, b2);
					gradient[pos]+=commonTerm*rtm.getBlockEdgeRate(b1, b2);
				}
				
				if (rtm.getTrainEdgeWeight(doc, d)==0)
				{
					for (int topic=0; topic<rtm.param.numTopics; topic++)
					{
						gradient[topic]-=1.0*rtm.getDoc(doc).getTopicCount(topic)/rtm.getDoc(doc).docLength()*
								rtm.getDoc(d).getTopicCount(topic)/rtm.getDoc(d).docLength();
					}
					for (int token : rtm.getDoc(doc).getWordSet())
					{
						if (rtm.getDoc(d).containsWord(token))
						{
							gradient[token+rtm.param.numTopics]-=1.0*rtm.getDoc(doc).getWordCount(token)/rtm.getDoc(doc).docLength()*
									rtm.getDoc(d).getWordCount(token)/rtm.getDoc(d).docLength();
						}
					}
					if (rtm.param.blockFeat && rtm.getWSBM()!=null)
					{
						int b1=rtm.getBlockAssign(doc),b2=rtm.getBlockAssign(d),pos=getPosition(b1, b2);
						gradient[pos]-=rtm.getBlockEdgeRate(b1, b2);
					}
				}
			}
		}
		for (int topic=0; topic<rtm.param.numTopics; topic++)
		{
			gradient[topic]-=parameters[topic]/MathUtil.sqr(rtm.param.nu);
		}
		for (int vocab=0; vocab<rtm.param.numVocab; vocab++)
		{
			gradient[vocab+rtm.param.numTopics]-=parameters[vocab+rtm.param.numTopics]/MathUtil.sqr(rtm.param.nu);
		}
		if (rtm.param.blockFeat && rtm.getWSBM()!=null)
		{
			for (int b1=0; b1<rtm.param.numBlocks; b1++)
			{
				for (int b2=0; b2<rtm.param.numBlocks; b2++)
				{
					int pos=getPosition(b1, b2);
					gradient[pos]-=parameters[pos]/MathUtil.sqr(rtm.param.nu);
				}
			}
		}
	}
	
	private double computeWeight(int doc1, int doc2)
	{
		double weight=0.0;
		for (int topic=0; topic<rtm.param.numTopics; topic++)
		{
			weight+=parameters[topic]*rtm.getDoc(doc1).getTopicCount(topic)/rtm.getDoc(doc1).docLength()*
					rtm.getDoc(doc2).getTopicCount(topic)/rtm.getDoc(doc2).docLength();
		}
		for (int token : rtm.getDoc(doc1).getWordSet())
		{
			if (rtm.getDoc(doc2).containsWord(token))
			{
				weight+=parameters[token+rtm.param.numTopics]*rtm.getDoc(doc1).getWordCount(token)/
						rtm.getDoc(doc1).docLength()*rtm.getDoc(doc2).getWordCount(token)/rtm.getDoc(doc2).docLength();
			}
		}
		if (rtm.param.blockFeat && rtm.getWSBM()!=null)
		{
			int b1=rtm.getBlockAssign(doc1),b2=rtm.getBlockAssign(doc2);
			weight+=parameters[getPosition(b1, b2)]*rtm.getBlockEdgeRate(b1, b2);
		}
		return weight;
	}
	
	private int getPosition(int block1, int block2)
	{
		return block1*rtm.param.numBlocks+block2+rtm.param.numTopics+rtm.param.numVocab;
	}
	
	public int getNumParameters()
	{
		return parameters.length;
	}
	
	public double getParameter(int i)
	{
		return parameters[i];
	}
	
	public void getParameters(double buffer[])
	{
		for (int i=0; i<parameters.length; i++)
		{
			buffer[i]=parameters[i];
		}
	}
	
	public void setParameter(int i, double r)
	{
		parameters[i]=r;
	}
	
	public void setParameters(double newParameters[])
	{
		for (int i=0; i<parameters.length; i++)
		{
			parameters[i]=newParameters[i];
		}
	}
}
