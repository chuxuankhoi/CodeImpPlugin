/**
 * 
 */
package codeimp.refactoring;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
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
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.PushDownRefactoringProcessor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

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
		actionsList.add(IJavaRefactorings.EXTRACT_CLASS);
		// actionsList.add(IJavaRefactorings.MOVE_STATIC_MEMBERS);

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
				pairs = getRenameFieldPairs(rootElement);
				break;
			case IJavaRefactorings.RENAME_LOCAL_VARIABLE:
				// Get all local variables declared in the root element (method)
				pairs = getRenameLocalVariablePairs(rootElement, sourceFile);
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
				break;
			case IJavaRefactorings.DELETE:
				pairs = getDeletePairs(rootElement);
				break;
			case IJavaRefactorings.ENCAPSULATE_FIELD:
				break;
			case IJavaRefactorings.EXTRACT_CLASS:
				pairs = getExtractClassPairs(rootElement);
				break;
			case IJavaRefactorings.EXTRACT_CONSTANT:
			case IJavaRefactorings.EXTRACT_INTERFACE:
				break;
			case IJavaRefactorings.EXTRACT_LOCAL_VARIABLE:
			case IJavaRefactorings.EXTRACT_METHOD:
				pairs = getExtractMethodPairs(rootElement, sourceFile);
				break;
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
				break;
			case IJavaRefactorings.MOVE:
				pairs = getMovePairs(rootElement, sourceFile);
				break;
			case IJavaRefactorings.MOVE_METHOD:
				pairs = getMoveMethodPairs(rootElement);
				break;
			case IJavaRefactorings.MOVE_STATIC_MEMBERS:
				pairs = getMoveStaticPairs(rootElement);
				break;
			case IJavaRefactorings.PULL_UP:
				pairs = getPullUpPairs(rootElement);
				break;
			case IJavaRefactorings.PUSH_DOWN:
				pairs = getPushDownPairs(rootElement);
				break;
			default:
				break;
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
			return null;
		}
		return pairs;
	}

	private RefactoringPair[] getExtractMethodPairs(IJavaElement rootElement,
			IFile file) throws JavaModelException {
		ArrayList<RefactoringPair> pairList = new ArrayList<RefactoringPair>();
		if (rootElement instanceof IMethod) {
			getExtractMethodPairsInMethod((IMethod) rootElement, pairList, file);
		} else if (rootElement instanceof IType) {
			getExtractMethodPairsInClass((IType) rootElement, pairList, file);
		}
		RefactoringPair[] pairs = new RefactoringPair[pairList.size()];
		pairs = pairList.toArray(pairs);
		return pairs;
	}

	private void getExtractMethodPairsInClass(IType rootElement,
			ArrayList<RefactoringPair> pairList, IFile sourceFile) {
		try {
			IMethod[] methods = rootElement.getMethods();
			for (IMethod m : methods) {
				if (!m.isMainMethod()) {
					getExtractMethodPairsInMethod(m, pairList, sourceFile);
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void getExtractMethodPairsInMethod(IMethod rootElement,
			ArrayList<RefactoringPair> pairList, IFile sourceFile) {
		try {
			// Get source code of the function
			String code = CodeImpUtils.getBody(rootElement);
			getExtractMethodPairsInString(code, pairList, sourceFile);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void getExtractMethodPairsInString(String code,
			ArrayList<RefactoringPair> pairList, IFile sourceFile)
			throws JavaModelException {
		if (code == "") {
			return;
		}
		String[] codeLines = code.split("\n");
		for (int i = 0; i < codeLines.length; i++) {
			String before = "";
			for (int j = 0; j <= i; j++) {
				before += codeLines[j];
			}
			String after = "";
			for (int j = i + 1; j < codeLines.length; j++) {
				after += codeLines[j];
			}
			// Get pairs from before
			RefactoringPair pair = new RefactoringPair();
			pair.action = IJavaRefactorings.EXTRACT_METHOD;
			ICompilationUnit cu = JavaCore
					.createCompilationUnitFrom(sourceFile);
			String fileCode = cu.getSource();
			pair.element = new TextSelection(fileCode.indexOf(before),
					before.length());
			pair.addition = cu;
			pairList.add(pair);

			// Get pairs from after
			getExtractMethodPairsInString(after, pairList, sourceFile);
		}
	}

	private RefactoringPair[] getPushDownPairs(IJavaElement rootElement) {
		ArrayList<RefactoringPair> pairList = new ArrayList<RefactoringPair>();
		if (rootElement instanceof IMethod) {
			RefactoringPair pair = new RefactoringPair();
			pair.action = IJavaRefactorings.PUSH_DOWN;
			pair.element = rootElement;
			pairList.add(pair);
		} else if (rootElement instanceof IType) {
			try {
				IField[] fields = ((IType) rootElement).getFields();
				IMethod[] methods = ((IType) rootElement).getMethods();
				for (IField f : fields) {
					RefactoringPair pair = new RefactoringPair();
					pair.action = IJavaRefactorings.PUSH_DOWN;
					pair.element = f;
					pairList.add(pair);
				}
				for (IMethod m : methods) {
					RefactoringPair pair = new RefactoringPair();
					pair.action = IJavaRefactorings.PUSH_DOWN;
					pair.element = m;
					pairList.add(pair);
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
				return null;
			}
		}
		RefactoringPair[] pairs = new RefactoringPair[pairList.size()];
		pairs = pairList.toArray(pairs);
		return pairs;
	}

	private RefactoringPair[] getPullUpPairs(IJavaElement rootElement) {
		ArrayList<RefactoringPair> pairList = new ArrayList<RefactoringPair>();
		if (rootElement instanceof IMethod) {
			RefactoringPair pair = new RefactoringPair();
			pair.action = IJavaRefactorings.PULL_UP;
			pair.element = rootElement;
			pairList.add(pair);
		} else if (rootElement instanceof IType) {
			try {
				IField[] fields = ((IType) rootElement).getFields();
				IMethod[] methods = ((IType) rootElement).getMethods();
				for (IField f : fields) {
					RefactoringPair pair = new RefactoringPair();
					pair.action = IJavaRefactorings.PULL_UP;
					pair.element = f;
					pairList.add(pair);
				}
				for (IMethod m : methods) {
					RefactoringPair pair = new RefactoringPair();
					pair.action = IJavaRefactorings.PULL_UP;
					pair.element = m;
					pairList.add(pair);
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
				return null;
			}
		}
		RefactoringPair[] pairs = new RefactoringPair[pairList.size()];
		pairs = pairList.toArray(pairs);
		return pairs;
	}

	private RefactoringPair[] getMoveStaticPairs(IJavaElement rootElement) {
		RefactoringPair[] pairs = null;
		if (rootElement instanceof IField) {
			if (Modifier.isStatic(rootElement.getClass().getModifiers())) {
				pairs = new RefactoringPair[1];
				pairs[0].action = IJavaRefactorings.DELETE;
				pairs[0].element = rootElement;
			}
		} else if (rootElement instanceof IType) {
			try {
				IField[] fields = ((IType) rootElement).getFields();
				IMethod[] methods = ((IType) rootElement).getMethods();
				ArrayList<RefactoringPair> pairList = new ArrayList<RefactoringPair>();
				for (IField f : fields) {
					if (CodeImpUtils.isStatic(f)) {
						RefactoringPair pair = new RefactoringPair();
						pair.action = IJavaRefactorings.MOVE_STATIC_MEMBERS;
						pair.element = f;
						pairList.add(pair);
					}
				}
				for (IMethod m : methods) {
					if (m.isMainMethod()) {
						continue;
					}
					if (CodeImpUtils.isStatic(m)) {
						RefactoringPair pair = new RefactoringPair();
						pair.action = IJavaRefactorings.MOVE_STATIC_MEMBERS;
						pair.element = m;
						pairList.add(pair);
					}
				}
				pairs = new RefactoringPair[pairList.size()];
				pairs = pairList.toArray(pairs);
			} catch (JavaModelException e) {
				e.printStackTrace();
				return null;
			}
		}
		return pairs;
	}

	private RefactoringPair[] getMoveMethodPairs(IJavaElement rootElement) {
		RefactoringPair[] pairs = null;
		if (rootElement instanceof IMethod) {
			pairs = new RefactoringPair[1];
			pairs[0].action = IJavaRefactorings.MOVE_METHOD;
			pairs[0].element = rootElement;
		} else if (rootElement instanceof IType) {
			try {
				IMethod[] methods = ((IType) rootElement).getMethods();
				ArrayList<RefactoringPair> pairList = new ArrayList<RefactoringPair>();
				for (IMethod m : methods) {
					if (m.isMainMethod()) {
						continue;
					}
					RefactoringPair pair = new RefactoringPair();
					pair.action = IJavaRefactorings.MOVE_METHOD;
					pair.element = m;
					pairList.add(pair);
				}
				pairs = new RefactoringPair[pairList.size()];
				pairs = pairList.toArray(pairs);
			} catch (JavaModelException e) {
				e.printStackTrace();
				return null;
			}
		}
		return pairs;
	}

	private RefactoringPair[] getMovePairs(IJavaElement rootElement,
			IFile sourceFile) throws JavaModelException {
		RefactoringPair[] pairs = null;
		if (rootElement instanceof IMember) {
			pairs = new RefactoringPair[1];
			pairs[0].action = IJavaRefactorings.MOVE;
			pairs[0].element = rootElement;
		} else if (rootElement instanceof IType) {
			IJavaElement[] elements = CodeImpUtils.getJElementTreeElements(
					rootElement, sourceFile);
			ArrayList<RefactoringPair> pairList = new ArrayList<RefactoringPair>();
			for (IJavaElement e : elements) {
				if (!CodeImpUtils.isInProject(e, sourceFile.getProject())) {
					continue;
				}
				if (e instanceof IMethod && ((IMethod) e).isMainMethod()) {
					continue;
				}
				RefactoringPair pair = new RefactoringPair();
				pair.action = IJavaRefactorings.MOVE;
				pair.element = e;
				pairList.add(pair);
			}
			pairs = new RefactoringPair[pairList.size()];
			pairs = pairList.toArray(pairs);
		}
		return pairs;
	}

	private RefactoringPair[] getExtractClassPairs(IJavaElement rootElement) {
		ArrayList<RefactoringPair> pairList = new ArrayList<RefactoringPair>();
		if (rootElement instanceof IMethod || rootElement instanceof IField) {
			RefactoringPair pair = new RefactoringPair();
			pair.action = IJavaRefactorings.EXTRACT_CLASS;
			pair.element = rootElement;
			pairList.add(pair);
		} else if (rootElement instanceof IType) {
			try {
				IField[] fields = ((IType) rootElement).getFields();
				IMethod[] methods = ((IType) rootElement).getMethods();
				for (IField f : fields) {
					RefactoringPair pair = new RefactoringPair();
					pair.action = IJavaRefactorings.EXTRACT_CLASS;
					pair.element = f;
					pairList.add(pair);
				}
				for (IMethod m : methods) {
					if (m.isMainMethod()) {
						continue;
					}
					RefactoringPair pair = new RefactoringPair();
					pair.action = IJavaRefactorings.EXTRACT_CLASS;
					pair.element = m;
					pairList.add(pair);
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
				return null;
			}
		}
		RefactoringPair[] pairs = new RefactoringPair[pairList.size()];
		pairs = pairList.toArray(pairs);
		return pairs;
	}

	private RefactoringPair[] getDeletePairs(IJavaElement rootElement) {
		RefactoringPair[] pairs = new RefactoringPair[1];
		pairs[0].action = IJavaRefactorings.DELETE;
		pairs[0].element = rootElement;
		return pairs;
	}

	/**
	 * @param rootElement
	 * @param sourceFile
	 * @return
	 * @throws JavaModelException
	 */
	private RefactoringPair[] getRenameLocalVariablePairs(
			IJavaElement rootElement, IFile sourceFile)
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
					pair.action = IJavaRefactorings.RENAME_LOCAL_VARIABLE;
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
	 * @return
	 * @throws JavaModelException
	 */
	private RefactoringPair[] getRenameFieldPairs(IJavaElement rootElement)
			throws JavaModelException {
		if (rootElement instanceof IType) {
			IField[] fields = ((IType) rootElement).getFields();
			RefactoringPair[] pairs = new RefactoringPair[fields.length];
			for (int i = 0; i < fields.length; i++) {
				pairs[i].element = fields[i];
				pairs[i].action = IJavaRefactorings.RENAME_FIELD;
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
	 * @throws CoreException
	 */
	public Refactoring getRefactoring(RefactoringPair pair, IResource project)
			throws CoreException {
		RefactoringContribution contribution = RefactoringCore
				.getRefactoringContribution(pair.action);
		JavaRefactoringDescriptor descriptor = null;
		Refactoring refactoring = null;
		RefactoringStatus status = new RefactoringStatus();
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
			refactoring = createRenameJavaElementRefactoring(pair, descriptor,
					status);
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
			refactoring = createExtractClassRefactoring(pair, descriptor,
					status);
			break;
		case IJavaRefactorings.EXTRACT_CONSTANT:
		case IJavaRefactorings.EXTRACT_INTERFACE:
		case IJavaRefactorings.EXTRACT_LOCAL_VARIABLE:
		case IJavaRefactorings.EXTRACT_METHOD:
			refactoring = createExtractMethodRefactoring(pair, descriptor,
					status);
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
			refactoring = createMoveRefactoring(pair, descriptor, status);
			break;
		case IJavaRefactorings.MOVE_METHOD:
			refactoring = createMoveMethodRefactoring(pair, descriptor, status);
			break;
		case IJavaRefactorings.MOVE_STATIC_MEMBERS:
			refactoring = createMoveStaticRefactoring(pair, descriptor, status);
			break;
		case IJavaRefactorings.PULL_UP:
			refactoring = createPullUpRefactoring(pair, descriptor, status);
			break;
		case IJavaRefactorings.PUSH_DOWN:
			refactoring = createPushDownRefactoring(pair, descriptor, status);
		default:
			break;
		}

		return refactoring;
	}

	private Refactoring createPushDownRefactoring(RefactoringPair pair,
			JavaRefactoringDescriptor descriptor, RefactoringStatus status) {
		PushDownRefactoringProcessor processor = new PushDownRefactoringProcessor(
				new IMember[] { (IMember) pair.element });
		Refactoring refactoring = new ProcessorBasedRefactoring(processor);
		return refactoring;
	}

	private Refactoring createPullUpRefactoring(RefactoringPair pair,
			JavaRefactoringDescriptor descriptor, RefactoringStatus status) {
		PullUpRefactoringProcessor processor = new PullUpRefactoringProcessor(
				new IMember[] { (IMember) pair.element },
				JavaPreferencesSettings
						.getCodeGenerationSettings(((IMember) pair.element)
								.getJavaProject()));
		Refactoring refactoring = new ProcessorBasedRefactoring(processor);
		return refactoring;
	}

	private Refactoring createMoveStaticRefactoring(RefactoringPair pair,
			JavaRefactoringDescriptor descriptor, RefactoringStatus status) {
		MoveStaticMembersProcessor processor = new MoveStaticMembersProcessor(
				new IMember[] { (IMember) pair.element },
				JavaPreferencesSettings
						.getCodeGenerationSettings(((IMember) pair.element)
								.getJavaProject()));
		Refactoring refactoring = new MoveRefactoring(processor);
		return refactoring;
	}

	private Refactoring createMoveMethodRefactoring(RefactoringPair pair,
			JavaRefactoringDescriptor descriptor, RefactoringStatus status) {
		MoveInstanceMethodProcessor processor = new MoveInstanceMethodProcessor(
				(IMethod) pair.element,
				JavaPreferencesSettings
						.getCodeGenerationSettings(((IJavaElement) pair.element)
								.getJavaProject()));
		Refactoring refactoring = new MoveRefactoring(processor);
		return refactoring;
	}

	private Refactoring createMoveRefactoring(RefactoringPair pair,
			JavaRefactoringDescriptor descriptor, RefactoringStatus status)
			throws CoreException {
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
		Refactoring refactoring = moveDescriptor.createRefactoring(status);
		return refactoring;
	}

	private Refactoring createExtractMethodRefactoring(RefactoringPair pair,
			JavaRefactoringDescriptor descriptor, RefactoringStatus status) {
		Refactoring refactoring = new ExtractMethodRefactoring(
				(ICompilationUnit) pair.addition,
				((ITextSelection) pair.element).getOffset(),
				((ITextSelection) pair.element).getLength());
		return refactoring;
	}

	private Refactoring createExtractClassRefactoring(RefactoringPair pair,
			JavaRefactoringDescriptor descriptor, RefactoringStatus status)
			throws CoreException {
		ExtractClassDescriptor extractClassDescriptor = (ExtractClassDescriptor) descriptor;
		extractClassDescriptor.setClassName(((IJavaElement) pair.element)
				.getElementName() + "Class");
		extractClassDescriptor.setCreateTopLevel(true);
		extractClassDescriptor.setType((IType) ((IJavaElement) pair.element)
				.getParent());
		Refactoring refactoring = extractClassDescriptor
				.createRefactoring(status);
		return refactoring;
	}

	/**
	 * @param pair
	 * @param descriptor
	 * @param status
	 * @return
	 * @throws CoreException
	 */
	private Refactoring createRenameJavaElementRefactoring(
			RefactoringPair pair, JavaRefactoringDescriptor descriptor,
			RefactoringStatus status) throws CoreException {
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
		return renameDescriptor.createRefactoring(status);
	}

}
