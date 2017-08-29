package yang.weiwei.util;

import yang.weiwei.util.format.Fourmat;

public class Word implements Comparable<Word>
{
	private String word;
	private double weight;
	
	public String getWord()
	{
		return word;
	}
	
	public double getWeight()
	{
		return weight;
	}
	
	public Word(String word, double weight)
	{
		this.word=word;
		this.weight=weight;
	}
	
	public String toString()
	{
		return word+":"+Fourmat.format(weight);
	}
	
	public int compareTo(Word o)
	{
		return -Double.compare(this.weight, o.weight);
	}
	
	public boolean equals(Object o)
	{
		if (!(o instanceof Word)) return false;
		return word.equals(((Word)o).word);
	}
}
