package yang.weiwei.scc;

import java.io.File;

import yang.weiwei.cfg.Cfg;

public final class SCCCfg
{
	private static String dataPath=Cfg.dataPath+"scc"+File.separator;
	
	public static String graphFileName=dataPath+"graph";
	public static String clusterFileName=dataPath+"cluster";
}
