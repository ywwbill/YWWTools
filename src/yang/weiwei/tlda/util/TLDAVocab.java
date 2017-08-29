package yang.weiwei.tlda.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class TLDAVocab
{
	private static ArrayList<String> id2vocab;
	private static HashMap<String, Integer> vocab2id;
	
	static
	{
		id2vocab=new ArrayList<String>();
		vocab2id=new HashMap<String, Integer>();
	}
	
	public static void readVocab(String vocabFileName) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(vocabFileName));
		String line;
		while ((line=br.readLine())!=null)
		{
			addVocab(line);
		}
		br.close();
	}
	
	public static void readVocabFromCorpus(String corpusFileName) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(corpusFileName));
		String line,seg[];
		while ((line=br.readLine())!=null)
		{
			seg=line.split(" ");
			for (int i=0; i<seg.length; i++)
			{
				if (seg[i].length()==0) continue;
				addVocab(seg[i]);
			}
		}
		br.close();
	}
	
	public static void writeVocab(String vocabFileName) throws IOException
	{
		BufferedWriter bw=new BufferedWriter(new FileWriter(vocabFileName));
		for (String word : id2vocab)
		{
			bw.write(word);
			bw.newLine();
		}
		bw.close();
	}
	
	public static void addVocab(String word)
	{
		if (vocab2id.containsKey(word)) return;
		vocab2id.put(word, id2vocab.size());
		id2vocab.add(word);
	}
	
	public static int vocabSize()
	{
		return id2vocab.size();
	}
	
	public static String getVocab(int id)
	{
		return id2vocab.get(id);
	}
	
	public static int getID(String vocab)
	{
		return (vocab2id.containsKey(vocab)? vocab2id.get(vocab):-1);
	}
	
	public static boolean containsVocab(String vocab)
	{
		return vocab2id.containsKey(vocab);
	}
}
