/**
 * Storing common used and single functions of the project
 */
package codeimp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public final class CodeImpUtils {
	public static int indexOfValue(List<IJavaElement> list, String elementName) {
		for (IJavaElement e : list) {
			if (e.getElementName().equals(elementName)) {
				return list.indexOf(e);
			}
		}
		return -1;
	}
	
	public static IJavaElement[] getRefactoredElements(String source, IFile file)
			throws JavaModelException {
		ArrayList<IJavaElement> refactoredElements = new ArrayList<IJavaElement>();
		IJavaElement[] rootElements = identifyElements(source, file);
		for (IJavaElement e : rootElements) {
			if (isInProject(e, file.getProject())) {
				if (e instanceof IMethod && ((IMethod) e).isMainMethod()) {
					continue;
				}
				if (CodeImpUtils.indexOfValue(refactoredElements, e.getElementName()) == -1) {
					refactoredElements.add(e);
					IJavaElement[] childElements = getJElementTreeElements(e,
							file);
					if (childElements == null) {
						continue;
					}
					for (IJavaElement ce : childElements) {
						refactoredElements.add(ce);
					}
				}
			}
		}

		if (refactoredElements.size() > 0) {
			IJavaElement[] retArray = new IJavaElement[refactoredElements
					.size()];
			refactoredElements.toArray(retArray);
			return retArray;
		} else {
			return null;
		}
	}

	public static IJavaElement[] getJElementTreeElements(IJavaElement root, IFile file)
			throws JavaModelException {
		ArrayList<IJavaElement> treeElements = new ArrayList<IJavaElement>();
		IJavaElement[] childElements = null;
		if (root instanceof IType) {
			childElements = ((IType) root).getChildren();
		} else if (root instanceof ISourceReference) {
			String rootSource = ((ISourceReference) root).getSource();
			childElements = identifyElements(rootSource, file);
		} else {
			return null;
		}
		if (childElements == null) {
			return null;
		}
		for (IJavaElement ce : childElements) {
			if (!isInProject(ce, file.getProject())) {
				continue;
			}
			if (ce instanceof IMethod && ((IMethod) ce).isMainMethod()) {
				continue;
			}
			if (ce.getElementName().equals(root.getElementName())) {
				continue;
			}
			if (CodeImpUtils.indexOfValue(treeElements, ce.getElementName()) == -1) {
				treeElements.add(ce);
				IJavaElement[] grandChildren = getJElementTreeElements(ce, file);
				if (grandChildren == null) {
					continue;
				}
				for (IJavaElement gc : grandChildren) {
					if (CodeImpUtils.indexOfValue(treeElements, gc.getElementName()) == -1) {
						treeElements.add(gc);
					}
				}
			}
		}
		if (treeElements.size() == 0) {
			return null;
		}

		IJavaElement[] retArray = new IJavaElement[treeElements.size()];
		treeElements.toArray(retArray);
		return retArray;
	}
	
	/**
	 * Check the class is belong to the current project or not. This method is
	 * important because IJavaElement.getChildren() may get IType instances
	 * which belong to packages that we don't want to modify
	 * 
	 * @param e
	 * @return
	 */
	public static boolean isInProject(IJavaElement e, IProject project) {
		String elementLocation = e.getPath().toString();
		String[] elementPath = elementLocation.split("/");
		String projectName = project.getName();
		if (projectName.equals(elementPath[1])) {
			return true;
		}
		return false;
	}
	
	/**
	 * Look for IJavaElement instances in the given string. The string must be a
	 * part of a source file.
	 * 
	 * @param code
	 * @param file
	 * @return
	 * @throws JavaModelException
	 */
	public static IJavaElement[] identifyElements(String code, IFile file)
			throws JavaModelException {
		if (code == null || file == null || code.equals("")) {
			return null;
		}
		ArrayList<IJavaElement> retList = new ArrayList<IJavaElement>();

		ICompilationUnit cu = JavaCore.createCompilationUnitFrom(file);
		String sourceCode = cu.getSource();

		String[] words = code.split("[ \t\\x0b\n(){}'\";,.]");
		int oldOffset = sourceCode.indexOf(code, 0);
		if (oldOffset == -1) {
			// printLog("Code seems not be a part of the file");
			return null;
		}
		int offset = 0;
		int length = 0;
		for (String word : words) {
			if (word.equals(""))
				continue;
			offset = sourceCode.indexOf(word, oldOffset);
			length = word.length();
			IJavaElement[] elements = cu.codeSelect(offset, length);
			for (IJavaElement element : elements) {
				retList.add(element);
			}
			oldOffset = offset;
		}

		// Return found array
		IJavaElement[] retArray = new IJavaElement[retList.size()];
		retList.toArray(retArray);
		return retArray;
	}
	
	/**
	 * Extract method to look for the fields used
	 * 
	 * @param method1
	 * @param referentFields
	 * @throws JavaModelException
	 */
	public static IField[] getFieldsInMethod(IMethod method, IField[] referentFields, IFile file)
			throws JavaModelException {
		String method1Code = method.getSource();
		ArrayList<IField> usedField = new ArrayList<IField>();
		IJavaElement[] elements = CodeImpUtils.identifyElements(method1Code, file);
		for (IJavaElement e : elements) {
			if (!(e instanceof IField))
				continue;
			for (IField f : referentFields) {
				if (((IField) e).equals(f)) {
					// before getting field, avoid duplicated
					boolean existed = false;
					for (int i = 0; i < usedField.size(); i++) {
						if (e.getElementName().equals(
								usedField.get(i).getElementName())) {
							existed = true;
							break;
						}
					}
					if (!existed) {
						usedField.add((IField) e);
					}
				}
			}
		}

		IField[] retArray = new IField[usedField.size()];
		usedField.toArray(retArray);
		return retArray;
	}
}
