package yang.weiwei.lda.rtm;

import cc.mallet.optimize.Optimizable.ByGradientValue;
import yang.weiwei.util.MathUtil;

public class RTMFunction implements ByGradientValue
{
	private double parameters[];
	private RTM rtm;
	
	public RTMFunction(RTM RTMInst)
	{
		this.rtm=RTMInst;
		parameters=new double[rtm.param.numTopics];
		for (int topic=0; topic<rtm.param.numTopics; topic++)
		{
			parameters[topic]=rtm.eta[topic];
		}
	}
	
	public double getValue()
	{
		double value=0.0,weight;
		for (int doc=0; doc<rtm.getNumDocs(); doc++)
		{
			for (int d : rtm.trainEdgeWeights.get(doc).keySet())
			{
				weight=computeWeight(doc, d);
				value-=Math.log(1.0+Math.exp(-weight));
				if (rtm.trainEdgeWeights.get(doc).get(d)==0)
				{
					value-=weight;
				}
			}
		}
		for (int topic=0; topic<rtm.param.numTopics; topic++)
		{
			value-=MathUtil.sqr(parameters[topic]/rtm.param.nu)/2.0;
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
			for (int d : rtm.trainEdgeWeights.get(doc).keySet())
			{
				double weight=computeWeight(doc, d);
				double commonTerm=Math.exp(-weight)/(1.0+Math.exp(-weight));
				for (int topic=0; topic<rtm.param.numTopics; topic++)
				{
					gradient[topic]+=commonTerm*rtm.getDoc(doc).getTopicCount(topic)/rtm.getDoc(doc).docLength()*
							rtm.getDoc(d).getTopicCount(topic)/rtm.getDoc(d).docLength();
				}
				if (rtm.trainEdgeWeights.get(doc).get(d)==0)
				{
					for (int topic=0; topic<rtm.param.numTopics; topic++)
					{
						gradient[topic]-=1.0*rtm.getDoc(doc).getTopicCount(topic)/rtm.getDoc(doc).docLength()*
								rtm.getDoc(d).getTopicCount(topic)/rtm.getDoc(d).docLength();
					}
				}
			}
		}
		for (int topic=0; topic<rtm.param.numTopics; topic++)
		{
			gradient[topic]-=parameters[topic]/MathUtil.sqr(rtm.param.nu);
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
		return weight;
	}
	
//	private double computeEmptyWeight()
//	{
//		double weight=0.0;
//		for (int topic=0; topic<rtm.param.numTopics; topic++)
//		{
//			weight+=parameters[topic]/MathUtil.sqr(rtm.param.numTopics);
//		}
//		return weight;
//	}
	
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
