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
 * @author Chu Xuan Khoi
 * 
 */
public class LCOM5 implements IGrader {

	private IType type;
	private IFile file;

	/*
	 * (non-Javadoc)
	 * 
	 * @see codeimp.graders.IGrader#getScore()
	 */
	@Override
	public double getScore() {
		double a = 0; // summation of the number of distinct attributes accessed
						// by each method
		double k = 0; // number of method
		double l = 0; // number of attributes
		IField[] fields = null;
		IMethod[] methods = null;

		try {
			fields = type.getFields();
			methods = type.getMethods();
		} catch (JavaModelException e) {
			e.printStackTrace();
			return 0;
		}
		k = methods.length;
		l = fields.length;
		try {
			for (IMethod m : methods) {
				IField[] usedFields = CodeImpUtils.getFieldsInMethod(m, fields,
						file);
				if(usedFields != null) {
					a += usedFields.length;
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
			return 0;
		}

		// Calculate LCOM5
		double score = (a - k * l) / (l - k * l);
		return score;
	}

	/**
	 * @param type
	 * @param file
	 */
	public LCOM5(IType type, IFile file) {
		this.type = type;
		this.file = file;
	}

}
