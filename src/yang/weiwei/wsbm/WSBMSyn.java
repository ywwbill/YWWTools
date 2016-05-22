package yang.weiwei.wsbm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import cc.mallet.util.Randoms;
import yang.weiwei.util.MathUtil;
import yang.weiwei.util.IOUtil;

public class WSBMSyn
{
	protected final WSBMSynParam param;
	protected static Randoms randoms;
	
	protected double omega[][];
	protected double mu[];
	protected int y[];
	protected int graph[][];
	
	protected int blockMatch[];
	
	protected void generateOmega()
	{
		omega=new double[param.numBlocks][param.numBlocks];
		int startPos;
		for (int l1=0; l1<param.numBlocks; l1++)
		{
			startPos=(param.directed? 0:l1);
			for (int l2=startPos; l2<param.numBlocks; l2++)
			{
				omega[l1][l2]=randoms.nextGamma(param.a, param.b);
				if (!param.directed)
				{
					omega[l2][l1]=omega[l1][l2];
				}
			}
		}
	}
	
	protected void generateMu()
	{
		mu=MathUtil.sampleDir(param.gamma, param.numBlocks);
	}
	
	protected void generateY()
	{
		y=new int[param.numNodes];
		for (int node=0; node<param.numNodes; node++)
		{
			y[node]=MathUtil.selectDiscrete(mu);
		}
	}
	
	public void generateGraph()
	{
		generateOmega();
		generateMu();
		generateY();
		
		graph=new int[param.numNodes][param.numNodes];
		int startPos;
		for (int n1=0; n1<param.numNodes; n1++)
		{
			startPos=(param.directed? 0:n1+1);
			for (int n2=startPos; n2<param.numNodes; n2++)
			{
				if (n1==n2) continue;
				graph[n1][n2]=randoms.nextPoisson(omega[y[n1]][y[n2]]);
				if (!param.directed)
				{
					graph[n2][n1]=graph[n1][n2];
				}
			}
		}
	}
	
	public void writeGraph(String graphFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(graphFileName));
		int startPos;
		for (int n1=0; n1<param.numNodes; n1++)
		{
			startPos=(param.directed? 0:n1+1);
			for (int n2=startPos; n2<param.numNodes; n2++)
			{
				if (graph[n1][n2]>0 && n1!=n2)
				{
					bw.write(n1+"\t"+n2+"\t"+graph[n1][n2]);
					bw.newLine();
				}
			}
		}
		bw.close();
	}
	
	public void compareParams(WSBM wsbm)
	{
		matchBlocks(mu, wsbm.mu);
		compareOmega(blockMatch, omega, wsbm.omega);
		compareMu(blockMatch, mu, wsbm.mu);
	}
	
	public WSBMSyn readParams(String synParamFileName) throws IOException
	{
		WSBMSyn wsbmSyn=new WSBMSyn(param);
		BufferedReader br=new BufferedReader(new FileReader(synParamFileName));
		IOUtil.readMatrix(br, wsbmSyn.omega=new double[param.numBlocks][param.numBlocks]);
		IOUtil.readVector(br, wsbmSyn.mu=new double[param.numBlocks]);
		br.close();
		return wsbmSyn;
	}
	
	public void compareParams(String synParamFileName, String synModelFileName) throws IOException
	{		
		WSBMSyn syn1=readParams(synParamFileName);
		WSBMSyn syn2=readParams(synModelFileName);
		matchBlocks(syn1.mu, syn2.mu);
		compareOmega(blockMatch, syn1.omega, syn2.omega);
		compareMu(blockMatch, syn1.mu, syn2.mu);
	}
	
	protected void compareOmega(int blockMatch[], double omega[][], double wsbmOmega[][])
	{
		double matchOmega[][]=new double[param.numBlocks][param.numBlocks];
		for (int l1=0; l1<param.numBlocks; l1++)
		{
			for (int l2=0; l2<param.numBlocks; l2++)
			{
				matchOmega[l1][l2]=wsbmOmega[blockMatch[l1]][blockMatch[l2]];
			}
		}
		IOUtil.println("Omega Diff: "+MathUtil.matrixAbsDiff(omega, matchOmega));
	}
	
	protected void compareMu(int blockMatch[], double mu[], double wsbmMu[])
	{
		double matchMu[]=new double[param.numBlocks];
		for (int l=0; l<param.numBlocks; l++)
		{
			matchMu[l]=wsbmMu[blockMatch[l]];
		}
		IOUtil.println("Mu KL-D: "+MathUtil.vectorKLDivergence(mu, matchMu));
	}
	
	protected void matchBlocks(double mu[], double wsbmMu[])
	{
		int sortedMuPos[]=new int[param.numBlocks];
		for (int l=0; l<param.numBlocks; l++)
		{
			sortedMuPos[l]=-1;
		}
		for (int i=0; i<param.numBlocks; i++)
		{
			double max=Double.MIN_VALUE;
			int maxPos=-1;
			for (int l=0; l<param.numBlocks; l++)
			{
				if (sortedMuPos[l]==-1 && mu[l]>max)
				{
					max=mu[l];
					maxPos=l;
				}
			}
			sortedMuPos[maxPos]=i;
		}
		
		int sortedWSBMMuPos[]=new int[param.numBlocks];
		for (int l=0; l<param.numBlocks; l++)
		{
			sortedWSBMMuPos[l]=-1;
		}
		for (int i=0; i<param.numBlocks; i++)
		{
			double max=Double.MIN_VALUE;
			int maxPos=-1;
			for (int l=0; l<param.numBlocks; l++)
			{
				if (sortedWSBMMuPos[l]==-1 && wsbmMu[l]>max)
				{
					max=wsbmMu[l];
					maxPos=l;
				}
			}
			sortedWSBMMuPos[maxPos]=i;
		}
		
		for (int l=0; l<param.numBlocks; l++)
		{
			blockMatch[l]=-1;
			for (int i=0; i<param.numBlocks; i++)
			{
				if (sortedMuPos[l]==sortedWSBMMuPos[i])
				{
					blockMatch[l]=i;
					break;
				}
			}
			assert(blockMatch[l]!=-1);
		}
	}
	
	public void writeParam(String paramFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(paramFileName));
		IOUtil.writeMatrix(bw, omega);
		IOUtil.writeVector(bw, mu);
		bw.close();
	}
	
	public void writeSynModel(WSBM wsbm, String synModelFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(synModelFileName));
		IOUtil.writeMatrix(bw, wsbm.omega);
		IOUtil.writeVector(bw, wsbm.mu);
		bw.close();
	}
	
	public double[] getMu()
	{
		return mu;
	}
	
	public double[][] getOmega()
	{
		return omega;
	}
	
	public int getBlockAssign(int node)
	{
		return y[node];
	}
	
	public int getBlockMatch(int block)
	{
		return blockMatch[block];
	}
	
	static
	{
		randoms=new Randoms();
	}
	
	public WSBMSyn(WSBMSynParam parameters)
	{
		param=parameters;
		blockMatch=new int[param.numBlocks];
		for (int l=0; l<param.numBlocks; l++)
		{
			blockMatch[l]=l;
		}
	}
	
	public static void main(String args[]) throws IOException
	{
		WSBMSynParam param=new WSBMSynParam();
		WSBMSyn wsbmSyn=new WSBMSyn(param);
		wsbmSyn.generateGraph();
		wsbmSyn.writeGraph(WSBMCfg.synGraphFileName);
		wsbmSyn.writeParam(WSBMCfg.synParamFileName);
		
		WSBM wsbm=new WSBM(new WSBMParam(param));
		wsbm.readGraph(WSBMCfg.synGraphFileName);
		wsbm.init();
		wsbm.sample(WSBMCfg.numIters);
		
		wsbmSyn.compareParams(wsbm);
		wsbmSyn.writeSynModel(wsbm, WSBMCfg.synModelFileName);
//		wsbmSyn.compareParams(WSBMCfg.synParamFileName, WSBMCfg.synModelFileName);
	}
}
