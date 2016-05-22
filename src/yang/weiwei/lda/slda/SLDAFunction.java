package yang.weiwei.lda.slda;

import cc.mallet.optimize.Optimizable.ByGradientValue;
import yang.weiwei.util.MathUtil;

public class SLDAFunction implements ByGradientValue
{
	private double parameters[];
	private SLDA slda;
	
	public SLDAFunction(SLDA SLDAInst)
	{
		this.slda=SLDAInst;
		parameters=new double[slda.param.numTopics];
		for (int topic=0; topic<slda.param.numTopics; topic++)
		{
			parameters[topic]=slda.eta[topic];
		}
	}
	
	public double getValue()
	{
		double value=0.0,weight;
		for (int doc=0; doc<slda.getNumDocs(); doc++)
		{
			if (!slda.labelStatuses[doc]) continue;
			weight=computeWeight(doc);
			value-=MathUtil.sqr(weight)/2.0;
		}
		for (int topic=0; topic<slda.param.numTopics; topic++)
		{
			value-=MathUtil.sqr(parameters[topic]/slda.param.nu)/2.0;
		}
		return value;
	}
	
	public void getValueGradient(double gradient[])
	{
		for (int topic=0; topic<slda.param.numTopics; topic++)
		{
			gradient[topic]=0.0;
		}
		for (int doc=0; doc<slda.getNumDocs(); doc++)
		{
			if (!slda.labelStatuses[doc]) continue;
			double weight=computeWeight(doc);
			for (int topic=0; topic<slda.param.numTopics; topic++)
			{
				gradient[topic]+=weight*slda.getDoc(doc).getTopicCount(topic)/slda.getDoc(doc).docLength();
			}
		}
		for (int topic=0; topic<slda.param.numTopics; topic++)
		{
			gradient[topic]-=parameters[topic]/MathUtil.sqr(slda.param.nu);
		}
	}
	
	private double computeWeight(int doc)
	{
		double weight=(double)slda.labels[doc];
		for (int topic=0; topic<slda.param.numTopics; topic++)
		{
			weight-=parameters[topic]*slda.getDoc(doc).getTopicCount(topic)/slda.getDoc(doc).docLength();
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
