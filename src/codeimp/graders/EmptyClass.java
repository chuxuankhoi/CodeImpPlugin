package codeimp.graders;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import codeimp.CodeImpUtils;

/**
 * A metric to prevent empty class when judging the refactorings
 * @author Chu Xuan Khoi
 *
 */
public class EmptyClass implements IGrader {
	
	private IType type;
	
	public EmptyClass(IType t) {
		type = t;
	}

	@Override
	public double getScore() {
		IJavaElement[] children = null;
		try {
			children = type.getChildren();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(children == null || children.length == 0) {
			CodeImpUtils.printLog("EMPTY CLASS - It make the other score pointless");
			return Double.MAX_VALUE;
		} else {
			return 0;
		}
	}

}
