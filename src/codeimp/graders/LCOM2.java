/**
 * 
 */
package codeimp.graders;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import codeimp.CodeImpUtils;

/**
 * @author chuxuankhoi
 * 
 */
public class LCOM2 implements IGrader {

	private IType scoredClass;
	private IFile containingFile;

	public LCOM2(IType type, IFile file) {
		scoredClass = type;
		containingFile = file;
	}

	public double getScore() {
		// Get fields and method
		IField[] fields;
		try {
			fields = scoredClass.getFields();
			IMethod[] methods = scoredClass.getMethods();
			int consideredMethodsNum = methods.length; // number of methods
														// which
														// are not main()

			// Get number of pair of independent methods and dependent methods
			int indMethodsNum = 0; // number of independent pair of methods
			int depMethodsNum = 0; // number of dependent pair of methods
			for (int i = 0; i < methods.length; i++) {
				if (methods[i].isMainMethod()) {
					consideredMethodsNum--;
					continue;
				}
				for (int j = i + 1; j < methods.length; j++) {
					if (methods[j].isMainMethod()) {
						continue;
					}
					if (CodeImpUtils.calculateSharedFields(methods[i],
							methods[j], fields, null, containingFile, false) == 0) {
						indMethodsNum++;
					} else {
						depMethodsNum++;
					}
				}
			}
			// Calculate LCOM2
			if (indMethodsNum - depMethodsNum > 0 && consideredMethodsNum != 0) {
				return ((double) indMethodsNum - (double) depMethodsNum)
						/ (double) consideredMethodsNum;
			} else {
				return 0;
			}
		} catch (CoreException e) {
			e.printStackTrace();
			return 0;
		}
	}
}
