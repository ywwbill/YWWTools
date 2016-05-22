package yang.weiwei.lda;

public class LDASynParam
{
	//for topic model
	public final double alpha=0.5;
	public final double _alpha=0.5;
	public final double beta=0.01;
	public final int numTopics=10;
	public final int numVocab=100;
	public final int numDocs=500;
	public final int docLength=1000;
	
	//for rtm
	public final double nu=1.0;
	public final boolean negEdge=false;
	
	//for hinge loss
	public final double c=1.0;
	
	//for slda
	public final double sigma=1.0;
	
	//for wsbm
	public final boolean directed=false;
	public final double a=0.05;
	public final double b=1.0;
	public final double gamma=1.0;
	public final int numBlocks=20;
}
