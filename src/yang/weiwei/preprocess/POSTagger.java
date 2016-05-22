package yang.weiwei.preprocess;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.IOException;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

public class POSTagger
{
	private POSTaggerME tagger;
	
	/**
	 * Tag POS for documents in a tokenized corpus file
	 * @param srcFileName Tokenized corpus file name
	 * @param destFileName Result file name
	 * @throws IOException IOException
	 */
	public void tagFile(String srcFileName, String destFileName) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(srcFileName));
		BufferedWriter bw=new BufferedWriter(new FileWriter(destFileName));
		String line,seg[],pos[];
		while ((line=br.readLine())!=null)
		{
			seg=line.split(" ");
			pos=tag(seg);
			for (int i=0; i<seg.length; i++)
			{
				if (seg[i].length()==0) continue;
				bw.write(seg[i]+"_"+pos[i]+" ");
			}
			bw.newLine();
		}
		br.close();
		bw.close();
	}
	
	/**
	 * Tag POS for a single sentence
	 * @param tokens Tokenized sentence
	 * @return Tagged sentence
	 */
	public String[] tag(String[] tokens)
	{
		return tagger.tag(tokens);
	}
	
	/**
	 * Initialize with default English model
	 * @throws IOException IOException
	 */
	public POSTagger() throws IOException
	{
		this(PreprocessCfg.posModelFileName);
	}
	
	/**
	 * Initialize with user-provided model
	 * @param modelFileName Model file name
	 * @throws IOException IOException
	 */
	public POSTagger(String modelFileName) throws IOException
	{
		tagger=new POSTaggerME(new POSModel(new FileInputStream(modelFileName)));
	}
}
