/**
 * Pair of IJavaElement and action which are used to refactor the element
 * 
 * @author chuxuankhoi
 * 
 */
package codeimp.refactoring;

import org.eclipse.core.resources.IResource;

/**
 * @author chuxuankhoi
 * 
 */
public class RefactoringPair {
	public Object element;
	public String action; // get from IJavaRefactorings
	public Object addition; // required information for the refactoring
							// action, usually is an array of information
	public IResource resource; // resource which contains the element
}
