package yang.weiwei.tools;

import java.io.IOException;

import yang.weiwei.Tools;

public class ToolTest
{
	public static void testLDA() throws IOException
	{
		String args[]={"--tool", "lda", "--model", "lex-wsb-med-lda", "--help", "--label", "labels-train", "--vocab", "vocab", "--corpus", "corpus-train", "--trained-model", "model"};
		Tools.main(args);
	}
	
	public static void testWSBM() throws IOException
	{
		String args[]={"--tool", "wsbm", "--nodes", "50", "--blocks", "5", "--graph", "graph", "--output", "output"};
		Tools.main(args);
	}
	
	public static void testSCC() throws IOException
	{
		String args[]={"--tool", "scc", "--nodes", "1615", "--graph", "graph", "--output", "cluster"};
		Tools.main(args);
	}
	
	public static void testTokenizer() throws IOException
	{
		String args[]={"--tool", "tokenizer"};
		Tools.main(args);
	}
	
	public static void testCorpusConverter() throws IOException
	{
		String args[]={"--tool", "corpus-converter", "--to-word", "--word-corpus", "corpus", "--vocab", "vocab", "--index-corpus", "index"};
		Tools.main(args);
	}
	
	public static void testICTCLAS() throws IOException
	{
		String args[]={"--tool", "ICTCLAS", "--help"};
		Tools.main(args);
	}
	
	public static void testTools() throws IOException
	{
		String args[]={"--help", "--tool"};
		Tools.main(args);
	}
	
	public static void main(String args[]) throws IOException
	{
		testLDA();
	}
}
