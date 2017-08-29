package yang.weiwei.tlda.util;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;

import yang.weiwei.util.IOUtil;

public class TLDATopicPriorNode
{
	private final String word;
	private final int wordID;
	private ArrayList<TLDATopicPriorNode> children;
	private TLDATopicPriorNode father;
	private int leafNodeNo;
	private int numLeafNodes;
	
	public TLDATopicPriorNode(String word, int wordID, TLDATopicPriorNode father)
	{
		this.word=word;
		this.wordID=wordID;
		children=new ArrayList<TLDATopicPriorNode>();
		this.father=father;
		this.leafNodeNo=-1;
	}
	
	public TLDATopicPriorNode(String word, int wordID)
	{
		this(word, wordID, null);
	}
	
	public TLDATopicPriorNode()
	{
		this("", -1);
	}
	
	public TLDATopicPriorNode(TLDATopicPriorNode node)
	{
		this(node.word, node.wordID);
	}
	
	public static TLDATopicPriorNode fromPrettyPrint(String treeFileName) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(treeFileName));
		TLDATopicPriorNode root=fromPrettyPrint(br);
		br.close();
		return root;
	}
	
	public static TLDATopicPriorNode fromPrettyPrint(BufferedReader br) throws IOException
	{
		Stack<TLDATopicPriorNode> stack=new Stack<TLDATopicPriorNode>();
		Stack<Integer> level=new Stack<Integer>();
		String line,seg[];
		TLDATopicPriorNode root=null;
		while ((line=br.readLine())!=null)
		{
			if (line.equals("##")) break;
			int lv=line.indexOf('-');
			while (!stack.isEmpty() && level.peek()>=lv)
			{
				stack.pop();
				level.pop();
			}
			TLDATopicPriorNode newNode;
			if (lv==line.length()-1)
			{
				newNode=new TLDATopicPriorNode();
			}
			else
			{
				seg=line.substring(lv+1).split(":");
				newNode=new TLDATopicPriorNode(seg[1], Integer.valueOf(seg[0]));
			}
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
	
	public void prettyPrint(BufferedWriter bw) throws IOException
	{
		Stack<String> indent=new Stack<String>();
		Stack<Boolean> last=new Stack<Boolean>();
		Stack<Integer> cid=new Stack<Integer>();
		Stack<TLDATopicPriorNode> stack=new Stack<TLDATopicPriorNode>();
		indent.push("");
		last.push(true);
		cid.add(-1);
		stack.push(this);
		
		TLDATopicPriorNode temp;
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
				if (temp.word.length()>0)
				{
					bw.write(temp.wordID+":"+temp.word);
				}
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
	
	public void prettyPrint(String treeFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(treeFileName));
		prettyPrint(bw);
		bw.close();
	}
	
	public void prettyPrint()
	{
		Stack<String> indent=new Stack<String>();
		Stack<Boolean> last=new Stack<Boolean>();
		Stack<Integer> cid=new Stack<Integer>();
		Stack<TLDATopicPriorNode> stack=new Stack<TLDATopicPriorNode>();
		indent.push("");
		last.push(true);
		cid.add(-1);
		stack.push(this);
		
		TLDATopicPriorNode temp;
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
				if (temp.word.length()>0)
				{
					IOUtil.print(temp.wordID+":"+temp.word);
				}
				IOUtil.println();
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
		Stack<TLDATopicPriorNode> stack=new Stack<TLDATopicPriorNode>();
		cid.add(-1);
		stack.push(this);
		
		int numLeafNodes=0;
		TLDATopicPriorNode temp;
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
		
		this.numLeafNodes=numLeafNodes;
	}
	
	public void addChild(TLDATopicPriorNode child)
	{
		child.setFather(this);
		children.add(child);
	}
	
	public void setFather(TLDATopicPriorNode newFather)
	{
		father=newFather;
	}
	
	public boolean isRoot()
	{
		return father==null;
	}
	
	public boolean isLeaf()
	{
		return children.size()==0;
	}
	
	public int getNumChildren()
	{
		return children.size();
	}
	
	public TLDATopicPriorNode getChild(int no)
	{
		return (no>=0 && no<children.size()? children.get(no) : null);
	}
	
	public String getWord()
	{
		return word;
	}
	
	public int getWordID()
	{
		return wordID;
	}
	
	public TLDATopicPriorNode getFather()
	{
		return father;
	}
	
	public int getLeafNodeNo()
	{
		return leafNodeNo;
	}
	
	public int getNumLeafNodes()
	{
		return numLeafNodes;
	}
	
	public TLDATopicPriorNode getNode(List<Integer> path)
	{
		TLDATopicPriorNode temp=this;
		for (int i=0; i<path.size(); i++)
		{
			if (path.get(i)<0 || path.get(i)>=temp.children.size()) return null;
			temp=temp.getChild(path.get(i));
		}
		return temp;
	}
	
	public void getVocabPathMap(List<List<List<Integer>>> map)
	{
		getVocabPathMap(map, new ArrayList<Integer>());
	}
	
	private void getVocabPathMap(List<List<List<Integer>>> map, List<Integer> tempPath)
	{
		Stack<TLDATopicPriorNode> stack=new Stack<TLDATopicPriorNode>();
		stack.push(this);
		tempPath.add(-1);
		TLDATopicPriorNode temp;
		while (!stack.isEmpty())
		{
			temp=stack.peek();
			int pos=stack.size()-1;
			tempPath.set(pos, tempPath.get(pos)+1);
			if (tempPath.get(pos)>=temp.getNumChildren())
			{
				tempPath.remove(pos);
				if (temp.isLeaf())
				{
					List<Integer> path=new ArrayList<Integer>();
					path.addAll(tempPath);
					map.get(temp.wordID).add(path);
					
				}
				stack.pop();
			}
			else
			{
				stack.push(temp.getChild(tempPath.get(pos)));
				tempPath.add(-1);
			}
		}
	}
	
	public String getWordPath()
	{
		StringBuilder sb=new StringBuilder("");
		TLDATopicPriorNode temp=this;
		while (temp!=null)
		{
			if (temp.word.length()>0)
			{
				if (sb.length()==0)
				{
					sb.append(temp.word);
				}
				else
				{
					sb.insert(0, temp.word+":");
				}
			}
			temp=temp.father;
		}
		return sb.toString();
	}
}
