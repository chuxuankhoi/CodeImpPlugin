/**
 * Pair of IJavaElement and action which are used to refactor the element
 * 
 * @author chuxuankhoi
 * 
 */
package codeimp;

import org.eclipse.jdt.core.IJavaElement;

/**
 * @author chuxuankhoi
 * 
 */
public class RefactoringPair {
	public IJavaElement element;
	public String action; // get from IJavaRefactorings
	public Object addition; // required information for the refactoring
							// action, usually is an array of information
}
