package codeimp.graders;

import java.util.ArrayList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import codeimp.CodeImpUtils;

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
public class SharedMethodsInChildren implements IGrader {

	private static final double DUP_THRESHOLD = 0.2;

	private IType type;

	public SharedMethodsInChildren(IType type) {
		this.type = type;
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

		// Get all methods which have different declaration
		ArrayList<IMethod> allDiffMethods = new ArrayList<IMethod>();
		ArrayList<IMethod> allMethods = new ArrayList<IMethod>();
		for (IType t : subclasses) {
			IMethod[] methods = null;
			try {
				methods = t.getMethods();
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			if (methods == null || methods.length == 0) {
				continue;
			}
			for (IMethod m : methods) {
				allMethods.add(m);
				if (!isCountedDeclaration(m, allDiffMethods)) {
					allDiffMethods.add(m);
				}
			}
		}
		if (allDiffMethods.size() == 0
				|| allDiffMethods.size() == allMethods.size()) {
			return 0;
		}

		// Score the methods
		double sumMethod = 0.0;
		for (IMethod m1 : allDiffMethods) {
			int dupNum = 0;
			int simNum = 0;
			// Get all methods that have the same declaration with m1
			ArrayList<IMethod> m1Sim = new ArrayList<IMethod>();
			for (IMethod m2 : allMethods) {
				if (m2.isSimilar(m1)) {
					simNum++;
					m1Sim.add(m2);
				}
			}
			if (simNum == 0) {
				continue;
			}
			// Get the duplicate methods in the methods that have the same
			// declaration with m1. The function count not only the methods that
			// are duplication of m1 but also other duplications
			String curComp = null;
			try {
				curComp = CodeImpUtils.getBody(m1Sim.get(0));
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			if(curComp == null) {
				continue;
			}
			String nextComp = String.format(curComp);
			while (curComp != null) {
				int curDup = -1;
				ArrayList<IMethod> used = new ArrayList<IMethod>();
				for (IMethod m : m1Sim) {
					try {
						String mBody = CodeImpUtils.getBody(m);
						if(mBody == null) {
							continue;
						}
						if (mBody.equals(curComp)) {
							curDup++;
							used.add(m);
						} else if (nextComp.equals(curComp)) {
							nextComp = CodeImpUtils.getBody(m);
						}
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
				// Delete used methods
				for (IMethod m : used) {
					m1Sim.remove(m);
				}
				dupNum += curDup;
				if (!nextComp.equals(curComp)) {
					curComp = nextComp;
				} else {
					curComp = null;
				}
			}
			double absShared = ((double) dupNum / (double) simNum);
			if (absShared > DUP_THRESHOLD) {
				sumMethod += absShared;
			}
		}

		return sumMethod;// / allDiffMethods.size();
	}

	private boolean isCountedDeclaration(IMethod method,
			ArrayList<IMethod> methodList) {
		for (IMethod m : methodList) {
			if (method.isSimilar(m)) {
				return true;
			}
		}
		return false;
	}
}
