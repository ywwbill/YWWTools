package yang.weiwei.preprocess;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.IOException;

import opennlp.tools.lemmatizer.SimpleLemmatizer;

public class Lemmatizer
{
	private SimpleLemmatizer lemmatizer;
	
	/**
	 * Lemmatize documents in a corpus file
	 * @param srcFileName Tokenized and POS-tagged corpus file name
	 * @param destFileName Result file name
	 * @throws IOException IOException
	 */
	public void lemmatizeFile(String srcFileName, String destFileName) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(srcFileName));
		BufferedWriter bw=new BufferedWriter(new FileWriter(destFileName));
		String line,seg[],segseg[];
		while ((line=br.readLine())!=null)
		{
			seg=line.split(" ");
			for (int i=0; i<seg.length; i++)
			{
				if (seg.length==0) continue;
				segseg=seg[i].split("_");
				if (segseg.length!=2) continue;
				bw.write(lemmatize(segseg[0], segseg[1])+" ");
			}
			bw.newLine();
		}
		br.close();
		bw.close();
	}
	
	/**
	 * Lemmatize a single word
	 * @param word Word to be lemmatized
	 * @param pos Word's POS
	 * @return Lemmatized word
	 */
	public String lemmatize(String word, String pos)
	{
		return lemmatizer.lemmatize(word, pos);
	}
	
	/**
	 * Initialize with default English dictionary
	 * @throws IOException IOException
	 */
	public Lemmatizer() throws IOException
	{
		this(PreprocessCfg.lemmaDictFileName);
	}
	
	/**
	 * Initialize with user-provided dictionary
	 * @param dictFileName Dictionary file name
	 * @throws IOException IOException
	 */
	public Lemmatizer(String dictFileName) throws IOException
	{
		lemmatizer=new SimpleLemmatizer(new FileInputStream(dictFileName));
	}
}
