package yang.weiwei.scc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Strongly Connected Component
 * @author Weiwei Yang
 *
 */
public class SCC
{
	protected int size;
	protected ArrayList<ArrayList<Integer>> edges;
	protected HashSet<String> visited;
	protected ArrayList<ArrayList<Integer>> clusters;
	
	protected int dfn[];
	protected int lowLink[];
	
	protected int stack[];
	protected int sp;
	
	protected int step;
	
	/**
	 * Read graph from graph file
	 * @param graphFileName Graph file name
	 * @throws IOException IOException
	 */
	public void readGraph(String graphFileName) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(graphFileName));
		String line,seg[];
		while ((line=br.readLine())!=null)
		{
			seg=line.split("\t");
			int u=Integer.valueOf(seg[0]);
			int v=Integer.valueOf(seg[1]);
			edges.get(u).add(v);
			edges.get(v).add(u);
		}
		br.close();
	}
	
	protected void dfs(int start)
	{	
		int tempStack[]=new int[size];
		int tempSp=0;
		tempStack[tempSp]=start;
		boolean added=true;
		
		while (tempSp>=0)
		{
			int v=tempStack[tempSp];
			
			if (added)
			{
				step++;
				dfn[v]=step;
				lowLink[v]=step;
				
				sp++;
				stack[sp]=v;
			}
			
			added=false;
			
			for (int u : edges.get(v))
			{
				if (dfn[u]==-1)
				{
					tempSp++;
					tempStack[tempSp]=u;
					added=true;
					break;
				}
				else
				{
					if (dfn[u]<dfn[v] && check(u))
					{
						lowLink[v]=Math.min(lowLink[u], lowLink[v]);
					}
				}
			}
			
			if (added) continue;
			
			if (tempSp>0)
			{
				lowLink[tempStack[tempSp-1]]=Math.min(lowLink[tempStack[tempSp-1]], lowLink[v]);
			}
			
			
			if (dfn[v]==lowLink[v])
			{
				ArrayList<Integer> temp=new ArrayList<Integer>();
				do
				{
					temp.add(stack[sp]);
					sp--;
				}while (stack[sp+1]!=v);
				clusters.add(temp);
			}
			
			tempSp--;
		}
	}
	
	protected boolean check(int u)
	{
		for (int i=0; i<=sp; i++)
		{
			if (stack[i]==u)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Run SCC algorithm
	 */
	public void cluster()
	{
		step=-1;
		sp=-1;
		
		for (int i=0; i<size; i++)
		{
			if (dfn[i]==-1)
			{
				dfs(i);
			}
		}
	}
	
	/**
	 * Write strongly connected components to file
	 * @param clusterFileName Component file name
	 * @throws IOException IOException
	 */
	public void writeClusters(String clusterFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(clusterFileName));
		for (ArrayList<Integer> temp : clusters)
		{
			for (int i : temp)
			{
				bw.write(i+" ");
			}
			bw.newLine();
		}
		bw.close();
	}
	
	/**
	 * Initialize SCC with number of nodes
	 * @param size Number of nodes
	 */
	public SCC(int size)
	{
		this.size=size;
		edges=new ArrayList<ArrayList<Integer>>();
		visited=new HashSet<String>();
		clusters=new ArrayList<ArrayList<Integer>>();
		
		dfn=new int[size];
		lowLink=new int[size];
		
		stack=new int[size];
		sp=-1;
		
		step=-1;
		
		for (int i=0; i<size; i++)
		{
			edges.add(new ArrayList<Integer>());
			dfn[i]=-1;
			lowLink[i]=-1;
		}
	}
	
	public static void main(String args[]) throws IOException
	{
		SCC scc=new SCC(1615);
		scc.readGraph(SCCCfg.graphFileName);
		scc.cluster();
		scc.writeClusters(SCCCfg.clusterFileName);
	}
}
