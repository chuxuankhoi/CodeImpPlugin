/**
 * 
 */
package codeimp.graders;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import codeimp.CodeImpUtils;

/**
 * @author chuxuankhoi
 * 
 */
public class LCOM1Score implements IGrader {

	private IType scoredClass;
	private IFile containingFile;

	public LCOM1Score(IType type, IFile file) {
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
					if (similar(methods[i], methods[j], fields, containingFile) == 0) {
						indMethodsNum++;
					} else {
						depMethodsNum++;
					}
				}
			}
			// Calculate LCOM1
			if (indMethodsNum - depMethodsNum > 0) {
				return ((double) indMethodsNum - (double) depMethodsNum)
						/ (double) consideredMethodsNum;
			} else {
				return 0;
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Calculate number of fields used by both 2 methods
	 * 
	 * @param method1
	 * @param method2
	 * @param fields
	 * @return
	 * @throws JavaModelException
	 */
	private int similar(IMethod method1, IMethod method2, IField[] fields,
			IFile file) throws JavaModelException {
		// Extract fields used in methods
		IField[] usedFields1 = CodeImpUtils.getFieldsInMethod(method1, fields,
				file);
		IField[] usedFields2 = CodeImpUtils.getFieldsInMethod(method2, fields,
				file);

		// Get number of intersections between 2 fields
		int ret = 0;
		for (IField f1 : usedFields1) {
			for (IField f2 : usedFields2) {
				if (f1.equals(f2)) {
					ret++;
				}
			}
		}

		return ret;
	}
}
