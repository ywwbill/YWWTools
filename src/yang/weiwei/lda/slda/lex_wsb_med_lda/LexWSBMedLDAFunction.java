package yang.weiwei.lda.slda.lex_wsb_med_lda;

import cc.mallet.optimize.Optimizable.ByGradientValue;
import yang.weiwei.util.MathUtil;

public class LexWSBMedLDAFunction implements ByGradientValue
{
	private double parameters[];
	private LexWSBMedLDA slda;
	
	public LexWSBMedLDAFunction(LexWSBMedLDA SLDAInst)
	{
		this.slda=SLDAInst;
		parameters=new double[slda.param.numTopics+slda.param.numVocab];
		for (int topic=0; topic<slda.param.numTopics; topic++)
		{
			parameters[topic]=slda.getTopicWeight(topic);
		}
		for (int vocab=0; vocab<slda.param.numVocab; vocab++)
		{
			parameters[vocab+slda.param.numTopics]=slda.getLexWeight(vocab);
		}
	}
	
	public double getValue()
	{
		double value=0.0,weight;
		for (int doc=0; doc<slda.getNumDocs(); doc++)
		{
			if (!slda.getLabelStatus(doc)) continue;
			weight=computeWeight(doc);
			value-=(MathUtil.sqr(slda.param.c*weight)-2.0*slda.param.c*(slda.param.c+slda.lambda[doc])*slda.getLabel(doc)*weight)/
					(2.0*slda.lambda[doc]);
		}
		for (int topic=0; topic<slda.param.numTopics; topic++)
		{
			value-=MathUtil.sqr(parameters[topic]/slda.param.nu)/2.0;
		}
		for (int vocab=0; vocab<slda.param.numVocab; vocab++)
		{
			value-=MathUtil.sqr(parameters[vocab+slda.param.numTopics]/slda.param.nu)/2.0;
		}
		return value;
	}
	
	public void getValueGradient(double gradient[])
	{
		for (int i=0; i<gradient.length; i++)
		{
			gradient[i]=0.0;
		}
		for (int doc=0; doc<slda.getNumDocs(); doc++)
		{
			if (!slda.getLabelStatus(doc)) continue;
			double weight=computeWeight(doc);
			double commonTerm=slda.param.c*(slda.param.c*weight-(slda.param.c+slda.lambda[doc])*slda.getLabel(doc))/
					(slda.lambda[doc]*slda.getDoc(doc).docLength());
			for (int topic=0; topic<slda.param.numTopics; topic++)
			{
				gradient[topic]-=commonTerm*slda.getDoc(doc).getTopicCount(topic);
			}
			for (int token : slda.getDoc(doc).getWordSet())
			{
				gradient[token+slda.param.numTopics]-=commonTerm*slda.getDoc(doc).getWordCount(token);
			}
		}
		for (int topic=0; topic<slda.param.numTopics; topic++)
		{
			gradient[topic]-=parameters[topic]/MathUtil.sqr(slda.param.nu);
		}
		
		for (int vocab=0; vocab<slda.param.numVocab; vocab++)
		{
			gradient[vocab+slda.param.numTopics]-=parameters[vocab+slda.param.numTopics]/MathUtil.sqr(slda.param.nu);
		}
	}
	
	private double computeWeight(int doc)
	{
		double weight=0.0;
		for (int topic=0; topic<slda.param.numTopics; topic++)
		{
			weight+=parameters[topic]*slda.getDoc(doc).getTopicCount(topic)/slda.getDoc(doc).docLength();
		}
		for (int token : slda.getDoc(doc).getWordSet())
		{
			weight+=parameters[token+slda.param.numTopics]*slda.getDoc(doc).getWordCount(token)/slda.getDoc(doc).docLength();
		}
		return weight;
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
