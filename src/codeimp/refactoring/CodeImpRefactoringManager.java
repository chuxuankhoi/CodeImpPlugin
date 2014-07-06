/**
 * 
 */
package codeimp.refactoring;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.ExtractClassDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.MoveDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

import codeimp.CodeImpUtils;

/**
 * @author Chu Xuan Khoi
 * 
 */
public class CodeImpRefactoringManager {

	private static CodeImpRefactoringManager manager = null;

	private ArrayList<String> actionsList = null;

	public static CodeImpRefactoringManager getManager() {
		if (manager == null) {
			manager = new CodeImpRefactoringManager();
		}
		return manager;
	}

	private CodeImpRefactoringManager() {
		initialize();
	}

	private void initialize() {
		Field[] fields = IJavaRefactorings.class.getDeclaredFields();
		actionsList = new ArrayList<String>();
		for (int i = 0; i < fields.length; i++) {
			if (Modifier.isStatic(fields[i].getModifiers())) {
				try {
					actionsList.add((String) fields[i].get(null));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					continue;
				}
			}
		}

	}

	/**
	 * Get all refactoring actions supported by scanning
	 * {@link IJavaRefactorings} to find all static fields' value
	 * 
	 * @return
	 */
	public String[] getActionsList() {
		if (actionsList.size() == 0) {
			return null;
		}

		String[] ret = new String[actionsList.size()];
		actionsList.toArray(ret);
		return ret;
	}

	/**
	 * Look for the objects that the given refactoring action can be applied to
	 * Support: Eclipse 3.8.1 refactoring actions
	 * 
	 * @param rootElement
	 *            scope to look for the objects
	 * @param action
	 *            determine the refactoring action, must be listed in
	 *            {@link IJavaRefactorings}
	 * @param sourceFile
	 * @return list of refactoring pair which describes all required items for
	 *         the refactoring action
	 */
	public RefactoringPair[] getRefactoringPairs(IJavaElement rootElement,
			String action, IFile sourceFile) {
		RefactoringPair[] pairs = null;
		// TODO Get available pairs for the action in the rootElement
		try {
			switch (action) {
			case IJavaRefactorings.RENAME_FIELD:
				// Get all fields declared in the root element (class)
				pairs = getRenameFieldPairs(rootElement, action);
				break;
			case IJavaRefactorings.RENAME_LOCAL_VARIABLE:
				// Get all local variables declared in the root element (method)
				pairs = getRenameLocalVariablePairs(rootElement, action,
						sourceFile);
				break;
			case IJavaRefactorings.RENAME_COMPILATION_UNIT:
			case IJavaRefactorings.RENAME_ENUM_CONSTANT:
			case IJavaRefactorings.RENAME_JAVA_PROJECT:
			case IJavaRefactorings.RENAME_METHOD:
			case IJavaRefactorings.RENAME_PACKAGE:
			case IJavaRefactorings.RENAME_SOURCE_FOLDER:
			case IJavaRefactorings.RENAME_TYPE:
			case IJavaRefactorings.RENAME_TYPE_PARAMETER:
				break;
			case IJavaRefactorings.CHANGE_METHOD_SIGNATURE:
			case IJavaRefactorings.CONVERT_ANONYMOUS:
			case IJavaRefactorings.CONVERT_LOCAL_VARIABLE:
			case IJavaRefactorings.CONVERT_MEMBER_TYPE:
			case IJavaRefactorings.COPY:
			case IJavaRefactorings.DELETE:
			case IJavaRefactorings.ENCAPSULATE_FIELD:
			case IJavaRefactorings.EXTRACT_CLASS:
			case IJavaRefactorings.EXTRACT_CONSTANT:
			case IJavaRefactorings.EXTRACT_INTERFACE:
			case IJavaRefactorings.EXTRACT_LOCAL_VARIABLE:
			case IJavaRefactorings.EXTRACT_METHOD:
			case IJavaRefactorings.EXTRACT_SUPERCLASS:
			case IJavaRefactorings.GENERALIZE_TYPE:
			case IJavaRefactorings.INFER_TYPE_ARGUMENTS:
			case IJavaRefactorings.INLINE_CONSTANT:
			case IJavaRefactorings.INLINE_LOCAL_VARIABLE:
			case IJavaRefactorings.INLINE_METHOD:
			case IJavaRefactorings.INTRODUCE_FACTORY:
			case IJavaRefactorings.INTRODUCE_INDIRECTION:
			case IJavaRefactorings.INTRODUCE_PARAMETER:
			case IJavaRefactorings.INTRODUCE_PARAMETER_OBJECT:
			case IJavaRefactorings.MOVE:
			case IJavaRefactorings.MOVE_METHOD:
			case IJavaRefactorings.MOVE_STATIC_MEMBERS:
			case IJavaRefactorings.PULL_UP:
			case IJavaRefactorings.PUSH_DOWN:
			default:
				break;
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
			return null;
		}
		return pairs;
	}

	/**
	 * @param rootElement
	 * @param action
	 * @param sourceFile
	 * @param pairs
	 * @return
	 * @throws JavaModelException
	 */
	private RefactoringPair[] getRenameLocalVariablePairs(
			IJavaElement rootElement, String action, IFile sourceFile)
			throws JavaModelException {
		if (rootElement instanceof IMethod) {
			IJavaElement[] allElements = CodeImpUtils.identifyElements(
					CodeImpUtils.getBody((IMethod) rootElement), sourceFile);
			if (allElements == null || allElements.length == 0) {
				return null;
			}
			ArrayList<RefactoringPair> pairList = new ArrayList<RefactoringPair>();
			for (int i = 0; i < allElements.length; i++) {
				if (allElements[i] instanceof ILocalVariable) {
					RefactoringPair pair = new RefactoringPair();
					pair.element = allElements[i];
					pair.action = action;
					pair.addition = "local" + i;
					pairList.add(pair);
				}
			}
			if (pairList.size() == 0) {
				return null;
			}
			RefactoringPair[] pairs = new RefactoringPair[pairList.size()];
			pairList.toArray(pairs);
			return pairs;
		}
		return null;
	}

	/**
	 * @param rootElement
	 * @param action
	 * @param pairs
	 * @return
	 * @throws JavaModelException
	 */
	private RefactoringPair[] getRenameFieldPairs(IJavaElement rootElement,
			String action) throws JavaModelException {
		if (rootElement instanceof IType) {
			IField[] fields = ((IType) rootElement).getFields();
			RefactoringPair[] pairs = new RefactoringPair[fields.length];
			for (int i = 0; i < fields.length; i++) {
				pairs[i].element = fields[i];
				pairs[i].action = action;
				pairs[i].addition = "field" + i;
			}
			return pairs;
		}
		return null;
	}

	/**
	 * Depend on the action user expected, the function creates a descriptor
	 * which contains enough information for the refactoring
	 * 
	 * @param pair
	 * @param project
	 * @return
	 */
	public JavaRefactoringDescriptor getDescriptor(RefactoringPair pair,
			IResource project) {
		RefactoringContribution contribution = RefactoringCore
				.getRefactoringContribution(pair.action);
		JavaRefactoringDescriptor descriptor;
		descriptor = (JavaRefactoringDescriptor) contribution
				.createDescriptor();
		descriptor.setProject(project.getName());

		// TODO Complete refactoring list
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
			createRenameJavaElementDescriptor(pair, descriptor);
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
			createExtractMethodDescriptor(pair, descriptor);
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
			createMoveDescriptor(pair, descriptor);
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

	/**
	 * @param pair
	 * @param descriptor
	 */
	private void createMoveDescriptor(RefactoringPair pair,
			JavaRefactoringDescriptor descriptor) {
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
	}

	/**
	 * @param pair
	 * @param descriptor
	 */
	private void createExtractMethodDescriptor(RefactoringPair pair,
			JavaRefactoringDescriptor descriptor) {
		ExtractClassDescriptor extractClassDescriptor = (ExtractClassDescriptor) descriptor;
		extractClassDescriptor.setClassName(((IJavaElement) pair.element)
				.getElementName() + "Class");
		extractClassDescriptor.setCreateTopLevel(true);
		extractClassDescriptor.setType((IType) ((IJavaElement) pair.element)
				.getParent());
	}

	/**
	 * @param pair
	 * @param descriptor
	 */
	private void createRenameJavaElementDescriptor(RefactoringPair pair,
			JavaRefactoringDescriptor descriptor) {
		RenameJavaElementDescriptor renameDescriptor = (RenameJavaElementDescriptor) descriptor;
		renameDescriptor.setJavaElement((IJavaElement) pair.element);
		renameDescriptor
				.setNewName(pair.addition == null ? ((IJavaElement) pair.element)
						.getElementName() + "NewName"
						: (String) pair.addition);
		renameDescriptor.setDeprecateDelegate(true);
		renameDescriptor.setKeepOriginal(false);
		renameDescriptor.setUpdateReferences(true);
		renameDescriptor.setUpdateTextualOccurrences(true);
	}

}
