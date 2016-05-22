package yang.weiwei.wsbm;

import yang.weiwei.lda.LDASynParam;

public class WSBMSynParam
{
	public boolean directed=false;
	public int numNodes=500;
	public int numBlocks=20;
	public double a=2.0;
	public double b=1.0;
	public double gamma=0.1;
	public boolean verbose=true;
	
	public WSBMSynParam()
	{
	}
	
	public WSBMSynParam(LDASynParam ldaParam)
	{
		directed=ldaParam.directed;
		numNodes=ldaParam.numDocs;
		numBlocks=ldaParam.numBlocks;
		a=ldaParam.a;
		b=ldaParam.b;
		gamma=ldaParam.gamma;
		verbose=false;
	}
}
