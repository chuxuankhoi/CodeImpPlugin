package codeimp.graders;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import codeimp.CodeImpUtils;

public class TCC implements IGrader {

	private IType type;
	private IFile file;

	@Override
	public double getScore() {
		double np = 0; // number of all pairs of methods
		double sp = 0; // number of pairs that their methods share attribute
		IField[] fields = null; // all fields in the class
		IMethod[] methods = null; // all methods in the class

		try {
			fields = type.getFields();
			methods = type.getMethods();
		} catch (JavaModelException e) {
			return 0;
		}

		// Calculate np and sp
		np = CodeImpUtils.combination(methods.length, 2);
		sp = calculateConnectedPairs(methods, fields, file);

		// Calculate TCC
		if(np == 0) {
			return 0;
		}
		return sp / np;
	}

	private int calculateConnectedPairs(IMethod[] methods, IField[] fields,
			IFile sfile) {
		int mLen = methods.length;
		int count = 0;
		for (int i = 0; i < mLen; i++) {
			for (int j = i + 1; j < mLen; j++) {
				try {
					// TODO solve cannot see extracted(String)
					if (CodeImpUtils.calculateSharedFields(methods[i],
							methods[j], fields, methods, sfile, true) > 0) {
						count++;
					}
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
		return count;
	}

	public TCC(IType type, IFile file) {
		this.type = type;
		this.file = file;
	}

}
