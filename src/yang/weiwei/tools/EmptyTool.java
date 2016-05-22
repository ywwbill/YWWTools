package yang.weiwei.tools;

import java.io.IOException;

public class EmptyTool extends ToolInterface
{	
	public void parseCommand(String[] args)
	{
		
	}

	protected boolean checkCommand()
	{
		return true;
	}

	public void execute() throws IOException
	{
		
	}

	public void printHelp()
	{
		println("Arguments for Tools:");
		println("\t--help [optional]: Print help information.");
	}
}
