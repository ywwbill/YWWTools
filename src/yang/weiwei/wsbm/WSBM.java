package yang.weiwei.wsbm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import yang.weiwei.util.MathUtil;
import yang.weiwei.util.format.Fourmat;
import yang.weiwei.util.IOUtil;

/**
 * Weighted Stochastic Block Model
 * @author Weiwei Yang
 *
 */
public class WSBM
{
	/** Parameter object */
	public final WSBMParam param;
	
	protected double omega[][];
	protected double mu[];
	protected double logLikelihood;
	
	protected int numEdges;
	protected ArrayList<HashMap<Integer, Integer>> edgeWeights;
	protected ArrayList<HashMap<Integer, Integer>> reverseEdgeWeights;
	
	protected int blockAssigns[];
	protected int blockBlockWeights[][];
	protected int nodeBlockWeights[][];
	protected int blockNodeWeights[][];
	protected int blockSize[];
	
	protected static Random random;
	
	/**
	 * Read graph file
	 * @param graphFileName Graph file name
	 * @throws IOException IOException
	 */
	public void readGraph(String graphFileName) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(graphFileName));
		String line,seg[];
		int u,v,w;
		while ((line=br.readLine())!=null)
		{
			seg=line.split("\t");
			u=Integer.valueOf(seg[0]);
			v=Integer.valueOf(seg[1]);
			w=(seg.length>=3? Integer.valueOf(seg[2]) : 1);
			edgeWeights.get(u).put(v, w);
			numEdges++;
			if (param.directed)
			{
				reverseEdgeWeights.get(v).put(u, w);
			}
			else
			{
				edgeWeights.get(v).put(u, w);
				numEdges++;
			}
		}
		br.close();
	}
	
	/**
	 * Initialize
	 */
	public void init()
	{
		for (int u=0; u<param.numNodes; u++)
		{
			blockAssigns[u]=random.nextInt(param.numBlocks);
			blockSize[blockAssigns[u]]++;
		}
		
		for (int u=0; u<param.numNodes; u++)
		{
			for (int v : edgeWeights.get(u).keySet())
			{
				nodeBlockWeights[u][blockAssigns[v]]+=edgeWeights.get(u).get(v);
				if (param.directed)
				{
					blockBlockWeights[blockAssigns[u]][blockAssigns[v]]+=edgeWeights.get(u).get(v);
					blockNodeWeights[blockAssigns[u]][v]+=edgeWeights.get(u).get(v);
				}
				else
				{
					if (v>u)
					{
						blockBlockWeights[blockAssigns[u]][blockAssigns[v]]+=edgeWeights.get(u).get(v);
						if (blockAssigns[u]!=blockAssigns[v])
						{
							blockBlockWeights[blockAssigns[v]][blockAssigns[u]]+=edgeWeights.get(u).get(v);
						}
					}
				}
			}
		}
		
		if (param.verbose) printParam();
	}
	
	protected void printParam()
	{
		IOUtil.println("Running "+this.getClass().getSimpleName());
		IOUtil.println("\tedges: "+numEdges);
		param.printParam("\t");
	}
	
	/**
	 * Sample for a certain number of iterations
	 * @param numIters Number of iterations
	 */
	public void sample(int numIters)
	{
		for (int iter=1; iter<=numIters; iter++)
		{
			for (int u=0; u<param.numNodes; u++)
			{
				sampleNode(u);
			}
			computeLogLikelihood();
			if (param.verbose)
			{
				IOUtil.println("<"+iter+">\tLog-Likelihood: "+format(logLikelihood));
			}
		}
	}
	
	/**
	 * Sample a specific node
	 * @param u Node number
	 */
	public void sampleNode(int u)
	{
		int oldBlock=blockAssigns[u];
		blockSize[oldBlock]--;
		if (param.directed)
		{
			for (int l=0; l<param.numBlocks; l++)
			{	
				blockBlockWeights[oldBlock][l]-=nodeBlockWeights[u][l];
				blockBlockWeights[l][oldBlock]-=blockNodeWeights[l][u];
			}
			for (int v : edgeWeights.get(u).keySet())
			{
				blockNodeWeights[oldBlock][v]-=edgeWeights.get(u).get(v);
			}
			for (int v : reverseEdgeWeights.get(u).keySet())
			{
				nodeBlockWeights[v][oldBlock]-=reverseEdgeWeights.get(u).get(v);
			}
		}
		else
		{
			for (int l=0; l<param.numBlocks; l++)
			{	
				blockBlockWeights[oldBlock][l]-=nodeBlockWeights[u][l];
				if (l!=oldBlock)
				{
					blockBlockWeights[l][oldBlock]-=nodeBlockWeights[u][l];
				}
			}
			for (int v : edgeWeights.get(u).keySet())
			{
				nodeBlockWeights[v][oldBlock]-=edgeWeights.get(u).get(v);
			}
		}
		
		double blockScore[]=new double[param.numBlocks];
		
		for (int l=0; l<param.numBlocks; l++)
		{
			blockScore[l]=computeBlockScore(u, l);
		}
		int newBlock=MathUtil.selectLogDiscrete(blockScore);
		
		blockAssigns[u]=newBlock;
		blockSize[newBlock]++;
		if (param.directed)
		{
			for (int l=0; l<param.numBlocks; l++)
			{
				blockBlockWeights[newBlock][l]+=nodeBlockWeights[u][l];
				blockBlockWeights[l][newBlock]+=blockNodeWeights[l][u];
			}
			for (int v : edgeWeights.get(u).keySet())
			{
				blockNodeWeights[newBlock][v]+=edgeWeights.get(u).get(v);
			}
			for (int v : reverseEdgeWeights.get(u).keySet())
			{
				nodeBlockWeights[v][newBlock]+=reverseEdgeWeights.get(u).get(v);
			}
		}
		else
		{
			for (int l=0; l<param.numBlocks; l++)
			{
				blockBlockWeights[newBlock][l]+=nodeBlockWeights[u][l];
				if (l!=newBlock)
				{
					blockBlockWeights[l][newBlock]+=nodeBlockWeights[u][l];
				}
			}
			for (int v : edgeWeights.get(u).keySet())
			{
				nodeBlockWeights[v][newBlock]+=edgeWeights.get(u).get(v);
			}
		}
	}
	
	protected double computeBlockScore(int u, int l)
	{
		double score=0.0;
		
		if (param.directed)
		{
			for (int l1=0; l1<param.numBlocks; l1++)
			{
				if (l==l1) continue;
				
				score+=(blockBlockWeights[l][l1]+param.a)*Math.log(getBlockBlockEdges(l, l1)+param.b)-
						(blockBlockWeights[l][l1]+param.a+nodeBlockWeights[u][l1])*
						Math.log(getBlockBlockEdges(l, l1)+param.b+blockSize[l1]);
				for (int i=0; i<nodeBlockWeights[u][l1]; i++)
				{
					score+=Math.log(blockBlockWeights[l][l1]+param.a+i);
				}
				
				score+=(blockBlockWeights[l1][l]+param.a)*Math.log(getBlockBlockEdges(l1, l)+param.b)-
						(blockBlockWeights[l1][l]+param.a+blockNodeWeights[l1][u])*
						Math.log(getBlockBlockEdges(l1, l)+param.b+blockSize[l1]);
				for (int i=0; i<blockNodeWeights[l1][u]; i++)
				{
					score+=Math.log(blockBlockWeights[l1][l]+param.a+i);
				}
			}
			
			score+=(blockBlockWeights[l][l]+param.a)*Math.log(getBlockBlockEdges(l, l)+param.b)-
					(blockBlockWeights[l][l]+param.a+nodeBlockWeights[u][l]+blockNodeWeights[l][u])*
					Math.log(getBlockBlockEdges(l, l)+param.b+blockSize[l]*2);
			for (int i=0; i<nodeBlockWeights[u][l]+blockNodeWeights[l][u]; i++)
			{
				score+=Math.log(blockBlockWeights[l][l]+param.a+i);
			}
		}
		else
		{
			for (int l1=0; l1<param.numBlocks; l1++)
			{
				score+=(blockBlockWeights[l][l1]+param.a)*Math.log(getBlockBlockEdges(l, l1)+param.b)-
						(blockBlockWeights[l][l1]+param.a+nodeBlockWeights[u][l1])*
						Math.log(getBlockBlockEdges(l, l1)+param.b+blockSize[l1]);
				
				for (int i=0; i<nodeBlockWeights[u][l1]; i++)
				{
					score+=Math.log(blockBlockWeights[l][l1]+param.a+i);
					if (Double.isNaN(score))
					{
						IOUtil.println(blockBlockWeights[l][l1]+param.a+i);
					}
				}
			}
		}
		
		score+=Math.log(blockSize[l]+param.gamma);
		
		return score;
	}
	
	protected int getBlockBlockEdges(int l1, int l2)
	{
		if (l1==l2) return (param.directed? blockSize[l1]*(blockSize[l1]-1) : blockSize[l1]*(blockSize[l1]-1)/2);
		return blockSize[l1]*blockSize[l2];
	}
	
	protected void computeMu()
	{
		for (int l=0; l<param.numBlocks; l++)
		{
			mu[l]=(blockSize[l]+param.gamma)/(param.numNodes+param.numBlocks*param.gamma);
		}
	}
	
	protected void computeOmega()
	{
		for (int l1=0; l1<param.numBlocks; l1++)
		{
			for (int l2=0; l2<param.numBlocks; l2++)
			{
				omega[l1][l2]=(blockBlockWeights[l1][l2]+param.a)/(getBlockBlockEdges(l1, l2)+param.b);
			}
		}
	}
	
	/**
	 * Compute the log likelihood
	 */
	public void computeLogLikelihood()
	{
		computeOmega();
		computeMu();
		
		logLikelihood=0.0;
		int weight;
		for (int u=0; u<param.numNodes; u++)
		{
			logLikelihood+=Math.log(mu[blockAssigns[u]]);
			int start=(param.directed? 0:u+1);
			for (int v=start; v<param.numNodes; v++)
			{
				if (u==v) continue;
				weight=(edgeWeights.get(u).containsKey(v)? edgeWeights.get(u).get(v):0);
				logLikelihood+=weight*Math.log(omega[blockAssigns[u]][blockAssigns[v]])-MathUtil.logFactorial(weight)-omega[blockAssigns[u]][blockAssigns[v]];
			}
		}
	}
	
	/**
	 * Get block edge rate
	 * @param block1 Block number 1
	 * @param block2 Block number 2
	 * @return The edge rate of Blocks 1 and 2
	 */
	public double getBlockEdgeRate(int block1, int block2)
	{
		return omega[block1][block2];
	}
	
	/**
	 * Get block edge rates
	 * @return A matrix of block edge rates
	 */
	public double[][] getBlockEdgeRates()
	{
		return omega.clone();
	}
	
	/**
	 * Get the probability of a block
	 * @param block Block number
	 * @return Probability of given block
	 */
	public double getBlockWeight(int block)
	{
		return mu[block];
	}
	
	/**
	 * Get block distribution
	 * @return Block distribution
	 */
	public double[] getBlockDist()
	{
		return mu.clone();
	}
	
	/**
	 * Get log likelihood
	 * @return Log likelihood
	 */
	public double getLogLikelihood()
	{
		return logLikelihood;
	}
	
	/**
	 * Get number of edges in the graph
	 * @return Number of edges
	 */
	public int getNumEdges()
	{
		return numEdges;
	}
	
	/**
	 * Get the block assignment of a node
	 * @param node Node number
	 * @return Block assignment of given node
	 */
	public int getBlockAssign(int node)
	{
		return blockAssigns[node];
	}
	
	/**
	 * Print blocks to console
	 */
	public void printResults()
	{
		for (int l=0; l<param.numBlocks; l++)
		{
			IOUtil.print("Block "+l+":");
			for (int u=0; u<param.numNodes; u++)
			{
				if (blockAssigns[u]==l)
				{
					IOUtil.print(" "+u);
				}
			}
			IOUtil.println();
		}
	}
	
	/**
	 * Write block assignments to file
	 * @param blockAssignFileName Block assignment file name
	 * @throws IOException IOException
	 */
	public void writeBlockAssign(String blockAssignFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(blockAssignFileName));
		IOUtil.writeVector(bw, blockAssigns);
		bw.close();
	}
	
	/**
	 * Write each block's assigned nodes to file
	 * @param blockFileName Block file name
	 * @throws IOException IOException
	 */
	public void writeBlocks(String blockFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(blockFileName));
		for (int l=0; l<param.numBlocks; l++)
		{
			bw.write("Block "+l+":");
			for (int u=0; u<param.numNodes; u++)
			{
				if (blockAssigns[u]==l)
				{
					bw.write(" "+u);
				}
			}
			bw.newLine();
		}
		bw.close();
	}
	
	protected static String format(double num)
	{
		return Fourmat.format(num);
	}
	
	static
	{
		random=new Random();
	}
	
	/**
	 * Initialize WSBM object with parameters
	 * @param parameters WSBM parameters
	 */
	public WSBM(WSBMParam parameters)
	{
		param=parameters;
		omega=new double[param.numBlocks][param.numBlocks];
		mu=new double[param.numBlocks];
		blockAssigns=new int[param.numNodes];
		blockBlockWeights=new int[param.numBlocks][param.numBlocks];
		nodeBlockWeights=new int[param.numNodes][param.numBlocks];
		blockSize=new int[param.numBlocks];
		numEdges=0;
		edgeWeights=new ArrayList<HashMap<Integer, Integer>>();
		for (int node=0; node<param.numNodes; node++)
		{
			edgeWeights.add(new HashMap<Integer, Integer>());
		}
		
		if (param.directed)
		{
			blockNodeWeights=new int[param.numBlocks][param.numNodes];
			reverseEdgeWeights=new ArrayList<HashMap<Integer, Integer>>();
			for (int node=0; node<param.numNodes; node++)
			{
				reverseEdgeWeights.add(new HashMap<Integer, Integer>());
			}
		}
	}
	
	public static void main(String args[]) throws IOException
	{
		WSBMParam parameters=new WSBMParam();
		WSBM wsbm=new WSBM(parameters);
		wsbm.readGraph(WSBMCfg.graphFileName);
		wsbm.init();
		wsbm.sample(WSBMCfg.numIters);
		wsbm.printResults();
	}
}
