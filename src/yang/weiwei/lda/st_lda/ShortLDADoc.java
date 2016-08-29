package yang.weiwei.lda.st_lda;

import java.util.HashMap;
import java.util.Set;

public class ShortLDADoc
{
	private int tokens[];
	private int topicAssigns;
	private HashMap<Integer, Integer> wordCount;
	
	public ShortLDADoc(int numTopics, int numVocab)
	{
		this("", numTopics, numVocab);
	}
	
	public ShortLDADoc(String document, int numTopics, int numVocab)
	{
		wordCount=new HashMap<Integer, Integer>();
		
		String seg[]=document.split(" "),segseg[];
		int len=Integer.valueOf(seg[0]);
		tokens=new int[len];
		topicAssigns=-1;
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
			}
			tempLen+=count;
		}
	}
	
	public ShortLDADoc(String document, int numTopics, HashMap<String, Integer> vocabMap)
	{
		String seg[]=document.split(" ");
		int len=0;
		for (int i=0; i<seg.length; i++)
		{
			if (seg[i].length()>0) len++;
		}
		tokens=new int[len];
		topicAssigns=-1;
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
			tempLen++;
		}
	}
	
	public int docLength()
	{
		return tokens.length;
	}
	
	public int getTopicAssign()
	{
		return topicAssigns;
	}
	
	public void assignTopic(int topic)
	{
		topicAssigns=topic;
	}
	
	public void unassignTopic()
	{
		topicAssigns=-1;
	}
	
	public int getWord(int pos)
	{
		return tokens[pos];
	}
	
	public Set<Integer> getWordSet()
	{
		return wordCount.keySet();
	}
	
	public int getWordCount(int word)
	{
		return (wordCount.containsKey(word)? wordCount.get(word) : 0);
	}
	
	public boolean containsWord(int word)
	{
		return wordCount.containsKey(word);
	}
}
