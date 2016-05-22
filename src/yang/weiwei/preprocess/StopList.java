package yang.weiwei.preprocess;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

public class StopList
{
	public HashSet<String> stopWordSet;
	
	/**
	 * Check whether a word is stop word
	 * @param word Word to be checked
	 * @return true if the word is a stop word, false otherwise
	 */
	public boolean contains(String word)
	{
		return stopWordSet.contains(word);
	}
	
	/**
	 * Remove stop words in a corpus file
	 * @param srcFileName Corpus file name
	 * @param destFileName Result file name
	 * @throws IOException IOException
	 */
	public void removeStopWords(String srcFileName, String destFileName) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(srcFileName));
		BufferedWriter bw=new BufferedWriter(new FileWriter(destFileName));
		String line,seg[];
		while ((line=br.readLine())!=null)
		{
			seg=line.split(" ");
			for (int i=0; i<seg.length; i++)
			{
				if (seg[i].length()==0 || contains(seg[i])) continue;
				bw.write(seg[i]+" ");
			}
			bw.newLine();
		}
		br.close();
		bw.close();
	}
	
	/**
	 * Add stemmed stop words to stoplist
	 */
	public void addStemmedWords()
	{
		Stemmer stemmer=new Stemmer();
		HashSet<String> stemmedWordSet=new HashSet<String>();
		for (String word : stopWordSet)
		{
			stemmedWordSet.add(stemmer.stem(word));
		}
		for (String word : stemmedWordSet)
		{
			stopWordSet.add(word);
		}
	}
	
	/**
	 * Initialize with default English stop words
	 * @throws IOException IOException
	 */
	public StopList() throws IOException
	{
		this(PreprocessCfg.stopListFileName);
	}
	
	/**
	 * Initialize with user-provided stop word dictionary
	 * @param dictFileName Dictionary file name
	 * @throws IOException IOException
	 */
	public StopList(String dictFileName) throws IOException
	{
		stopWordSet=new HashSet<String>();
		BufferedReader br=new BufferedReader(new FileReader(dictFileName));
		String line;
		while ((line=br.readLine())!=null)
		{
			stopWordSet.add(line);
		}
		br.close();
	}
}
