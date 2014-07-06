/**
 * 
 */
package codeimp.graders;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import codeimp.CodeImpUtils;

/**
 * @author chuxuankhoi
 *
 */
public class LongMethodGrader implements IGrader {
	
	private final static int MAX_LOC = 100;

	private IMethod method;

	/**
	 * @param method
	 */
	public LongMethodGrader(IMethod method) {
		this.method = method;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see codeimp.graders.IGrader#getScore()
	 */
	@Override
	public double getScore() {
		// TODO Implement long method scoring
		String body;
		try {
			body = CodeImpUtils.getBody(method);
		} catch (JavaModelException e) {
			e.printStackTrace();
			return 0;
		}
		int loc = countLOC(body);
		System.out.println("LoC: " + loc);
		if (loc <= MAX_LOC) {
			return 0;
		} else {
			return ((double) loc - (double) MAX_LOC) / (double) MAX_LOC;
		}
	}

	private int countLOC(String body) {
		String[] lines = body.split("\n");
		int count = 0;
		for (String line : lines) {
			line = line.replaceAll("[\\s{}]", "");
			if (line.equals("") || line.equals("else") || line.equals("case:")
					|| line.equals("default:")) {
				continue;
			} else {
				count++;
			}
		}
		return count;
	}

}
