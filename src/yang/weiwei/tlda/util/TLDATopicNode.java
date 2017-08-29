package yang.weiwei.tlda.util;

import java.util.List;
import java.util.Stack;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import yang.weiwei.util.IOUtil;

/**
 * A node in the topic
 * @author Yang Weiwei
 *
 */
public class TLDATopicNode
{
	private ArrayList<TLDATopicNode> children;
	private double weight;
	private double pathLogProb;
	private int sampledCounts;
	private TLDATopicNode father;
	private int leafNodeNo;
	
	/**
	 * Initialize the node
	 */
	public TLDATopicNode()
	{
		children=new ArrayList<TLDATopicNode>();
		weight=0.0;
		pathLogProb=0.0;
		sampledCounts=0;
		father=null;
		leafNodeNo=-1;
	}
	
	/**
	 * Copy the tree structure (w/o weights) from a tree prior
	 * @param priorRoot Tree prior object
	 */
	public void copyTree(TLDATopicPriorNode priorRoot)
	{
		LinkedList<TLDATopicNode> queue=new LinkedList<TLDATopicNode>();
		queue.add(this);
		LinkedList<TLDATopicPriorNode> priorQueue=new LinkedList<TLDATopicPriorNode>();
		priorQueue.add(priorRoot);
		
		TLDATopicPriorNode tempPrior=null;
		TLDATopicNode temp=null;
		while (!queue.isEmpty() && !priorQueue.isEmpty())
		{
			temp=queue.poll();
			tempPrior=priorQueue.poll();
			temp.leafNodeNo=tempPrior.getLeafNodeNo();
			for (int i=0; i<tempPrior.getNumChildren(); i++)
			{
				TLDATopicNode child=new TLDATopicNode();
				temp.addChild(child);
				queue.add(child);
				priorQueue.add(tempPrior.getChild(i));
			}
		}
	}
	
	/**
	 * Load the tree from a file 
	 * @param treeFileName The file name of the tree
	 * @return A built tree's root node
	 * @throws IOException IOException
	 */
	public static TLDATopicNode fromPrettyPrint(String treeFileName) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(treeFileName));
		TLDATopicNode root=fromPrettyPrint(br);
		br.close();
		return root;
	}
	
	/**
	 * Load the tree from a BufferedReader
	 * @param br The BufferedReader for the tree file
	 * @return A built tree's root node
	 * @throws IOException IOException
	 */
	public static TLDATopicNode fromPrettyPrint(BufferedReader br) throws IOException
	{
		Stack<TLDATopicNode> stack=new Stack<TLDATopicNode>();
		Stack<Integer> level=new Stack<Integer>();
		String line;
		TLDATopicNode root=null;
		while ((line=br.readLine())!=null)
		{
			if (line.equals("##")) break;
			int lv=line.indexOf('-');
			while (!stack.isEmpty() && level.peek()>=lv)
			{
				stack.pop();
				level.pop();
			}
			TLDATopicNode newNode=new TLDATopicNode();
			newNode.weight=Double.valueOf(line.substring(lv+1));
			if (!stack.isEmpty())
			{
				stack.peek().addChild(newNode);
			}
			else
			{
				root=newNode;
			}
			stack.push(newNode);
			level.push(lv);
		}
		root.computeLeafNodeNo();
		return root;
	}
	
	/**
	 * Write the tree (w/ weights) to a BufferedWriter
	 * @param bw The BufferedWriter to write the three
	 * @throws IOException IOException
	 */
	public void prettyPrint(BufferedWriter bw) throws IOException
	{
		Stack<String> indent=new Stack<String>();
		Stack<Boolean> last=new Stack<Boolean>();
		Stack<Integer> cid=new Stack<Integer>();
		Stack<TLDATopicNode> stack=new Stack<TLDATopicNode>();
		indent.push("");
		last.push(true);
		cid.add(-1);
		stack.push(this);
		
		TLDATopicNode temp;
		while (!stack.isEmpty())
		{
			temp=stack.peek();
			if (cid.peek()==-1)
			{
				bw.write(indent.peek());
				if (last.peek())
				{
					bw.write("\\-");
				}
				else
				{
					bw.write("|-");
				}
				bw.write(temp.weight+"");
				bw.newLine();
			}
			
			cid.push(cid.pop()+1);
			if (cid.peek()>=temp.getNumChildren())
			{
				indent.pop();
				last.pop();
				cid.pop();
				stack.pop();
			}
			else
			{
				if (last.peek())
				{
					indent.push(indent.peek()+"  ");
				}
				else
				{
					indent.push(indent.peek()+"| ");
				}
				last.push(cid.peek()==temp.getNumChildren()-1);
				stack.push(temp.getChild(cid.peek()));
				cid.push(-1);
			}
		}
		bw.write("##");
		bw.newLine();
	}
	
	/**
	 * Write the tree (w/ weights) to a file
	 * @param treeFileName The file to store the tree
	 * @throws IOException IOException
	 */
	public void prettyPrint(String treeFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(treeFileName));
		prettyPrint(bw);
		bw.close();
	}
	
	protected void prettyPrint()
	{
		Stack<String> indent=new Stack<String>();
		Stack<Boolean> last=new Stack<Boolean>();
		Stack<Integer> cid=new Stack<Integer>();
		Stack<TLDATopicNode> stack=new Stack<TLDATopicNode>();
		indent.push("");
		last.push(true);
		cid.add(-1);
		stack.push(this);
		
		TLDATopicNode temp;
		while (!stack.isEmpty())
		{
			temp=stack.peek();
			if (cid.peek()==-1)
			{
				IOUtil.print(indent.peek());
				if (last.peek())
				{
					IOUtil.print("\\-");
				}
				else
				{
					IOUtil.print("|-");
				}
				IOUtil.println(temp.weight);
			}
			
			cid.push(cid.pop()+1);
			if (cid.peek()>=temp.getNumChildren())
			{
				indent.pop();
				last.pop();
				cid.pop();
				stack.pop();
			}
			else
			{
				if (last.peek())
				{
					indent.push(indent.peek()+"  ");
				}
				else
				{
					indent.push(indent.peek()+"| ");
				}
				last.push(cid.peek()==temp.getNumChildren()-1);
				stack.push(temp.getChild(cid.peek()));
				cid.push(-1);
			}
		}
		IOUtil.println("##");
	}
	
	private void computeLeafNodeNo()
	{
		Stack<Integer> cid=new Stack<Integer>();
		Stack<TLDATopicNode> stack=new Stack<TLDATopicNode>();
		cid.add(-1);
		stack.push(this);
		
		int numLeafNodes=0;
		TLDATopicNode temp;
		while (!stack.isEmpty())
		{
			temp=stack.peek();
			if (cid.peek()==-1)
			{
				if (temp.isLeaf())
				{
					temp.leafNodeNo=numLeafNodes;
					numLeafNodes++;
				}
				else
				{
					temp.leafNodeNo=-1;
				}
			}
			
			cid.push(cid.pop()+1);
			if (cid.peek()>=temp.getNumChildren())
			{
				cid.pop();
				stack.pop();
			}
			else
			{
				stack.push(temp.getChild(cid.peek()));
				cid.push(-1);
			}
		}
	}
	
	/**
	 * Assign the path which leads to this node to a token
	 */
	public void assignPath()
	{
		TLDATopicNode temp=this;
		while (temp!=null)
		{
			temp.sampledCounts++;
			temp=temp.father;
		}
	}
	
	/**
	 * Unassign the path which leads to this node from a token
	 */
	public void unassignPath()
	{
		TLDATopicNode temp=this;
		while (temp!=null)
		{
			temp.sampledCounts--;
			temp=temp.father;
		}
	}
	
	/**
	 * Compute the posterior distribution of child nodes
	 * @param beta The Dirichlet hyper-parameter
	 */
	public void computeChildrenDist(double beta)
	{
		LinkedList<TLDATopicNode> queue=new LinkedList<TLDATopicNode>();
		queue.add(this);
		TLDATopicNode temp;
		while (!queue.isEmpty())
		{
			temp=queue.poll();
			temp.weight=(temp.isRoot()? 1.0 : (temp.sampledCounts+beta)/
					(temp.father.sampledCounts+temp.father.getNumChildren()*beta));
			for (int i=0; i<temp.getNumChildren(); i++)
			{
				queue.add(temp.getChild(i));
			}
		}
	}
	
	/**
	 * Compute the log probability of the path which leads to this node.
	 */
	public void computePathLogProb()
	{
		LinkedList<TLDATopicNode> queue=new LinkedList<TLDATopicNode>();
		queue.add(this);
		TLDATopicNode temp;
		while (!queue.isEmpty())
		{
			temp=queue.poll();
			temp.pathLogProb=(temp.isRoot()? 0.0 : temp.father.pathLogProb+Math.log(temp.weight));
			for (int i=0; i<temp.getNumChildren(); i++)
			{
				queue.add(temp.getChild(i));
			}
		}
	}
	
	/**
	 * Compute the log probability of the path which leads to this node in the training process
	 * @param beta The Dirichlet hyper-parameter
	 * @return The log probability of the paths leading to this node
	 */
	public double computePathLogProb(double beta) // for leaf node in sampling
	{
		TLDATopicNode temp=this;
		double logProb=0.0;
		while (temp.father!=null)
		{
			logProb+=Math.log((temp.sampledCounts+beta)/
					(temp.father.sampledCounts+temp.father.getNumChildren()*beta));
			temp=temp.father;
		}
		return logProb;
	}
	
	/**
	 * Add a child node to the current node
	 * @param child The child node to add
	 */
	public void addChild(TLDATopicNode child)
	{
		child.setFather(this);
		children.add(child);
	}
	
	/**
	 * Set this node's father node
	 * @param newFather The father node
	 */
	public void setFather(TLDATopicNode newFather)
	{
		father=newFather;
	}
	
	/**
	 * Judge whether this node is the root node
	 * @return true if current node is the root node; false otherwise
	 */
	public boolean isRoot()
	{
		return father==null;
	}
	
	/**
	 * Judge whether this node is a leaf node
	 * @return true if current node is a leaf node; false otherwise
	 */
	public boolean isLeaf()
	{
		return children.size()==0;
	}
	
	/**
	 * Get this node's number of child nodes
	 * @return The number of child nodes
	 */
	public int getNumChildren()
	{
		return children.size();
	}
	
	/**
	 * Get one of this node's child nodes
	 * @param no The number of the child nodes
	 * @return The corresponding child nodes; null if out of range
	 */
	public TLDATopicNode getChild(int no)
	{
		return (no>=0 && no<children.size()? children.get(no) : null);
	}
	
	/**
	 * Get this node's father node
	 * @return Current node's father node
	 */
	public TLDATopicNode getFather()
	{
		return father;
	}
	
	/**
	 * Get the leaf node following a given path
	 * @param path The given path
	 * @return The final node of the path
	 */
	public TLDATopicNode getNode(List<Integer> path)
	{
		TLDATopicNode temp=this;
		for (int i=0; i<path.size(); i++)
		{
			if (path.get(i)<0 || path.get(i)>=temp.children.size()) return null;
			temp=temp.getChild(path.get(i));
		}
		return temp;
	}
	
	/**
	 * Get the log probability of the path which leads to this node
	 * @return The log probability of path leading to this node
	 */
	public double getPathLogProb()
	{
		return pathLogProb;
	}
	
	/**
	 * Get the weight of this node in its father node's child node distribution
	 * @return The weight of this node in its father node's child node distribution
	 */
	public double getWeight()
	{
		return weight;
	}
	
	/**
	 * Get the number of tokens assigned to this node and its child nodes
	 * @return The number of tokens assigned to this node and its child nodes
	 */
	public int getSampledCounts()
	{
		return sampledCounts;
	}
	
	/**
	 * Get the global leaf node number
	 * @return Global leaf node number
	 */
	public int getLeafNodeNo()
	{
		return leafNodeNo;
	}
}
