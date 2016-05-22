package yang.weiwei.preprocess;

import java.io.File;

import yang.weiwei.cfg.Cfg;

public final class PreprocessCfg
{
	private static String dictPath=Cfg.libPath+"dict"+File.separator;
	
	public static String lemmaDictFileName=dictPath+"en-lemmatizer.txt";
	public static String tokenModelFileName=dictPath+"en-token.bin";
	public static String posModelFileName=dictPath+"en-pos-maxent.bin";
	public static String stopListFileName=dictPath+"stoplist.txt";
}
