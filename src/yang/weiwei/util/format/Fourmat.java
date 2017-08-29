package yang.weiwei.util.format;

import java.text.NumberFormat;
import java.util.Arrays;

/**
 * Format a double number with four fraction digits
 * @author Weiwei Yang
 *
 */
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
	
	/**
	 * Format a double array with four fraction digits
	 * @param nums Double array to format
	 * @return Formatted String
	 */
	public static String format(double nums[])
	{
		String formatStr[]=new String[nums.length];
		for (int i=0; i<nums.length; i++)
		{
			formatStr[i]=format(nums[i]);
		}
		return Arrays.toString(formatStr);
	}
	
	/**
	 * Format a double number with four fraction digits
	 * @param num Double number to format
	 * @return Formatted String
	 */
	public static String format(double num)
	{
		return format.format(num);
	}
}
