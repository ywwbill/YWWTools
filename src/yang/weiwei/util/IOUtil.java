package yang.weiwei.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class IOUtil
{
	public static void print(Object obj)
	{
		System.out.print(obj);
	}
	
	public static void println(Object obj)
	{
		System.out.println(obj);
	}
	
	public static void println()
	{
		System.out.println();
	}
	
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
	
	public static void readMatrix(BufferedReader br, double matrix[][]) throws IOException
	{
		readMatrix(br, matrix, matrix.length, matrix[0].length);
	}
	
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
	
	public static void readVector(BufferedReader br, double vector[]) throws IOException
	{
		readVector(br, vector, vector.length);
	}
	
	public static void readVector(BufferedReader br, double vector[], int dim) throws IOException
	{
		String line;
		for (int i=0; i<dim; i++)
		{
			line=br.readLine();
			vector[i]=Double.valueOf(line);
		}
	}
	
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
	
	public static void writeVector(BufferedWriter bw, double vector[]) throws IOException
	{
		for (int i=0; i<vector.length; i++)
		{
			bw.write(vector[i]+"");
			bw.newLine();
		}
	}
	
	public static void writeVector(BufferedWriter bw, int vector[]) throws IOException
	{
		for (int i=0; i<vector.length; i++)
		{
			bw.write(vector[i]+"");
			bw.newLine();
		}
	}
	
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
