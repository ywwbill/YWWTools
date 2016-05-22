package yang.weiwei.util.format;

import java.text.NumberFormat;

public class Fourmat
{
	private static NumberFormat format;
	
	static
	{
		format=NumberFormat.getInstance();
		format.setMaximumFractionDigits(4);
		format.setMinimumFractionDigits(4);
		format.setGroupingUsed(false);
	}
	
	public static String format(double num)
	{
		return format.format(num);
	}
}
