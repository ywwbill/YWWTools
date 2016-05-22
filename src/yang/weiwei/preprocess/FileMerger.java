package yang.weiwei.preprocess;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class FileMerger
{
	private BufferedWriter bw;
	
	public void merge(List<String> fileNames) throws IOException
	{
		for (String fileName : fileNames)
		{
			merge(fileName);
		}
	}
	
	public void merge(String fileNames[]) throws IOException
	{
		for (int i=0; i<fileNames.length; i++)
		{
			merge(fileNames[i]);
		}
	}
	
	public void merge(String fileName) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(fileName));
		String line;
		while ((line=br.readLine())!=null)
		{
			bw.write(line);
			bw.newLine();
			bw.flush();
		}
		br.close();
	}
	
	public void finalize() throws IOException
	{
		bw.flush();
		bw.close();
	}
	
	public FileMerger(String destFileName) throws IOException
	{
		bw=new BufferedWriter(new FileWriter(destFileName));
	}
}
