package yang.weiwei.lda.util;

import java.util.HashMap;
import java.util.Set;

/**
 * LDA document
 * @author Weiwei Yang
 *
 */
public class LDADoc
{	
	private int tokens[];
	private int topicAssigns[];
	private HashMap<Integer, Integer> wordCount;
	
	private int topicCounts[];
	
	public LDADoc(int numTopics, int numVocab)
	{
		this("", numTopics, numVocab);
	}
	
	/**
	 * Initialize the object with a document
	 * @param document Document
	 * @param numTopics Number of topics
	 * @param numVocab Vocabulary size
	 */
	public LDADoc(String document, int numTopics, int numVocab)
	{
		wordCount=new HashMap<Integer, Integer>();
		topicCounts=new int[numTopics];
		
		String seg[]=document.split(" "),segseg[];
		int len=Integer.valueOf(seg[0]);
		tokens=new int[len];
		topicAssigns=new int[len];
		int tempLen=0;
		for (int i=1; i<seg.length; i++)
		{
			if (seg[i].length()==0) continue;
			segseg=seg[i].split(":");
			int word=Integer.valueOf(segseg[0]);
			int count=Integer.valueOf(segseg[1]);
			assert(word>=0 && word<numVocab);
			
			if (!wordCount.containsKey(word))
			{
				wordCount.put(word, 0);
			}
			wordCount.put(word, wordCount.get(word)+count);
				
			for (int j=0; j<count; j++)
			{
				tokens[tempLen+j]=word;
				topicAssigns[tempLen+j]=-1;
			}
			tempLen+=count;
		}
	}
	
	public LDADoc(String document, int numTopics, HashMap<String, Integer> vocabMap)
	{
		topicCounts=new int[numTopics];
		
		String seg[]=document.split(" ");
		int len=0;
		for (int i=0; i<seg.length; i++)
		{
			if (seg[i].length()>0) len++;
		}
		tokens=new int[len];
		topicAssigns=new int[len];
		int tempLen=0;
		for (int i=0; i<seg.length; i++)
		{
			if (seg[i].length()==0) continue;
			int word=vocabMap.get(seg[i]);
			
			if (!wordCount.containsKey(word))
			{
				wordCount.put(word, 0);
			}
			wordCount.put(word, wordCount.get(word)+1);
			
			tokens[tempLen]=word;
			topicAssigns[tempLen]=-1;
			tempLen++;
		}
	}
	
	/**
	 * Get document's number of tokens
	 * @return Number of tokens
	 */
	public int docLength()
	{
		return tokens.length;
	}
	
	/**
	 * Get a token's topic assignment
	 * @param pos Token position
	 * @return Corresponding token's topic assignment
	 */
	public int getTopicAssign(int pos)
	{
		return topicAssigns[pos];
	}
	
	/**
	 * Assign a topic to a token
	 * @param pos Token position
	 * @param topic Topic to assign
	 */
	public void assignTopic(int pos, int topic)
	{
		int oldTopic=getTopicAssign(pos);
		topicAssigns[pos]=topic;
		if (oldTopic==-1)
		{
			topicCounts[topic]++;
		}
	}
	
	/**
	 * Unassign a token's topic
	 * @param pos Token position
	 */
	public void unassignTopic(int pos)
	{
		int oldTopic=getTopicAssign(pos);
		topicAssigns[pos]=-1;
		if (oldTopic!=-1)
		{
			topicCounts[oldTopic]--;
		}
	}
	
	/**
	 * Get a token
	 * @param pos Token position
	 * @return Token
	 */
	public int getWord(int pos)
	{
		return tokens[pos];
	}
	
	/**
	 * Get the number of tokens assigned to a topic
	 * @param topic Topic number
	 * @return Number of tokens assigned to this topic
	 */
	public int getTopicCount(int topic)
	{
		return topicCounts[topic];
	}
	
	/**
	 * Get the set of unique words in this document
	 * @return Set of unique words
	 */
	public Set<Integer> getWordSet()
	{
		return wordCount.keySet();
	}
	
	/**
	 * Get the frequency of a given word
	 * @param word Word
	 * @return Frequency of the given word
	 */
	public int getWordCount(int word)
	{
		return (wordCount.containsKey(word)? wordCount.get(word) : 0);
	}
	
	/**
	 * Check whether this document contains a given word
	 * @param word Word
	 * @return Whether this document contains the word
	 */
	public boolean containsWord(int word)
	{
		return wordCount.containsKey(word);
	}
}
