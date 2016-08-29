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
	
	public static String sldaDataPath=dataPath+"slda"+File.separator;
	public static String sldaVocabFileName=sldaDataPath+"vocab";
	public static String sldaTrainCorpusFileName=sldaDataPath+"corpus-train";
	public static String sldaTestCorpusFileName=sldaDataPath+"corpus-test";
	public static String sldaTrainLabelFileName=sldaDataPath+"labels-train";
	public static String sldaTestLabelFileName=sldaDataPath+"labels-test";
	
	public static String rtmDataPath=dataPath+"rtm"+File.separator;
	public static String rtmVocabFileName=rtmDataPath+"vocab";
	public static String rtmTrainCorpusFileName=rtmDataPath+"corpus-train";
	public static String rtmTestCorpusFileName=rtmDataPath+"corpus-test";
	public static String rtmTrainLinkFileName=rtmDataPath+"links-train";
	public static String rtmTestLinkFileName=rtmDataPath+"links-test";
	public static String rtmTrainClusterFileName=rtmDataPath+"clusters-train";
	
	public static String stldaDataPath=dataPath+"st-lda"+File.separator;
	public static String stldaVocabFileName=stldaDataPath+"vocab";
	public static String stldaLongCorpusFileName=stldaDataPath+"corpus-news";
	public static String stldaShortCorpusFileName=stldaDataPath+"corpus-tweets";
	
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
