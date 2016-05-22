package yang.weiwei.wsbm;

import java.io.File;

import yang.weiwei.cfg.Cfg;

public final class WSBMCfg
{
	private static String dataPath=Cfg.dataPath+"wsbm"+File.separator;
	private static String synPath=dataPath+"synthetic"+File.separator;
	
	public static int numIters=100;
	
	public static String graphFileName=dataPath+"graph";
	public static String synParamFileName=synPath+"syn-param";
	public static String synModelFileName=synPath+"syn-model";
	public static String synGraphFileName=synPath+"syn-graph";
}
