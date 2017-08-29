package yang.weiwei.tlda.util;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Set;

/**
 * TLDA document
 * @author Weiwei Yang
 *
 */
public class TLDADoc
{	
	private int tokens[];
	private int topicAssigns[];
	private TLDATopicNode nodeAssigns[];
	private HashMap<Integer, Integer> wordCount;
	
	private int topicCounts[];
	private TreeMap<Integer, Integer> pathCounts;
	
	public TLDADoc(int numTopics, int numVocab, int numPaths)
	{
		this("", numTopics, numVocab, numPaths);
	}
	
	/**
	 * Initialize the object with a document
	 * @param document Document
	 * @param numTopics Number of topics
	 * @param numVocab Vocabulary size
	 * @param numPaths Number of total paths
	 */
	public TLDADoc(String document, int numTopics, int numVocab, int numPaths)
	{
		wordCount=new HashMap<Integer, Integer>();
		topicCounts=new int[numTopics];
		pathCounts=new TreeMap<Integer, Integer>();
		
		String seg[]=document.split(" "),segseg[];
		int len=Integer.valueOf(seg[0]);
		tokens=new int[len];
		topicAssigns=new int[len];
		nodeAssigns=new TLDATopicNode[len];
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
				nodeAssigns[tempLen+j]=null;
			}
			tempLen+=count;
		}
	}
	
	public TLDADoc(String document, int numTopics, HashMap<String, Integer> vocabMap, int numPaths)
	{
		topicCounts=new int[numTopics];
		pathCounts=new TreeMap<Integer, Integer>();
		
		String seg[]=document.split(" ");
		int len=0;
		for (int i=0; i<seg.length; i++)
		{
			if (seg[i].length()>0) len++;
		}
		tokens=new int[len];
		topicAssigns=new int[len];
		nodeAssigns=new TLDATopicNode[len];
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
			nodeAssigns[tempLen]=null;
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
	 * Get a token's assigned leaf node
	 * @param pos Token position
	 * @return The token's assigned leaf node
	 */
	public TLDATopicNode getNodeAssign(int pos)
	{
		return nodeAssigns[pos];
	}
	
	/**
	 * Assign a topic and a node to a token
	 * @param pos Token position
	 * @param topic Topic to assign
	 * @param node Node to assign
	 */
	public void assignTopicAndNode(int pos, int topic, TLDATopicNode node)
	{
		int oldTopic=getTopicAssign(pos);
		TLDATopicNode oldNode=getNodeAssign(pos);
		nodeAssigns[pos]=node;
		nodeAssigns[pos].assignPath();
		topicAssigns[pos]=topic;
		if (oldTopic==-1)
		{
			topicCounts[topic]++;
		}
		if (oldNode==null)
		{
			int no=node.getLeafNodeNo();
			if (!pathCounts.containsKey(no))
			{
				pathCounts.put(no, 0);
			}
			pathCounts.put(no, pathCounts.get(no)+1);
		}
	}
	
	/**
	 * Unassign a token's topic and node
	 * @param pos Token position
	 */
	public void unassignTopicAndNode(int pos)
	{
		int oldTopic=getTopicAssign(pos);
		TLDATopicNode oldNode=getNodeAssign(pos);
		nodeAssigns[pos].unassignPath();
		nodeAssigns[pos]=null;
		topicAssigns[pos]=-1;
		if (oldTopic!=-1)
		{
			topicCounts[oldTopic]--;
		}
		if (oldNode!=null)
		{
			int no=oldNode.getLeafNodeNo();
			pathCounts.put(no, pathCounts.get(no)-1);
			if (pathCounts.get(no)==0)
			{
				pathCounts.remove(no);
			}
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
	 * Get the number of tokens assigned to a path
	 * @param path Path number
	 * @return Number of tokens assigned to this path
	 */
	public int getPathCount(int path)
	{
		return (pathCounts.containsKey(path)? pathCounts.get(path) : 0);
	}
	
	/**
	 * Get the set of unique paths in this document
	 * @return Set of unique paths
	 */
	public Set<Integer> getPathSet()
	{
		return pathCounts.keySet();
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
