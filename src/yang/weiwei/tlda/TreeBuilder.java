package yang.weiwei.tlda;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

import yang.weiwei.util.IOUtil;
import yang.weiwei.util.Word;
import yang.weiwei.tlda.util.TLDATopicPriorNode;

/**
 * Build tree priors given vocabulary and word associations
 * @author Weiwei Yang
 *
 */
public class TreeBuilder
{
	/**
	 * Build a two-level tree
	 * @param scoreFileName The word association file name
	 * @param vocabFileName The vocabulary file name
	 * @param printTreeFileName The tree file name
	 * @param numTop The number of child nodes per internal node
	 * @throws IOException IOException
	 */
	public void build2LevelTree(String scoreFileName, String vocabFileName,
			String printTreeFileName, int numTop) throws IOException
	{
		BufferedReader brVocab=new BufferedReader(new FileReader(vocabFileName));
		ArrayList<String> vocab=new ArrayList<String>();
		HashMap<String, Integer> vocabMap=new HashMap<String, Integer>();
		String line;
		while ((line=brVocab.readLine())!=null)
		{
			vocab.add(line);
			vocabMap.put(line, vocabMap.size());
		}
		brVocab.close();
		int numVocab=vocab.size();
		
		TLDATopicPriorNode root=new TLDATopicPriorNode();
		BufferedReader brScore=new BufferedReader(new FileReader(scoreFileName));
		String seg[];
		ArrayList<Word> words=new ArrayList<Word>();
		for (int i=0; i<numVocab; i++)
		{
			words.clear();
			TLDATopicPriorNode internalNode=new TLDATopicPriorNode(vocab.get(i), i);
			internalNode.addChild(new TLDATopicPriorNode(vocab.get(i), i));
			line=brScore.readLine();
			seg=line.split(" ");
			for (int j=0; j<numVocab; j++)
			{
				if (seg[j].equals("0") || seg[j].equals("0.0") || Double.valueOf(seg[j])<0.0) continue;
				words.add(new Word(vocab.get(j), Double.valueOf(seg[j])));
			}
			Collections.sort(words);
			for (int j=0; j<numTop && j<words.size(); j++)
			{
				TLDATopicPriorNode leafNode=new TLDATopicPriorNode(words.get(j).getWord(), vocabMap.get(words.get(j).getWord()));
				internalNode.addChild(leafNode);
			}
			root.addChild(internalNode);
		}
		brScore.close();
		
		printTree(printTreeFileName, root);
	}
	
	/**
	 * Build a tree using hierarchical agglomerative clustering
	 * @param scoreFileName The word association file name
	 * @param vocabFileName The vocabulary file name
	 * @param printTreeFileName The tree file name
	 * @param threshold The threshold of association confidence: words with an association lower than the threshold are randomly paired and inserted (to avoid super long paths)
	 * @throws IOException IOException
	 */
	public void hac(String scoreFileName, String vocabFileName,
			String printTreeFileName, double threshold) throws IOException
	{
		IOUtil.println("Reading vocabulary ...");
		BufferedReader brVocab=new BufferedReader(new FileReader(vocabFileName));
		ArrayList<String> vocab=new ArrayList<String>();
		HashMap<String, Integer> vocabMap=new HashMap<String, Integer>();
		String line;
		while ((line=brVocab.readLine())!=null)
		{
			vocab.add(line);
			vocabMap.put(line, vocabMap.size());
		}
		brVocab.close();
		int numVocab=vocab.size();
		
		IOUtil.println("Reading scores ...");
		double scores[][]=new double[numVocab][numVocab];
		BufferedReader brScore=new BufferedReader(new FileReader(scoreFileName));
		String seg[];
		for (int i=0; i<numVocab; i++)
		{
			if ((i+1)%1000==0) IOUtil.println((i+1)+"/"+numVocab);
			line=brScore.readLine();
			seg=line.split(" ");
			for (int j=0; j<numVocab; j++)
			{
				scores[i][j]=Double.valueOf(seg[j]);
			}
		}
		brScore.close();
		
		IOUtil.println("Adding clusters ...");
		ArrayList<Cluster> clusters=new ArrayList<Cluster>();
		ArrayList<Boolean> available=new ArrayList<Boolean>();
		ArrayList<TLDATopicPriorNode> nodes=new ArrayList<TLDATopicPriorNode>();
		for (int i=0; i<numVocab; i++)
		{
			if ((i+1)%1000==0) IOUtil.println((i+1)+"/"+numVocab);
			Cluster cluster=new Cluster(i);
			cluster.addWord(i);
			clusters.add(cluster);
			available.add(true);
			
			nodes.add(new TLDATopicPriorNode(vocab.get(i), i));
		}
//		int maxClusterNo=numVocab;
		int numAvailable=numVocab;
		
		IOUtil.println("Adding edges ...");
		PriorityQueue<Edge> edges=new PriorityQueue<Edge>();
		for (int i=0; i<numVocab; i++)
		{
			if ((i+1)%1000==0) IOUtil.println((i+1)+"/"+numVocab);
			for (int j=0; j<numVocab; j++)
			{
				if (i==j) continue;
				edges.add(new Edge(i, j, scores[i][j]));
			}
		}
		
		IOUtil.println("Clustering ...");
		while (numAvailable>1)
		{
			if (numAvailable%100==0) IOUtil.println("numAvailable="+numAvailable+
					" #edges="+edges.size()+" topEdgeWeight="+edges.peek().weight);
			while ((!available.get(edges.peek().c1) || !available.get(edges.peek().c2)) && edges.peek().weight>=threshold)
			{
				edges.poll();
			}
			if (edges.peek().weight>=threshold)
			{
				Edge edge=edges.poll();
				Cluster newCluster=new Cluster(clusters.size());
				newCluster.addAllWord(clusters.get(edge.c1));
				newCluster.addAllWord(clusters.get(edge.c2));
				TLDATopicPriorNode newNode=new TLDATopicPriorNode();
				newNode.addChild(nodes.get(edge.c1));
				newNode.addChild(nodes.get(edge.c2));
				nodes.add(newNode);
				available.set(edge.c1, false);
				available.set(edge.c2, false);
				numAvailable-=2;
				for (int cid=0; cid<clusters.size(); cid++)
				{
					if (!available.get(cid)) continue;
					edges.add(new Edge(newCluster.ID, cid, newCluster.computeScore(clusters.get(cid), scores)));
					edges.add(new Edge(cid, newCluster.ID, clusters.get(cid).computeScore(newCluster, scores)));
				}
				clusters.add(newCluster);
				available.add(true);
				numAvailable++;
			}
			else
			{
				int c1=-1,c2=-1;
				for (int i=0; i<available.size(); i++)
				{
					if (!available.get(i)) continue;
					if (c1==-1)
					{
						c1=i;
					}
					else
					{
						if (c2==-1)
						{
							c2=i;
							break;
						}
					}
				}
				
				Cluster newCluster=new Cluster(clusters.size());
				newCluster.addAllWord(clusters.get(c1));
				newCluster.addAllWord(clusters.get(c2));
				TLDATopicPriorNode newNode=new TLDATopicPriorNode();
				newNode.addChild(nodes.get(c1));
				newNode.addChild(nodes.get(c2));
				nodes.add(newNode);
				available.set(c1, false);
				available.set(c2, false);
				numAvailable-=2;
				for (int cid=0; cid<clusters.size(); cid++)
				{
					if (!available.get(cid)) continue;
					edges.add(new Edge(newCluster.ID, cid, newCluster.computeScore(clusters.get(cid), scores)));
					edges.add(new Edge(cid, newCluster.ID, clusters.get(cid).computeScore(newCluster, scores)));
				}
				clusters.add(newCluster);
				available.add(true);
				numAvailable++;
			}
		}
		
		IOUtil.println("Printing tree ...");
		printTree(printTreeFileName, nodes.get(nodes.size()-1));
	}
	
	/**
	 * Build a tree using hierarchical agglomerative clustering with leaf duplication
	 * @param scoreFileName The word association file name
	 * @param vocabFileName The vocabulary file name
	 * @param printTreeFileName The tree file name
	 * @param threshold The threshold of association confidence: words with an association lower than the threshold are randomly paired and inserted (to avoid super long paths)
	 * @throws IOException IOException
	 */
	public void hacWithLeafDup(String scoreFileName, String vocabFileName,
			String printTreeFileName, double threshold) throws IOException
	{
		IOUtil.println("Reading vocabulary ...");
		BufferedReader brVocab=new BufferedReader(new FileReader(vocabFileName));
		ArrayList<String> vocab=new ArrayList<String>();
		HashMap<String, Integer> vocabMap=new HashMap<String, Integer>();
		String line;
		while ((line=brVocab.readLine())!=null)
		{
			vocab.add(line);
			vocabMap.put(line, vocabMap.size());
		}
		brVocab.close();
		int numVocab=vocab.size();
		
		IOUtil.println("Reading scores ...");
		double scores[][]=new double[numVocab][numVocab];
		BufferedReader brScore=new BufferedReader(new FileReader(scoreFileName));
		String seg[];
		for (int i=0; i<numVocab; i++)
		{
			if ((i+1)%1000==0) IOUtil.println((i+1)+"/"+numVocab);
			line=brScore.readLine();
			seg=line.split(" ");
			for (int j=0; j<numVocab; j++)
			{
				scores[i][j]=Double.valueOf(seg[j]);
			}
		}
		brScore.close();
		
		IOUtil.println("Adding clusters ...");
		ArrayList<Cluster> clusters=new ArrayList<Cluster>();
		ArrayList<Boolean> available=new ArrayList<Boolean>();
		ArrayList<TLDATopicPriorNode> nodes=new ArrayList<TLDATopicPriorNode>();
		HashSet<Integer> addedPairs=new HashSet<Integer>();
		for (int i=0; i<numVocab; i++)
		{
			if ((i+1)%1000==0) IOUtil.println((i+1)+"/"+numVocab);
			double maxScore=0.0;
			int maxWord=-1;
			for (int j=0; j<numVocab; j++)
			{
				if (i==j || addedPairs.contains(j*numVocab+i)) continue;
				if (scores[i][j]>maxScore)
				{
					maxScore=scores[i][j];
					maxWord=j;
				}
			}
			
			if (maxWord==-1)
			{
				Cluster newCluster=new Cluster(i);
				newCluster.addWord(i);
				clusters.add(newCluster);
				available.add(true);
				
				nodes.add(new TLDATopicPriorNode(vocab.get(i), i));
			}
			else
			{
				Cluster newCluster=new Cluster(i);
				newCluster.addWord(i);
				newCluster.addWord(maxWord);
				clusters.add(newCluster);
				available.add(true);
				
				TLDATopicPriorNode newNode=new TLDATopicPriorNode();
				newNode.addChild(new TLDATopicPriorNode(vocab.get(i), i));
				newNode.addChild(new TLDATopicPriorNode(vocab.get(maxWord), maxWord));
				nodes.add(newNode);
				
				addedPairs.add(i*numVocab+maxWord);
			}
		}
		int numAvailable=numVocab;
		
		IOUtil.println("Adding edges ...");
		PriorityQueue<Edge> edges=new PriorityQueue<Edge>();
		for (int i=0; i<numVocab; i++)
		{
			for (int j=0; j<numVocab; j++)
			{
				if (i==j) continue;
				edges.add(new Edge(i, j, clusters.get(i).computeScore(clusters.get(j), scores)));
			}
		}
		
		IOUtil.println("Clustering ...");
		while (numAvailable>1)
		{
			if (numAvailable%100==0) IOUtil.println("numAvailable="+numAvailable+
					" #edges="+edges.size()+" topEdgeWeight="+edges.peek().weight);
			while ((!available.get(edges.peek().c1) || !available.get(edges.peek().c2)) && edges.peek().weight>=threshold)
			{
				edges.poll();
			}
			if (edges.peek().weight>=threshold)
			{
				Edge edge=edges.poll();
				Cluster newCluster=new Cluster(clusters.size());
				newCluster.addAllWord(clusters.get(edge.c1));
				newCluster.addAllWord(clusters.get(edge.c2));
				TLDATopicPriorNode newNode=new TLDATopicPriorNode();
				newNode.addChild(nodes.get(edge.c1));
				newNode.addChild(nodes.get(edge.c2));
				nodes.add(newNode);
				available.set(edge.c1, false);
				available.set(edge.c2, false);
				numAvailable-=2;
				for (int cid=0; cid<clusters.size(); cid++)
				{
					if (!available.get(cid)) continue;
					edges.add(new Edge(newCluster.ID, cid, newCluster.computeScore(clusters.get(cid), scores)));
					edges.add(new Edge(cid, newCluster.ID, clusters.get(cid).computeScore(newCluster, scores)));
				}
				clusters.add(newCluster);
				available.add(true);
				numAvailable++;
			}
			else
			{
				int c1=-1,c2=-1;
				for (int i=0; i<available.size(); i++)
				{
					if (!available.get(i)) continue;
					if (c1==-1)
					{
						c1=i;
					}
					else
					{
						if (c2==-1)
						{
							c2=i;
							break;
						}
					}
				}
				
				Cluster newCluster=new Cluster(clusters.size());
				newCluster.addAllWord(clusters.get(c1));
				newCluster.addAllWord(clusters.get(c2));
				TLDATopicPriorNode newNode=new TLDATopicPriorNode();
				newNode.addChild(nodes.get(c1));
				newNode.addChild(nodes.get(c2));
				nodes.add(newNode);
				available.set(c1, false);
				available.set(c2, false);
				numAvailable-=2;
				for (int cid=0; cid<clusters.size(); cid++)
				{
					if (!available.get(cid)) continue;
					edges.add(new Edge(newCluster.ID, cid, newCluster.computeScore(clusters.get(cid), scores)));
					edges.add(new Edge(cid, newCluster.ID, clusters.get(cid).computeScore(newCluster, scores)));
				}
				clusters.add(newCluster);
				available.add(true);
				numAvailable++;
			}
		}
		
		IOUtil.println("Printing tree ...");
		printTree(printTreeFileName, nodes.get(nodes.size()-1));
	}
	
	/**
	 * Write the tree to a file in human-readable format
	 * @param printTreeFileName The tree prior file name
	 * @param root The root node
	 * @throws IOException IOException
	 */
	public void printTree(String printTreeFileName,	TLDATopicPriorNode root) throws IOException
	{
		root.prettyPrint(printTreeFileName);
	}
	
	class Cluster
	{
		public HashSet<Integer> wordSet;
		public final int ID;
		
		public Cluster(int id)
		{
			this.ID=id;
			wordSet=new HashSet<Integer>();
		}
		
		public void addWord(int word)
		{
			wordSet.add(word);
		}
		
		public void addAllWord(Cluster c)
		{
			wordSet.addAll(c.wordSet);
		}
		
		public double computeScore(Cluster c, double scores[][])
		{
			double score=0.0;
			int num=0;
			for (int w1 : wordSet)
			{
				for (int w2 : c.wordSet)
				{
					if (w1!=w2)
					{
						score+=scores[w1][w2];
						num++;
					}
				}
			}
			return score/num;
		}
	}
	
	class Edge implements Comparable<Edge>
	{
		public int c1,c2;
		public double weight;
		
		public Edge(int c1, int c2, double weight)
		{
			this.c1=c1;
			this.c2=c2;
			this.weight=weight;
		}
		
		public boolean equals(Edge e)
		{
			return (this.c1==e.c1 && this.c2==e.c2);
		}
		
		public int compareTo(Edge e)
		{
			return -Double.compare(this.weight, e.weight);
		}
	}
}
