/**
 * 
 */
package codeimp;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.ExtractClassDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.MoveDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * @author chuxuankhoi
 * 
 *         Improve selected code using hill-climbing algorithm
 * 
 */
public class CodeImp {

	protected IWorkbenchWindow window;
	protected ITextSelection codeSelection;
	protected IFile sourceFile;
	protected String[] refactoringHistory;

	protected boolean needStopThread = false;

	public static void printLog(String log) {
		System.out.println(System.currentTimeMillis() + " - CodeImp - " + log);
	}

	public CodeImp(ITextSelection selectedCode, IFile file,
			IWorkbenchWindow currentWindow) {
		codeSelection = selectedCode;
		sourceFile = file;
		window = currentWindow;
	}

	public void runImprovement() {
		try {
			double curScore = getCurrentScore();
			double oldScore = curScore;
			boolean needMoreImp = true;
			while (needMoreImp && !needStopThread) {
				// TODO run improvement over the elements using hill-climbing
				// algorithm
				curScore = getCurrentScore();
				if (curScore >= oldScore) {
					needMoreImp = false;
				}
				oldScore = curScore;
			}
		} catch (Exception e) {
			printLog(e.getMessage());
		}

	}

	public String getRefactoringHistory() {
		return "";
	}

	public double getCurrentScore() throws JavaModelException {
		IJavaElement[] elements = identifyElements(codeSelection.getText(),
				sourceFile);
		if (elements == null) {
			System.out.println("No element found by identifier.");
			return 0;
		}
		double score = 0;
		for (IJavaElement element : elements) {
			double elementScore = scoreElement(element);
			printLog("Element: " + element.getElementName() + " - Type: "
					+ element.getElementType() + " - Score: " + elementScore);
			score += elementScore;
		}
		return score;
	}

	private double scoreElement(IJavaElement element) throws JavaModelException {
		// TODO Select appropriate scoring function for element based on its
		// type
		double score = 0;
		if (element instanceof IType) {
			if (isInProject((IType) element, sourceFile.getProject())) {
				score = scoreLCOM1((IType) element);
			}
		}

		return score;
	}

	/**
	 * Check the class is belong to the current project or not. This method is
	 * important because IJavaElement.getChildren() may get IType instances
	 * which belong to packages that we don't want to modify
	 * 
	 * @param element
	 * @return
	 */
	private boolean isInProject(IType element, IProject project) {
		String elementLocation = element.getPath().toString();
		String[] elementPath = elementLocation.split("/");
		String projectName = project.getName();
		if (projectName.equals(elementPath[1])) {
			return true;
		}
		return false;
	}

	/**
	 * S. R. Chidamber, C. F. Kemerer - A Metrics suite for Object Oriented
	 * design
	 * 
	 * @param element
	 * @return
	 * @throws JavaModelException
	 */
	private double scoreLCOM1(IType element) throws JavaModelException {
		// Get fields and method
		IField[] fields = element.getFields();
		IMethod[] methods = element.getMethods();
		int consideredMethodsNum = methods.length; // number of methods which are not main()

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
				if (similar(methods[i], methods[j], fields) == 0) {
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
	}

	/**
	 * Calculate number of fields used by both 2 methods
	 * @param method1
	 * @param method2
	 * @param fields
	 * @return
	 * @throws JavaModelException
	 */
	private int similar(IMethod method1, IMethod method2, IField[] fields)
			throws JavaModelException {
		// Extract fields used in methods
		IField[] usedFields1 = getFieldsInMethod(method1, fields);
		IField[] usedFields2 = getFieldsInMethod(method2, fields);

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

	/**
	 * Extract method to look for the fields used
	 * @param method1
	 * @param referentFields
	 * @throws JavaModelException
	 */
	private IField[] getFieldsInMethod(IMethod method, IField[] referentFields)
			throws JavaModelException {
		String method1Code = method.getSource();
		ArrayList<IField> usedField = new ArrayList<IField>();
		IJavaElement[] elements = identifyElements(method1Code, sourceFile);
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
		System.out.println("Number of used fields: " + usedField.size());
		for (int i = 0; i < usedField.size(); i++) {
			System.out.println(method.getElementName() + " - \tUsed field: "
					+ usedField.get(i).getElementName());
		}

		IField[] retArray = new IField[usedField.size()];
		usedField.toArray(retArray);
		return retArray;
	}

	/**
	 * Look for IJavaElement instances in the given string.
	 * The string must be a part of a source file.
	 * @param code
	 * @param file
	 * @return
	 * @throws JavaModelException
	 */
	private IJavaElement[] identifyElements(String code, IFile file)
			throws JavaModelException {
		if (code == null || file == null || code.equals("")) {
			printLog("Error when initializing CodeImp instance.");
			return null;
		}
		ArrayList<IJavaElement> retList = new ArrayList<IJavaElement>();

		ICompilationUnit cu = JavaCore.createCompilationUnitFrom(file);
		String sourceCode = cu.getSource();

		String[] words = code.split("[ \t\\x0b\n(){}'\";,.]");
		int oldOffset = sourceCode.indexOf(code, 0);
		if (oldOffset == -1) {
			printLog("Code seems not be a part of the file");
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
	 * @param pair
	 */
	private void refactorElement(RefactoringPair pair) {
		if (pair == null) {
			return;
		}
		if (pair.element == null || pair.action == null
				|| pair.addition == null) {
			MessageDialog.openInformation(window.getShell(), "Refactoring ...",
					"Error when creating pair.");
			return;
		}
		System.out.println("Refator " + pair.element.getElementName() + " by "
				+ pair.action);
		RefactoringContribution contribution = RefactoringCore
				.getRefactoringContribution(pair.action);
		JavaRefactoringDescriptor descriptor = createDescriptor(pair,
				contribution);

		RefactoringStatus status = new RefactoringStatus();
		try {
			Refactoring refactoring = descriptor.createRefactoring(status);
			IProgressMonitor monitor = new NullProgressMonitor();
			refactoring.checkInitialConditions(monitor);
			refactoring.checkFinalConditions(monitor);
			Change change = refactoring.createChange(monitor);
			change.perform(monitor);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Depend on the action user expected, the function creates a descriptor
	 * which contains enough information for the refactoring
	 * 
	 * @param pair
	 * @param contribution
	 * @return
	 */
	private JavaRefactoringDescriptor createDescriptor(RefactoringPair pair,
			RefactoringContribution contribution) {
		JavaRefactoringDescriptor descriptor;
		descriptor = (JavaRefactoringDescriptor) contribution
				.createDescriptor();
		// next lines are essential for any refactoring
		descriptor.setProject(sourceFile.getProject().getName());

		// following lines depend on the specific refactoring
		switch (pair.action) {
		case IJavaRefactorings.RENAME_FIELD:
		case IJavaRefactorings.RENAME_LOCAL_VARIABLE:
		case IJavaRefactorings.RENAME_COMPILATION_UNIT:
		case IJavaRefactorings.RENAME_ENUM_CONSTANT:
		case IJavaRefactorings.RENAME_JAVA_PROJECT:
		case IJavaRefactorings.RENAME_METHOD:
		case IJavaRefactorings.RENAME_PACKAGE:
		case IJavaRefactorings.RENAME_SOURCE_FOLDER:
		case IJavaRefactorings.RENAME_TYPE:
		case IJavaRefactorings.RENAME_TYPE_PARAMETER:
			RenameJavaElementDescriptor renameDescriptor = (RenameJavaElementDescriptor) descriptor;
			renameDescriptor.setJavaElement(pair.element);
			renameDescriptor.setNewName((String) pair.addition);
			renameDescriptor.setDeprecateDelegate(true);
			renameDescriptor.setKeepOriginal(false);
			renameDescriptor.setUpdateReferences(true);
			renameDescriptor.setUpdateTextualOccurrences(true);
			break;
		case IJavaRefactorings.CHANGE_METHOD_SIGNATURE:
			break;
		case IJavaRefactorings.CONVERT_ANONYMOUS:
		case IJavaRefactorings.CONVERT_LOCAL_VARIABLE:
		case IJavaRefactorings.CONVERT_MEMBER_TYPE:
		case IJavaRefactorings.COPY:
		case IJavaRefactorings.DELETE:
		case IJavaRefactorings.ENCAPSULATE_FIELD:
			break;
		case IJavaRefactorings.EXTRACT_CLASS:
			ExtractClassDescriptor extractClassDescriptor = (ExtractClassDescriptor) descriptor;
			extractClassDescriptor.setClassName((String) pair.addition);
			break;
		case IJavaRefactorings.EXTRACT_CONSTANT:
		case IJavaRefactorings.EXTRACT_INTERFACE:
		case IJavaRefactorings.EXTRACT_LOCAL_VARIABLE:
		case IJavaRefactorings.EXTRACT_METHOD:
			break;
		case IJavaRefactorings.EXTRACT_SUPERCLASS:
			break;
		case IJavaRefactorings.GENERALIZE_TYPE:
		case IJavaRefactorings.INFER_TYPE_ARGUMENTS:
		case IJavaRefactorings.INLINE_CONSTANT:
		case IJavaRefactorings.INLINE_LOCAL_VARIABLE:
		case IJavaRefactorings.INLINE_METHOD:
		case IJavaRefactorings.INTRODUCE_FACTORY:
		case IJavaRefactorings.INTRODUCE_INDIRECTION:
		case IJavaRefactorings.INTRODUCE_PARAMETER:
		case IJavaRefactorings.INTRODUCE_PARAMETER_OBJECT:
			break;
		case IJavaRefactorings.MOVE:
			MoveDescriptor moveDescriptor = (MoveDescriptor) descriptor;
			moveDescriptor.setDestination((IJavaElement) pair.addition);
			moveDescriptor.setUpdateReferences(true);
			moveDescriptor.setUpdateQualifiedNames(true);
			if (pair.element instanceof IMember) {
				IMember[] members = { (IMember) pair.element };
				moveDescriptor.setMoveMembers(members);
			} else if (pair.element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot[] roots = { (IPackageFragmentRoot) pair.element };
				moveDescriptor.setMovePackageFragmentRoots(roots);
			} else if (pair.element instanceof IPackageFragment) {
				IPackageFragment[] packages = { (IPackageFragment) pair.element };
				moveDescriptor.setMovePackages(packages);
			} else if (pair.element instanceof IResource) {
				// Do not support
			} else {
				// Do nothing
			}
			break;
		case IJavaRefactorings.MOVE_METHOD:
			break;
		case IJavaRefactorings.MOVE_STATIC_MEMBERS:
			break;
		case IJavaRefactorings.PULL_UP:
			break;
		case IJavaRefactorings.PUSH_DOWN:
		default:
			break;
		}

		return descriptor;
	}
}
