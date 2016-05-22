package yang.weiwei.lda.util;

public class LDATopic
{
	private int vocabCounts[];
	private int totalTokens;
	
	public LDATopic(int numVocab)
	{
		vocabCounts=new int[numVocab];
		totalTokens=0;
	}
	
	public void addVocab(int vocab)
	{
		vocabCounts[vocab]++;
		totalTokens++;
	}
	
	public void removeVocab(int vocab)
	{
		vocabCounts[vocab]--;
		totalTokens--;
	}
	
	public int getVocabCount(int vocab)
	{
		return vocabCounts[vocab];
	}
	
	public int getTotalTokens()
	{
		return totalTokens;
	}
}
