package yang.weiwei.tlda.util;

import yang.weiwei.util.format.Fourmat;

/**
 * TLDA Word
 * @author Weiwei Yang
 *
 */
public class TLDAWord implements Comparable<TLDAWord>
{
	private String word;
	private int count;
	private double weight;
	
	private boolean compareByCount;
	
	/**
	 * Get word
	 * @return word
	 */
	public String getWord()
	{
		return word;
	}
	
	/**
	 * Get the frequency of this word
	 * @return Frequency of this word
	 */
	public int getCount()
	{
		return count;
	}
	
	/**
	 * Get the weight of this word
	 * @return Weight of this word
	 */
	public double getWeight()
	{
		return weight;
	}
	
	/**
	 * Initialize the object with word and count
	 * @param word Word
	 * @param count Count
	 */
	public TLDAWord(String word, int count)
	{
		this.word=word;
		this.count=count;
		compareByCount=true;
	}
	
	/**
	 * Initialize the object with word and weight
	 * @param word Word
	 * @param weight Weight
	 */
	public TLDAWord(String word, double weight)
	{
		this.word=word;
		this.weight=weight;
		compareByCount=false;
	}
	
	public String toString()
	{
		if (compareByCount) return word+":"+count;
		return word+":"+Fourmat.format(weight);
	}
	
	public int compareTo(TLDAWord o)
	{
		if (compareByCount) return -Integer.compare(this.count, o.count);
		return -Double.compare(this.weight, o.weight);
	}
	
	public boolean equals(Object o)
	{
		if (!(o instanceof TLDAWord)) return false;
		return word.equals(((TLDAWord)o).word);
	}
}
