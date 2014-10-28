<<<<<<< HEAD
package codeimp.refactoring;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import codeimp.CodeImpUtils;

public class FieldUsedMethodFilter implements IExtractor {

	private IField input = null;

	public FieldUsedMethodFilter() {
	}

	public FieldUsedMethodFilter(Object input) {
		if (input instanceof IField) {
			this.input = (IField) input;
		}
	}

	@Override
	public void setInput(Object input) {
		if (input instanceof IField) {
			this.input = (IField) input;
		}
	}

	@Override
	public Object[] getOutput() {
		IMethod[] output = null;
		IType parent = input.getDeclaringType();
		System.out.println("Containing type: " + parent.getElementName());
		IMethod[] possibleMethods = null;
		try {
			possibleMethods = parent.getMethods();
		} catch (JavaModelException e) {
			e.printStackTrace();
			return null;
		}
		IField[] referentFields = new IField[] { input };
		ArrayList<IMethod> methodsList = new ArrayList<IMethod>();
		for (IMethod m : possibleMethods) {
			IField[] fields = null;
			try {
				fields = CodeImpUtils.getFieldsInMethod(m, referentFields,
						(IFile) parent.getResource());
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			if(fields == null || fields.length == 0) {
				continue;
			}
			methodsList.add(m);
		}
		if(methodsList.size() == 0) {
			return null;
		}
		System.out.println("Methods found number: " + methodsList.size());
		output = new IMethod[methodsList.size()];
		output = methodsList.toArray(output);
		return output;
	}

}
=======
package codeimp.refactoring;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import codeimp.CodeImpUtils;

public class FieldUsedMethodFilter implements IExtractor {

	private IField input = null;

	public FieldUsedMethodFilter() {
	}

	public FieldUsedMethodFilter(Object input) {
		if (input instanceof IField) {
			this.input = (IField) input;
		}
	}

	@Override
	public void setInput(Object input) {
		if (input instanceof IField) {
			this.input = (IField) input;
		}
	}

	@Override
	public Object[] getOutput() {
		IMethod[] output = null;
		IType parent = input.getDeclaringType();
		System.out.println("Containing type: " + parent.getElementName());
		IMethod[] possibleMethods = null;
		try {
			possibleMethods = parent.getMethods();
		} catch (JavaModelException e) {
			e.printStackTrace();
			return null;
		}
		IField[] referentFields = new IField[] { input };
		ArrayList<IMethod> methodsList = new ArrayList<IMethod>();
		for (IMethod m : possibleMethods) {
			IField[] fields = null;
			try {
				fields = CodeImpUtils.getFieldsInMethod(m, referentFields,
						(IFile) parent.getResource());
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			if(fields == null || fields.length == 0) {
				continue;
			}
			methodsList.add(m);
		}
		if(methodsList.size() == 0) {
			return null;
		}
		System.out.println("Methods found number: " + methodsList.size());
		output = new IMethod[methodsList.size()];
		output = methodsList.toArray(output);
		return output;
	}

}
>>>>>>> origin/master
