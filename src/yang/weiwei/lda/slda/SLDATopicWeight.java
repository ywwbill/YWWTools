package yang.weiwei.lda.slda;

import yang.weiwei.util.format.Fourmat;

/**
 * SLDA topic with weight
 * @author Yang Weiwei
 *
 */
public class SLDATopicWeight implements Comparable<SLDATopicWeight>
{
	private double wordDist[];
	private double weight;
	private int topicNo;
	
	/**
	 * Initialize SLDA topic object
	 * @param wordDist Topic's word distribution
	 * @param weight Topic weight
	 * @param topicNo Topic number
	 */
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
	
	/**
	 * Get this topic's word distribution
	 * @return This topic's word distribution
	 */
	public double[] getWordDist()
	{
		return wordDist;
	}
	
	/**
	 * Get this topic's weight
	 * @return This topic's weight
	 */
	public double getWeight()
	{
		return weight;
	}
	
	/**
	 * Get this topic's number
	 * @return This topic's number
	 */
	public int getTopicNo()
	{
		return topicNo;
	}
	
	public String toString()
	{
		return "Topic: "+topicNo+"\tWeight: "+Fourmat.format(weight);
	}
}
