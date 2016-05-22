package yang.weiwei.preprocess;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.IOException;

import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class Tokenizer
{
	private TokenizerME tokenizer;
	
	/**
	 * Tokenize documents in a corpus file
	 * @param srcFileName Corpus file name
	 * @param destFileName Result file name
	 * @throws IOException IOException
	 */
	public void tokenizeFile(String srcFileName, String destFileName) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(srcFileName));
		BufferedWriter bw=new BufferedWriter(new FileWriter(destFileName));
		String line,tokens[];
		while ((line=br.readLine())!=null)
		{
			tokens=tokenize(line);
			for (int i=0; i<tokens.length; i++)
			{
				if (tokens[i].length()==0) continue;
				bw.write(tokens[i]+" ");
			}
			bw.newLine();
		}
		br.close();
		bw.close();
	}
	
	/**
	 * Tokenize a sentence
	 * @param sentence The sentence to be tokenized
	 * @return Tokenized sentence
	 */
	public String[] tokenize(String sentence)
	{
		return tokenizer.tokenize(sentence);
	}
	
	/**
	 * Initialize with default English model
	 * @throws IOException IOException
	 */
	public Tokenizer() throws IOException
	{
		this(PreprocessCfg.tokenModelFileName);
	}
	
	/**
	 * Initialize with user-provided model
	 * @param modelFileName Model file name
	 * @throws IOException IOException
	 */
	public Tokenizer(String modelFileName) throws IOException
	{
		tokenizer=new TokenizerME(new TokenizerModel(new FileInputStream(modelFileName)));
	}
}
