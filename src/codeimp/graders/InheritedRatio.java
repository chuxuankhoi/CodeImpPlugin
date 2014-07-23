package codeimp.graders;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import codeimp.CodeImpUtils;

/**
 * <p>
 * A Stochastic Approach to Automated Design Improvement
 * </p>
 * <p>
 * Judge the effectiveness of the inheritance of the branches related to a given
 * type. The metric is built based on following metrics:
 * </p>
 * <p>
 * - Inherited methods number: the number of methods that the given class
 * inherits from its ancestors.
 * </p>
 * <p>
 * - Available methods number: the number of the methods the given class
 * introduces in its body
 * </p>
 * 
 * <strong>Note: prevent the error that 2 methods have the same declaration in 1
 * class (error of refactoring actions) - the method should be counted</strong>
 * 
 * @author Chu Xuan Khoi
 * 
 */
public class InheritedRatio implements IGrader {

	private static final double RATIO_LIM = 1.0; // limitation of the division
													// of available methods to
													// inherited methods
	private static final double IDEAL_RATIO = 1.2; // Ratio between available
													// methods and inherited
													// methods that we think it
													// is the best
	IType type;

	public InheritedRatio(IType t) {
		type = t;
	}

	@Override
	public double getScore() {
		int iMethodsNum = 0;
		int aMethodsNum = 0;
		ArrayList<IMethod> avaiMethods = new ArrayList<IMethod>();
		// Collect available methods
		IMethod[] curMethods = null;
		try {
			curMethods = type.getMethods();
		} catch (JavaModelException e) {
		}
		for (IMethod m : curMethods) {
			if (!isUsedMethod(m, avaiMethods)) {
				avaiMethods.add(m);
			}
		}

		// Find iMethodsNum
		IProject project = type.getJavaProject().getProject();
		ITypeHierarchy hierachy = null;
		try {
			hierachy = type.newSupertypeHierarchy(new NullProgressMonitor());
		} catch (JavaModelException e) {
		}
		if (hierachy == null) {
			return 0;
		}
		IType[] superTypes = hierachy.getAllSupertypes(type);
		// CodeImpUtils.printJElementArray(superTypes);
		int superTypesNum = superTypes.length;
		for (IType superType : superTypes) {
			if (!CodeImpUtils.isInProject(superType, project)) {
				superTypesNum--;
				continue;
			}
			// Get number of public or protected methods in superType
			int inheritableMethodsNum = 0;
			IMethod[] methods = null;
			try {
				methods = superType.getMethods();
			} catch (JavaModelException e) {
			}
			if (methods == null) {
				continue;
			}
			for (IMethod method : methods) {
				if (!isPrivate(method) && !isUsedMethod(method, avaiMethods)) {
					inheritableMethodsNum++;
					avaiMethods.add(method);
				}
			}
			iMethodsNum += inheritableMethodsNum;
		}
		if (superTypesNum <= 0) {
			return 0;
		}

		aMethodsNum = avaiMethods.size();

		// TODO normalize the score
		if (iMethodsNum == 0) {
			return Double.MAX_VALUE;
		} else {
			return f((double) aMethodsNum / (double) iMethodsNum);
		}
	}

	private boolean isUsedMethod(IMethod method, ArrayList<IMethod> avaiMethods) {
		for (IMethod m : avaiMethods) {
			if (method.isSimilar(m)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check a IMember instance be a private member or not
	 * 
	 * @param method
	 * @return
	 */
	private boolean isPrivate(IMember member) {
		String memDec = null;
		try {
			memDec = member.getSource();
		} catch (JavaModelException e) {
			return false;
		}
		int index = memDec.indexOf("{");
		if (index > 0)
			memDec = memDec.substring(0, memDec.indexOf("{"));
		if (memDec.matches(".*\\bprivate\\b.*")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Function to calculate the dependency of the given value to a constant
	 * (IDEAL_RATIO). The function should satisfy: (f(a) -> 0 when a ->
	 * IDEAL_RATIO) and (f(a) -> 1 when a -> inf. or a -> 1)
	 * 
	 * @param a
	 * @return
	 */
	private double f(double a) {
		if (a == RATIO_LIM) {
			return 0;
		} else {
			return Math.abs(a * (a - IDEAL_RATIO) / (a - RATIO_LIM));
		}
	}

}
