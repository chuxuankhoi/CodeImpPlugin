/**
 * 
 */
package codeimp.graders;

import org.eclipse.jdt.core.IMethod;

/**
 * @author chuxuankhoi
 *
 */
public class LongMethodGrader implements IGrader {
	
	private IMethod method;

	/**
	 * @param method
	 */
	public LongMethodGrader(IMethod method) {
		this.method = method;
	}

	/* (non-Javadoc)
	 * @see codeimp.graders.IGrader#getScore()
	 */
	@Override
	public double getScore() {
		// TODO Implement long method scoring
		return 0;
	}

}
