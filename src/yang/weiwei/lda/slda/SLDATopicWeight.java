package yang.weiwei.lda.slda;

import yang.weiwei.util.format.Fourmat;

public class SLDATopicWeight implements Comparable<SLDATopicWeight>
{
	private double wordDist[];
	private double weight;
	private int topicNo;
	
	public SLDATopicWeight(double wordDist[], double weight, int topicNo)
	{
		this.weight=weight;
		this.topicNo=topicNo;
		this.wordDist=new double[wordDist.length];
		for (int vocab=0; vocab<wordDist.length; vocab++)
		{
			this.wordDist[vocab]=wordDist[vocab];
		}
	}
	
	public int compareTo(SLDATopicWeight o)
	{
		return -Double.compare(weight, o.weight);
	}
	
	public boolean equals(Object o)
	{
		if (!(o instanceof SLDATopicWeight)) return false;
		return (topicNo==((SLDATopicWeight)o).topicNo);
	}
	
	public double[] getWordDist()
	{
		return wordDist;
	}
	
	public double getWeight()
	{
		return weight;
	}
	
	public int getTopicNo()
	{
		return topicNo;
	}
	
	public String toString()
	{
		return "Topic: "+topicNo+"\tWeight: "+Fourmat.format(weight);
	}
}
