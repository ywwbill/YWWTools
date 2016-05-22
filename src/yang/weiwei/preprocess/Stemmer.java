package yang.weiwei.preprocess;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import yang.weiwei.util.PorterStemmer;

public class Stemmer
{
	private PorterStemmer stemmer;
	
	/**
	 * Stem documents in a corpus file
	 * @param srcFileName Unstemmed corpus file name
	 * @param destFileName Result corpus file name
	 * @throws IOException IOException
	 */
	public void stemFile(String srcFileName, String destFileName) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(srcFileName));
		BufferedWriter bw=new BufferedWriter(new FileWriter(destFileName));
		String line,seg[];
		while ((line=br.readLine())!=null)
		{
			seg=line.split(" ");
			for (int i=0; i<seg.length; i++)
			{
				if (seg[i].length()==0) continue;
				bw.write(stem(seg[i])+" ");
			}
			bw.newLine();
		}
		br.close();
		bw.close();
	}
	
	/**
	 * Stem a single word
	 * @param word The word to be stemmed
	 * @return Stemmed word
	 */
	public String stem(String word)
	{
		return stemmer.stem(word);
	}
	
	public Stemmer()
	{
		stemmer=new PorterStemmer();
	}
}
