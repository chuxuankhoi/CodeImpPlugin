package codeimp.graders;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;


/**
 * <p>
 * Judge methods in the way that brother classes use them or not.
 * </p>
 * <p>
 * Brother classes are the classes which have the same super-class with the
 * given class
 * </p>
 * <p>
 * Score is the average score of each method. Score of each method is counted by
 * the fraction of the same method in all methods which have the same
 * declaration.
 * </p>
 * <p>
 * In this version, the methods are compared in the simplest way that they use
 * the same source code. In fact, in order to compare the method, we can see
 * another way like comparing the output set.
 * </p>
 * 
 * <strong>Note: prevent the error that 2 methods have the same declaration in 1
 * class (error of refactoring actions) - the method should be counted</strong>
 * 
 * @author Chu Xuan Khoi
 * 
 */
public class SharedMethodsInChildren extends SharedMethodsAbstract implements IGrader {

//	private static final double DUP_THRESHOLD = 0.2;

	public SharedMethodsInChildren(IType type) {
		super(type);
	}

	@Override
	public double getScore() {
		// Get all subclasses of the given class
		ITypeHierarchy hierachy = null;
		try {
			hierachy = type.newTypeHierarchy(new NullProgressMonitor());
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		if (hierachy == null) {
			return 0;
		}
		IType[] subclasses = hierachy.getSubtypes(type);
		if (subclasses.length == 0) {
			return 0;
		}

		double sumMethod = getScoreOfGroup(subclasses);

		return sumMethod;// / allDiffMethods.size();
	}
}
