package yang.weiwei.lda.util;

/**
 * LDA topic
 * @author Weiwei Yang
 *
 */
public class LDATopic
{
	private int vocabCounts[];
	private int totalTokens;
	
	/**
	 * Initialize the object with vocabulary size
	 * @param numVocab Vocabulary size
	 */
	public LDATopic(int numVocab)
	{
		vocabCounts=new int[numVocab];
		totalTokens=0;
	}
	
	/**
	 * Assign a word to this topic
	 * @param vocab Word
	 */
	public void addVocab(int vocab)
	{
		vocabCounts[vocab]++;
		totalTokens++;
	}
	
	/**
	 * Unassign a word from this topic
	 * @param vocab Word
	 */
	public void removeVocab(int vocab)
	{
		vocabCounts[vocab]--;
		totalTokens--;
	}
	
	/**
	 * Get given word's number of assignments to this topic 
	 * @param vocab Word
	 * @return Given word's number of assignments to this topic
	 */
	public int getVocabCount(int vocab)
	{
		return vocabCounts[vocab];
	}

	/**
	 * Get total number of tokens assigned to this topic
	 * @return Total number of tokens assigned to this topic
	 */
	public int getTotalTokens()
	{
		return totalTokens;
	}
}
