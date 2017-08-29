package yang.weiwei.util;

import java.util.ArrayList;

import cc.mallet.util.Randoms;

public class MathUtil
{
	public static Randoms randoms=new Randoms();
	
	public static int selectDiscrete(double score[])
	{
		double sum=0.0;
		for (int i=0; i<score.length; i++)
		{
			sum+=score[i];
		}
		
		double sample=randoms.nextDouble()*sum;
		int index=-1;
		while (sample>0 && index<score.length-1)
		{
			index++;
			sample-=score[index];
		}
		
		return index;
	}
	
	public static int selectLogDiscrete(double score[])
	{
		double max=selectMax(score);
		for (int i=0; i<score.length; i++)
		{
			score[i]=Math.exp(score[i]-max);
		}
		return selectDiscrete(score);
	}
	
	public static double selectMax(double score[])
	{
		double max=Double.NEGATIVE_INFINITY;
		for (int i=0; i<score.length; i++)
		{
			if (score[i]>max)
			{
				max=score[i];
			}
		}
		return max;
	}
	
	public static double average(double nums[])
	{
		double avg=0.0;
		for (int i=0; i<nums.length; i++)
		{
			avg+=nums[i];
		}
		return avg/(double)nums.length;
	}
	
	public static double average(ArrayList<Double> nums)
	{
		double avg=0.0;
		for (int i=0; i<nums.size(); i++)
		{
			avg+=nums.get(i);
		}
		return avg/(double)nums.size();
	}
	
	public static double sum(double nums[])
	{
		double result=0.0;
		for (int i=0; i<nums.length; i++)
		{
			result+=nums[i];
		}
		return result;
	}
	
	public static double matrixAbsDiff(double m1[][], double m2[][])
	{
		if (m1.length!=m2.length) return Double.MAX_VALUE;
		int numElements=0;
		double diff=0.0;
		for (int i=0; i<m1.length; i++)
		{
			if (m1[i].length!=m2[i].length) return Double.MAX_VALUE;
			numElements+=m1[i].length;
			for (int j=0; j<m1[i].length; j++)
			{
				diff+=Math.abs(m1[i][j]-m2[i][j]);
			}
		}
		if (numElements==0) return 0.0;
		return diff/numElements;
	}
	
	public static double matrixKLDivergence(double m1[][], double m2[][])
	{
		if (m1.length!=m2.length) return Double.MAX_VALUE;
		double avgKL=0.0;
		for (int i=0; i<m1.length; i++)
		{
			if (m1[i].length!=m2[i].length) return Double.MAX_VALUE;
			avgKL+=vectorKLDivergence(m1[i], m2[i]);
		}
		return avgKL/m1.length;
	}
	
	public static double vectorAbsDiff(double v1[], double v2[])
	{
		if (v1.length!=v2.length) return Double.MAX_VALUE;
		double diff=0.0;
		for (int i=0; i<v1.length; i++)
		{
			diff+=Math.abs(v1[i]-v2[i]);
		}
		return diff/v1.length;
	}
	
	public static double vectorKLDivergence(double v1[], double v2[])
	{
		if (v1.length!=v2.length) return Double.MAX_VALUE;
		double kl=0.0;
		for (int i=0; i<v1.length; i++)
		{
			kl+=v1[i]*Math.log(v1[i]/v2[i]);
		}
		return kl;
	}
	
	//useful functions
	public static double logFactorial(int n)
	{
		double result=0.0;
		for (int i=1; i<=n; i++)
		{
			result+=Math.log((double)i);
		}
		return result;
	}
	
	public static double sigmoid(double x)
	{
		return 1.0/(1.0+Math.exp(-1.0*x));
	}
	
	public static double sqr(double x)
	{
		return x*x;
	}
	
	//generation from distribution(s)
	public static double sampleIG(double muIG, double lambdaIG)
	{
		double v=randoms.nextGaussian();   
		double y=v*v;
		double x=muIG+(muIG*muIG*y)/(2*lambdaIG)-(muIG/(2*lambdaIG))*Math.sqrt(4*muIG*lambdaIG*y + muIG*muIG*y*y);
		double test=randoms.nextDouble();
		if (test<=(muIG)/(muIG+x))
		{
			return x;
		}
		return (muIG*muIG)/x;
	}
	
	public static double[] sampleDir(double alpha, int size)
	{
		double alphaVector[]=new double[size];
		for (int i=0; i<size; i++)
		{
			alphaVector[i]=alpha;
		}
		return sampleDir(alphaVector);
	}
	
	public static double[] sampleDir(double alpha[])
	{
		double v[]=new double[alpha.length];
		for (int i=0; i<alpha.length; i++)
		{
			v[i]=randoms.nextGamma(alpha[i]);
			while (v[i]==0.0)
			{
				v[i]=randoms.nextGamma(alpha[i]);
			}
		}
		double sumV=sum(v);
		for (int i=0; i<alpha.length; i++)
		{
			v[i]/=sumV;
		}
		return v;
	}
	
	// compute log (a+b) given log a and log b
	public static double logSum(double logA, double logB)
	{
		double logMax=Math.max(logA, logB);
		return Math.log(Math.exp(logA-logMax)+Math.exp(logB-logMax))+logMax;
	}
	
	public static double[][] invert(double a[][]) 
	{
		int n = a.length;
		double x[][] = new double[n][n];
		double b[][] = new double[n][n];
		int index[] = new int[n];
		for (int i=0; i<n; ++i) 
		{
			b[i][i] = 1;
		} 
	 
		// Transform the matrix into an upper triangle
		gaussian(a, index);
	 
		// Update the matrix b[i][j] with the ratios stored
		for (int i=0; i<n-1; ++i)
		{
			for (int j=i+1; j<n; ++j)
			{
				for (int k=0; k<n; ++k)
				{
					b[index[j]][k]-= a[index[j]][i]*b[index[i]][k];
				}
			}
		}

		// Perform backward substitutions
		for (int i=0; i<n; ++i) 
		{
			x[n-1][i] = b[index[n-1]][i]/a[index[n-1]][n-1];
			for (int j=n-2; j>=0; --j) 
			{
				x[j][i] = b[index[j]][i];
				for (int k=j+1; k<n; ++k) 
				{
					x[j][i] -= a[index[j]][k]*x[k][i];
				}
				x[j][i] /= a[index[j]][j];
			}
		}
		
		return x;
	}
	
	// Method to carry out the partial-pivoting Gaussian
	// elimination.  Here index[] stores pivoting order.
	public static void gaussian(double a[][], int index[]) 
	{
		int n = index.length;
		double c[] = new double[n];
	 
		// Initialize the index
		for (int i=0; i<n; ++i) 
		{
			index[i] = i;
		}
	 
		// Find the rescaling factors, one from each row
		for (int i=0; i<n; ++i) 
		{
			double c1 = 0;
			for (int j=0; j<n; ++j) 
			{
				double c0 = Math.abs(a[i][j]);
				if (c0 > c1) c1 = c0;
			}
			c[i] = c1;
		}
	 
		// Search the pivoting element from each column
		int k = 0;
		for (int j=0; j<n-1; ++j) 
		{
			double pi1 = 0;
			for (int i=j; i<n; ++i) 
			{
				double pi0 = Math.abs(a[index[i]][j]);
				pi0 /= c[index[i]];
				if (pi0 > pi1) 
				{
					pi1 = pi0;
					k = i;
				}
			}
	 
			// Interchange rows according to the pivoting order
			int itmp = index[j];
			index[j] = index[k];
			index[k] = itmp;
			for (int i=j+1; i<n; ++i) 	
			{
				double pj = a[index[i]][j]/a[index[j]][j];
				
				// Record pivoting ratios below the diagonal
				a[index[i]][j] = pj;
	 
				// Modify other elements accordingly
				for (int l=j+1; l<n; ++l)
				{
					 a[index[i]][l] -= pj*a[index[j]][l];
				}  
			}
		}
	}
}
