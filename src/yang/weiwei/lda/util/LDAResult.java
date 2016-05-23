package yang.weiwei.lda.util;

import java.util.ArrayList;

import yang.weiwei.util.IOUtil;
import yang.weiwei.util.format.Fourmat;

/**
 * LDA result collector
 * @author Weiwei Yang
 *
 */
public class LDAResult
{
	public static final int LOGLIKELIHOOD=0;
	public static final int PERPLEXITY=1;
	public static final int BLOCKLOGLIKELIHOOD=2;
	public static final int PLR=3;
	public static final int ERROR=4;
	
	private ArrayList<Double> logLikelihood;
	private ArrayList<Double> perplexity;
	private ArrayList<Double> blockLogLikelihood;
	private ArrayList<Double> plr;
	private ArrayList<Double> error;
	
	/**
	 * Add a result
	 * @param resultType Result type
	 * @param result Result value
	 */
	public void add(int resultType, double result)
	{
		switch (resultType)
		{
		case LOGLIKELIHOOD: logLikelihood.add(result); break;
		case PERPLEXITY: perplexity.add(result); break;
		case BLOCKLOGLIKELIHOOD: blockLogLikelihood.add(result); break;
		case PLR: plr.add(result); break;
		case ERROR: error.add(result); break;
		}
	}
	
	/**
	 * Print average result
	 * @param message Optional message
	 * @param resultType Result type
	 */
	public void printResults(String message, int resultType)
	{
		switch (resultType)
		{
		case LOGLIKELIHOOD: printAvg(message, logLikelihood); break;
		case PERPLEXITY: printAvg(message, perplexity); break;
		case BLOCKLOGLIKELIHOOD: printAvg(message, blockLogLikelihood); break;
		case PLR: printAvg(message, plr); break;
		case ERROR: printAvg(message, error); break;
		}
	}
	
	private static void printAvg(String message, ArrayList<Double> values)
	{
		if (values.size()==0) return;
		double avg=0.0;
		for (double value : values)
		{
			IOUtil.println(value);
			avg+=value;
		}
		avg/=(double)values.size();
		IOUtil.println(message+" "+Fourmat.format(avg));
	}
	
	public LDAResult()
	{
		logLikelihood=new ArrayList<Double>();
		perplexity=new ArrayList<Double>();
		blockLogLikelihood=new ArrayList<Double>();
		plr=new ArrayList<Double>();
		error=new ArrayList<Double>();
	}
}
