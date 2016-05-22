package yang.weiwei.lda.rtm;

import yang.weiwei.util.format.Fourmat;

public class RTMDocProb implements Comparable<RTMDocProb>
{
	private int docNo;
	private double prob;
	
	public int getDocNo()
	{
		return docNo;
	}
	
	public double getProb()
	{
		return prob;
	}
	
	public RTMDocProb(int no, double prob)
	{
		this.docNo=no;
		this.prob=prob;
	}
	
	public int compareTo(RTMDocProb o)
	{
		return -Double.compare(this.prob, o.prob);
	}
	
	public String toString()
	{
		return docNo+":"+Fourmat.format(prob);
	}
}
