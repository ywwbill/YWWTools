package yang.weiwei.tools;

import java.io.IOException;

import yang.weiwei.util.IOUtil;

public abstract class ToolInterface
{
	protected boolean help=false;
	
	public abstract void parseCommand(String args[]);
	
	protected abstract boolean checkCommand();
	
	public abstract void execute() throws IOException;
	
	public abstract void printHelp();
	
	protected String getArg(String arg, String args[])
	{
		for (int i=0; i<args.length; i++)
		{
			if (args[i].equalsIgnoreCase(arg) && i+1<args.length)
			{
				return args[i+1];
			}
		}
		return "";
	}
	
	protected int getArg(String arg, String args[], int defaultValue)
	{
		String strArg=getArg(arg, args);
		if (strArg.length()==0) return defaultValue;
		return Integer.valueOf(strArg);
	}
	
	protected double getArg(String arg, String args[], double defaultValue)
	{
		String strArg=getArg(arg, args);
		if (strArg.length()==0) return defaultValue;
		return Double.valueOf(strArg);
	}
	
	protected boolean findArg(String arg, String args[], boolean defaultValue)
	{
		for (int i=0; i<args.length; i++)
		{
			if (args[i].equalsIgnoreCase(arg))
			{
				return !defaultValue;
			}
		}
		return defaultValue;
	}
	
	protected static void println(Object obj)
	{
		IOUtil.println(obj);
	}
}
