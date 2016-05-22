package yang.weiwei.lda;

import java.io.File;

import yang.weiwei.cfg.Cfg;

public final class LDACfg
{	
	private static String dataPath=Cfg.dataPath+"lda"+File.separator;
	private static String modelPath=dataPath+"model"+File.separator;
	private static String synPath=dataPath+"synthetic"+File.separator;
	
	public static int numTrainIters=20;
	public static int numTestIters=20;
	
	public static String vocabFileName=dataPath+"vocab";
	
	public static String rawCorpusFileName=dataPath+"corpus-raw";
	public static String corpusFileName=dataPath+"corpus";
	public static String trainCorpusFileName=dataPath+"corpus-train";
	public static String testCorpusFileName=dataPath+"corpus-test";
	
	public static String trainLabelFileName=dataPath+"labels-train";
	public static String predLabelFileName=dataPath+"labels-pred";
	
	public static String trainClusterFileName=dataPath+"cluster-train";
	public static String testClusterFileName=dataPath+"cluster-test";
	
	public static String trainGraphFileName=dataPath+"graph-train";
	public static String testGraphFileName=dataPath+"graph-test";
	
	//output
	public static String getModelFileName(String modelName)
	{
		return modelPath+modelName+"-model";
	}
	
	//synthetic data
	public static String getSynParamFileName(String modelName)
	{
		return synPath+modelName+"-syn-param";
	}
	
	public static String getSynModelFileName(String modelName)
	{
		return synPath+modelName+"-syn-model";
	}
	
	public static String getSynLabelFileName(String modelName)
	{
		return synPath+modelName+"-syn-labels";
	}
	
	public static String getSynCorpusFileName(String modelName)
	{
		return synPath+modelName+"-syn-corpus";
	}
	
	public static String getSynGraphFileName(String modelName)
	{
		return synPath+modelName+"-syn-graph";
	}
	
	public static String getSynBlockGraphFileName(String modelName)
	{
		return synPath+modelName+"-syn-block-graph";
	}
	
	public static String getSynWSBMParamFileName(String modelName)
	{
		return synPath+modelName+"-syn-wsbm-param";
	}
	
	public static String getSynWSBMModelFileName(String modelName)
	{
		return synPath+modelName+"-syn-wsbm-model";
	}
}
