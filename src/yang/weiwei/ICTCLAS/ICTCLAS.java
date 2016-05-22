package yang.weiwei.ICTCLAS;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import ICTCLAS.I3S.AC.ICTCLAS50;
import yang.weiwei.cfg.Cfg;
import yang.weiwei.util.IOUtil;

/**
 * 
 * Chinese POS-tagger
 *
 */

public final class ICTCLAS
{
	private ICTCLAS50 parser;
	
	/**
	 * Import user's dictionary
	 * @param dictPath Dictionary file name
	 * @throws UnsupportedEncodingException UnsupportedEncodingException
	 */
	public void importUserDictFile(String dictPath) throws UnsupportedEncodingException
	{
		parser.ICTCLAS_ImportUserDictFile(dictPath.getBytes("UTF-8"), 0);
	}
	
	/**
	 * Tag POS for a corpus
	 * @param srcFileName Untagged corpus file name
	 * @param destFileName Result corpus file name
	 * @throws IOException IOException
	 */
	public void fileProcess(String srcFileName, String destFileName) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(srcFileName));
		BufferedWriter bw=new BufferedWriter(new FileWriter(destFileName));
		String line;
		int lineNum=0;
		while ((line=br.readLine())!=null)
		{
			lineNum++;
			if (lineNum%100==0) System.out.println(lineNum);
			bw.write(paragraphProcess(line));
			bw.newLine();
			bw.flush();
		}
		br.close();
		bw.close();
	}
	
	/**
	 * Tag POS for a document
	 * @param paragraph Untagged document
	 * @return Tagged document
	 * @throws UnsupportedEncodingException UnsupportedEncodingException
	 */
	public String paragraphProcess(String paragraph) throws UnsupportedEncodingException
	{
		byte paragraphByte[]=parser.ICTCLAS_ParagraphProcess(paragraph.getBytes("UTF-8"), 0, 1);
		return new String(paragraphByte, 0, paragraphByte.length, "UTF-8");
	}
	
	public ICTCLAS() throws UnsupportedEncodingException
	{
		parser=new ICTCLAS50();
		String ictclasDataPath=Cfg.libPath+"ictclas"+File.separator;
		if (!parser.ICTCLAS_Init(ictclasDataPath.getBytes("UTF-8")))
		{
			IOUtil.println("ICTCLAS Init Failed.");
		}
	}
	
	public void finalize()
	{
		parser.ICTCLAS_Exit();
	}
}
