package yang.weiwei.preprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class Splitter
{
	private Random random;
	
	public void splitCV(int n, int numFolds, String splitFileName) throws IOException
	{
		int numExamples[]=new int[numFolds];
		for (int i=0; i<numFolds; i++)
		{
			numExamples[i]=n/numFolds;
		}
		for (int i=0; i<n%numFolds; i++)
		{
			numExamples[i]++;
		}
		
		int fold[]=new int[n],next;
		for (int i=0; i<n; i++)
		{
			fold[i]=-1;
		}
		for (int i=0; i<numFolds-1; i++)
		{
			for (int j=0; j<numExamples[i]; j++)
			{
				do
				{
					next=random.nextInt(n);
				}while (fold[next]!=-1);
				fold[next]=i;
			}
		}
		for (int i=0; i<n; i++)
		{
			fold[i]=(fold[i]==-1? numFolds-1 : fold[i]);
		}
		
		BufferedWriter bw=new BufferedWriter(new FileWriter(splitFileName));
		for (int i=0; i<n; i++)
		{
			bw.write(fold[i]+"");
			bw.newLine();
		}
		bw.close();
	}
	
	public void splitDevTest(int n, int numFolds, String oldFoldFileName, String newFoldFileName, double devRatio) throws IOException
	{
		int foldSize[]=new int[numFolds];
		int devSize[]=new int[numFolds];
		int targetDevSize[]=new int[numFolds];
		int folds[]=new int[n];
		boolean dev[]=new boolean[n];
		
		BufferedReader br=new BufferedReader(new FileReader(oldFoldFileName));
		String line;
		for (int i=0; i<n; i++)
		{
			line=br.readLine();
			int fold=Integer.valueOf(line);
			foldSize[fold]++;
			folds[i]=fold;
		}
		br.close();
		
		for (int i=0; i<numFolds; i++)
		{
			targetDevSize[i]=(int)(foldSize[i]*devRatio);
		}
		
		Random random=new Random();
		boolean finish=false;
		int next,fold;
		while (!finish)
		{
			next=random.nextInt(n);
			fold=folds[next];
			while (dev[next] || devSize[fold]>=targetDevSize[fold])
			{
				next=random.nextInt(n);
				fold=folds[next];
			}
			
			dev[next]=true;
			devSize[fold]++;
			finish=true;
			for (int i=0; i<numFolds; i++)
			{
				finish=finish && (devSize[i]>=targetDevSize[i]);
			}
		}
		
		BufferedWriter bw=new BufferedWriter(new FileWriter(newFoldFileName));
		for (int i=0; i<n; i++)
		{
			if (dev[i])
			{
				bw.write(folds[i]+"\t0");
			}
			else
			{
				bw.write(folds[i]+"\t1");
			}
			bw.newLine();
		}
		bw.close();
	}
	
	public void splitCVDevTest(int n, int numFolds, double devRatio, String splitFileName) throws IOException
	{
		int numExamples[]=new int[numFolds];
		for (int i=0; i<numFolds; i++)
		{
			numExamples[i]=n/numFolds;
		}
		for (int i=0; i<n%numFolds; i++)
		{
			numExamples[i]++;
		}
		
		int fold[]=new int[n],next;
		for (int i=0; i<n; i++)
		{
			fold[i]=-1;
		}
		for (int i=0; i<numFolds-1; i++)
		{
			for (int j=0; j<numExamples[i]; j++)
			{
				do
				{
					next=random.nextInt(n);
				}while (fold[next]!=-1);
				fold[next]=i;
			}
		}
		for (int i=0; i<n; i++)
		{
			fold[i]=(fold[i]==-1? numFolds-1 : fold[i]);
		}
		
		int foldSize[]=new int[numFolds];
		int devSize[]=new int[numFolds];
		int targetDevSize[]=new int[numFolds];
		boolean dev[]=new boolean[n];
		
		for (int i=0; i<n; i++)
		{
			foldSize[fold[i]]++;
		}
		
		for (int i=0; i<numFolds; i++)
		{
			targetDevSize[i]=(int)(foldSize[i]*devRatio);
		}
		
		boolean finish=false;
		int tempFold;
		while (!finish)
		{
			next=random.nextInt(n);
			tempFold=fold[next];
			while (dev[next] || devSize[tempFold]>=targetDevSize[tempFold])
			{
				next=random.nextInt(n);
				tempFold=fold[next];
			}
			
			dev[next]=true;
			devSize[tempFold]++;
			finish=true;
			for (int i=0; i<numFolds; i++)
			{
				finish=finish && (devSize[i]>=targetDevSize[i]);
			}
		}
		
		BufferedWriter bw=new BufferedWriter(new FileWriter(splitFileName));
		for (int i=0; i<n; i++)
		{
			if (dev[i])
			{
				bw.write(fold[i]+"\t0");
			}
			else
			{
				bw.write(fold[i]+"\t1");
			}
			bw.newLine();
		}
		bw.close();
	}
	
	public void splitTrainTest(int n, double testRatio, String resultFileName) throws IOException
	{
		int numSelected=0,numSelect=(int)Math.round(n*testRatio);
		boolean selected[]=new boolean[n];
		while (numSelected<numSelect)
		{
			int next=random.nextInt(n);
			while (selected[next])
			{
				next=random.nextInt(n);
			}
			selected[next]=true;
			numSelected++;
		}
		
		BufferedWriter bw=new BufferedWriter(new FileWriter(resultFileName));
		for (int i=0; i<n; i++)
		{
			bw.write((selected[i]? "1" : "0"));
			bw.newLine();
		}
		bw.close();
	}
	
	public Splitter()
	{
		random=new Random();
	}
}
