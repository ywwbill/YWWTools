package yang.weiwei.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * IO utilities
 * @author Weiwei Yang
 *
 */
public class IOUtil
{
	/**
	 * Print an object on console
	 * @param obj Object to print
	 */
	public static void print(Object obj)
	{
		System.out.print(obj);
	}
	
	/**
	 * Print an object on console and start a new line
	 * @param obj Object to print
	 */
	public static void println(Object obj)
	{
		System.out.println(obj);
	}
	
	/**
	 * Start a new line
	 */
	public static void println()
	{
		System.out.println();
	}
	
	/**
	 * Print an int matrix to console
	 * @param matrix Int matrix
	 */
	public void printMatrix(int matrix[][])
	{
		for (int i=0; i<matrix.length; i++)
		{
			for (int j=0; j<matrix[i].length; j++)
			{
				IOUtil.print(matrix[i][j]+" ");
			}
			IOUtil.println();
		}
	}
	
	/**
	 * Read a double matrix from a BufferedReader
	 * @param br BufferedReader
	 * @param matrix Double matrix
	 * @throws IOException IOException
	 */
	public static void readMatrix(BufferedReader br, double matrix[][]) throws IOException
	{
		readMatrix(br, matrix, matrix.length, matrix[0].length);
	}
	
	/**
	 * Read a double matrix from a BufferedReader with given dimensions
	 * @param br BufferedReader
	 * @param matrix Double matrix
	 * @param dim1 First dimension
	 * @param dim2 Second dimension
	 * @throws IOException IOException
	 */
	public static void readMatrix(BufferedReader br, double matrix[][], int dim1, int dim2) throws IOException
	{
		String line,seg[];
		for (int i=0; i<dim1; i++)
		{
			line=br.readLine();
			seg=line.split(" ");
			for (int j=0; j<dim2; j++)
			{
				matrix[i][j]=Double.valueOf(seg[j]);
			}
		}
	}
	
	/**
	 * Read a double vector from a BufferedReader
	 * @param br BufferedReader
	 * @param vector Double vector
	 * @throws IOException IOException
	 */
	public static void readVector(BufferedReader br, double vector[]) throws IOException
	{
		readVector(br, vector, vector.length);
	}
	
	/**
	 * Read a double vector from a BufferedReader with given dimension
	 * @param br BufferedReader
	 * @param vector Double vector
	 * @param dim dimension
	 * @throws IOException IOException
	 */
	public static void readVector(BufferedReader br, double vector[], int dim) throws IOException
	{
		String line;
		for (int i=0; i<dim; i++)
		{
			line=br.readLine();
			vector[i]=Double.valueOf(line);
		}
	}
	
	/**
	 * Write an int matrix to a BufferedWriter
	 * @param bw BufferedWriter
	 * @param matrix Int matrix
	 * @throws IOException IOException
	 */
	public static void writeMatrix(BufferedWriter bw, int matrix[][]) throws IOException
	{
		for (int i=0; i<matrix.length; i++)
		{
			for (int j=0; j<matrix[i].length; j++)
			{
				bw.write(matrix[i][j]+" ");
			}
			bw.newLine();
		}
	}
	
	/**
	 * Write a double matrix to a BufferedWriter
	 * @param bw BufferedWriter
	 * @param matrix Double matrix
	 * @throws IOException IOException
	 */
	public static void writeMatrix(BufferedWriter bw, double matrix[][]) throws IOException
	{
		for (int i=0; i<matrix.length; i++)
		{
			for (int j=0; j<matrix[i].length; j++)
			{
				bw.write(matrix[i][j]+" ");
			}
			bw.newLine();
		}
	}
	
	/**
	 * Write a double vector to a BufferedWriter
	 * @param bw BufferedWriter
	 * @param vector Double vector
	 * @throws IOException IOException
	 */
	public static void writeVector(BufferedWriter bw, double vector[]) throws IOException
	{
		for (int i=0; i<vector.length; i++)
		{
			bw.write(vector[i]+"");
			bw.newLine();
		}
	}
	
	/**
	 * Write a int vector to a BufferedWriter
	 * @param bw BufferedWriter
	 * @param vector Int vector
	 * @throws IOException IOException
	 */
	public static void writeVector(BufferedWriter bw, int vector[]) throws IOException
	{
		for (int i=0; i<vector.length; i++)
		{
			bw.write(vector[i]+"");
			bw.newLine();
		}
	}
	
	/**
	 * Copy a file to another position
	 * @param srcFileName Source file name
	 * @param destFileName Destination file name
	 * @throws IOException IOException
	 */
	public static void copyFile(String srcFileName, String destFileName) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(srcFileName));
		BufferedWriter bw=new BufferedWriter(new FileWriter(destFileName));
		String line;
		while ((line=br.readLine())!=null)
		{
			bw.write(line);
			bw.newLine();
		}
		br.close();
		bw.close();
	}
}
