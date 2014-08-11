/**
 * Storing common used and single functions of the project
 */
package codeimp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

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
	/**
	 * Get index of the element that has the given name
	 * 
	 * @param list
	 * @param elementName
	 * @return
	 */
	public static int indexOfInstanceWithName(List<IJavaElement> list,
			String elementName) {
		for (IJavaElement e : list) {
			if (e.getElementName().equals(elementName)) {
				return list.indexOf(e);
			}
		}
		return -1;
	}

	/**
	 * Get all IJavaElement root uses in its code
	 * 
	 * @param root
	 * @param file
	 * @return
	 * @throws JavaModelException
	 */
	public static IJavaElement[] getJElementTreeElements(IJavaElement root,
			IFile file) throws JavaModelException {
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
			if (CodeImpUtils.indexOfInstanceWithName(treeElements,
					ce.getElementName()) == -1) {
				treeElements.add(ce);
				IJavaElement[] grandChildren = getJElementTreeElements(ce, file);
				if (grandChildren == null) {
					continue;
				}
				for (IJavaElement gc : grandChildren) {
					if (CodeImpUtils.indexOfInstanceWithName(treeElements,
							gc.getElementName()) == -1) {
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

		String[] words = code
				.split("[ \t\\x0b\n(){}'\";,.!\\+-/\\*&=%<>\\?\\|:]");
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
	 * @param method
	 *            method analysed
	 * @param referentFields
	 *            list of fields that may be used in the method
	 * @throws JavaModelException
	 */
	public static IField[] getFieldsInMethod(IMethod method,
			IField[] referentFields, IFile file) throws JavaModelException {
		String method1Code = method.getSource();
		ArrayList<IField> usedField = new ArrayList<IField>();
		IJavaElement[] elements = CodeImpUtils.identifyElements(method1Code,
				file);
		if (elements == null) {
			return null;
		}
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

	/**
	 * Extract method to look for the methods used
	 * 
	 * @param method
	 *            method analysed
	 * @param referentMethods
	 *            list of fields that may be used in the method
	 * @throws JavaModelException
	 */
	public static IMethod[] getMethodsInMethod(IMethod method,
			IMethod[] referentMethods, IFile file) throws JavaModelException {
		String method1Code = method.getSource();
		ArrayList<IMethod> usedMethods = new ArrayList<IMethod>();
		IJavaElement[] elements = CodeImpUtils.identifyElements(method1Code,
				file);
		if (elements == null) {
			return null;
		}
		for (IJavaElement e : elements) {
			if (!(e instanceof IMethod))
				continue;
			for (IMethod f : referentMethods) {
				if (((IMethod) e).equals(f)) {
					// before getting field, avoid duplicated
					boolean existed = false;
					for (int i = 0; i < usedMethods.size(); i++) {
						if (e.getElementName().equals(
								usedMethods.get(i).getElementName())) {
							existed = true;
							break;
						}
					}
					if (!existed) {
						usedMethods.add((IMethod) e);
					}
				}
			}
		}

		IMethod[] retArray = new IMethod[usedMethods.size()];
		usedMethods.toArray(retArray);
		return retArray;
	}

	/**
	 * Print message with time stamp
	 * 
	 * @param log
	 *            message user want to print to console
	 */
	public static void printLog(String log) {
//		System.out.println(System.currentTimeMillis() + " - " + log);
	}

	public static String getBody(IMethod method) throws JavaModelException {
		if(method == null) {
			return null;
		}
		String body = method.getSource();
		if (body == null) {
			return null;
		}
		int firstIndex = body.indexOf("{");
		int lastIndex = body.lastIndexOf("}");
		if (firstIndex < 0 || lastIndex < 0) {
			return null;
		}
		body = body.substring(firstIndex + 1, lastIndex);
		body = body.trim();
		return body;
	}

	public static boolean isStatic(IJavaElement element)
			throws JavaModelException {
		if (element instanceof IField) {
			return (((IField) element).getSource().indexOf("static ") > -1);
		} else if (element instanceof IMethod) {
			String source = ((IMethod) element).getSource();
			if (source == null) {
				return false;
			}
			source = source.substring(0, source.indexOf("{"));
			return (source.indexOf("static ") > -1);
		}
		return false;
	}

	public static int combination(int n, int k) {
		if (k <= 0 || (n - k) <= 0) {
			return 0;
		}
		return permutation(n) / (permutation(k) * permutation(n - k));
	}

	public static int permutation(int n) {
		if (n < 1) {
			return 0;
		}
		if (n == 1) {
			return 1;
		}
		return n * permutation(n - 1);
	}

	/**
	 * Calculate the number of fields that 2 methods use
	 * 
	 * @param method1
	 * @param method2
	 * @param fields
	 * @param file
	 * @param transitively
	 *            consider the fields that used by methods used in the given
	 *            methods
	 * @return
	 * @throws JavaModelException
	 */
	public static int calculateSharedFields(IMethod method1, IMethod method2,
			IField[] fields, IMethod[] methods, IFile file, boolean transitively)
			throws JavaModelException {
		// Extract fields used in methods
		IField[] usedFields1 = CodeImpUtils.getFieldsInMethod(method1, fields,
				file);
		IField[] usedFields2 = CodeImpUtils.getFieldsInMethod(method2, fields,
				file);
		if (usedFields1 == null || usedFields2 == null) {
			return 0;
		}

		// Get number of intersections between 2 fields
		int ret = 0;
		for (IField f1 : usedFields1) {
			for (IField f2 : usedFields2) {
				if (f1.equals(f2)) {
					ret++;
				}
			}
		}

		if (transitively) {
			if (methods != null) {
				IMethod[] usedMethods = getMethodsInMethod(method1, methods,
						file);
				if (usedMethods != null) {
					for (IMethod m : usedMethods) {
						if (m == method1 || m == method2) {
							continue;
						}
						ret += calculateSharedFields(m, method2, fields, null,
								file, false);
					}
				}
				usedMethods = getMethodsInMethod(method2, methods, file);
				if (usedMethods != null) {
					for (IMethod m : usedMethods) {
						if (m == method1 || m == method2) {
							continue;
						}
						ret += calculateSharedFields(m, method1, fields, null,
								file, false);
					}
				}
			}
		}

		return ret;
	}

	public static boolean ASC = true;
	public static boolean DESC = false;

	public static Map<String, Double> sortByComparator(
			Map<String, Double> unsortMap, final boolean order) {

		List<Entry<String, Double>> list = new LinkedList<Entry<String, Double>>(
				unsortMap.entrySet());

		// Sorting the list based on values
		Collections.sort(list, new Comparator<Entry<String, Double>>() {
			public int compare(Entry<String, Double> o1,
					Entry<String, Double> o2) {
				if (order) {
					return o1.getValue().compareTo(o2.getValue());
				} else {
					return o2.getValue().compareTo(o1.getValue());

				}
			}
		});

		// Maintaining insertion order with the help of LinkedList
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Entry<String, Double> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

	@SuppressWarnings("rawtypes")
	public static void printMap(Map<?, ?> mp) {
		Iterator<?> it = mp.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			System.out.println(pairs.getKey() + " = " + pairs.getValue());
		}
	}

	public static void printJElementArray(IJavaElement[] array) {
		for (int i = 0; i < array.length; i++) {
			System.out.println("Item " + i + ": " + array[i].getElementName());
		}
	}
	
	public static void printStringArrayList(ArrayList<String> input) {
		for(String str:input) {
			System.out.println(str);
		}
	}

	public static void shuffleArray(Object[] input) {
		Random rnd = new Random();
		for (int i = input.length - 1; i > 0; i--) {
			int index = rnd.nextInt(i + 1);
			// Simple swap
			Object a = input[index];
			input[index] = input[i];
			input[i] = a;
		}
	}
}
