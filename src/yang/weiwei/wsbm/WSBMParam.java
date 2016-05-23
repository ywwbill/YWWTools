package yang.weiwei.wsbm;

import yang.weiwei.lda.LDAParam;
import yang.weiwei.util.IOUtil;
import yang.weiwei.util.format.Fourmat;

/**
 * WSBM parameter
 * @author Weiwei Yang
 *
 */
public class WSBMParam
{	
	/** Indicate whether edges are directed (default: false) */
	public boolean directed=false; 
	/** Number of nodes */
	public int numNodes=50;
	/** Number of blocks */
	public int numBlocks=5;
	/** Parameter for edge rates' Gamma prior (default: 1.0) */
	public double a=1.0;
	/** Parameter for edge rates' Gamma prior (default: 1.0) */
	public double b=1.0;
	/** Parameter for edge rates' Gamma prior (default: 1.0)*/
	public double gamma=1.0;
	/** Whether to output log */
	public boolean verbose=true;
	
	public void printParam()
	{
		printParam("");
	}
	
	public void printParam(String prefix)
	{
		IOUtil.println(prefix+"directed: "+directed);
		IOUtil.println(prefix+"#nodes: "+numNodes);
		IOUtil.println(prefix+"#blocks: "+numBlocks);
		IOUtil.println(prefix+"a: "+Fourmat.format(a));
		IOUtil.println(prefix+"b: "+Fourmat.format(b));
		IOUtil.println(prefix+"gamma: "+Fourmat.format(gamma));
		IOUtil.println(prefix+"WSBM verbose: "+verbose);
	}
	
	public WSBMParam()
	{
	}
	
	public WSBMParam(WSBMSynParam param)
	{
		directed=param.directed;
		numNodes=param.numNodes;
		numBlocks=param.numBlocks;
		a=param.a;
		b=param.b;
		gamma=param.gamma;
		verbose=param.verbose;
	}
	
	public WSBMParam(LDAParam ldaParam, int numNodes)
	{
		directed=ldaParam.directed;
		numBlocks=ldaParam.numBlocks;
		a=ldaParam.a;
		b=ldaParam.b;
		gamma=ldaParam.gamma;
		verbose=false;
		this.numNodes=numNodes;
	}
}
