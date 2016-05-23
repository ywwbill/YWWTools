package yang.weiwei.lda.util;

import yang.weiwei.util.format.Fourmat;

/**
 * LDA word
 * @author Weiwei Yang
 *
 */
public class LDAWord implements Comparable<LDAWord>
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
	public LDAWord(String word, int count)
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
	public LDAWord(String word, double weight)
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
	
	public int compareTo(LDAWord o)
	{
		if (compareByCount) return -Integer.compare(this.count, o.count);
		return -Double.compare(this.weight, o.weight);
	}
	
	public boolean equals(Object o)
	{
		if (!(o instanceof LDAWord)) return false;
		return word.equals(((LDAWord)o).word);
	}
}
