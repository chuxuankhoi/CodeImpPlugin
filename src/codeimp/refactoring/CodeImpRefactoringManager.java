/**
 * 
 */
package codeimp.refactoring;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.ExtractClassDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.MoveDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.PushDownRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor.ReadyOnlyFieldFinder;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

import codeimp.CodeImpUtils;

/**
 * @author Chu Xuan Khoi
 * 
 */
@SuppressWarnings("restriction")
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
		actionsList = new ArrayList<String>();
		Field[] fields = IJavaRefactorings.class.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			if (Modifier.isStatic(fields[i].getModifiers())) {
				try {
					actionsList.add((String) fields[i].get(null));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					continue;
				}
			}
		}
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
				// pairs = getDeletePairs(rootElement);
				break;
			case IJavaRefactorings.ENCAPSULATE_FIELD:
				break;
			case IJavaRefactorings.EXTRACT_CLASS:
				pairs = getExtractClassPairs(rootElement);
				break;
			case IJavaRefactorings.EXTRACT_CONSTANT:
			case IJavaRefactorings.EXTRACT_INTERFACE:
			case IJavaRefactorings.EXTRACT_LOCAL_VARIABLE:
				break;
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
				try {
					pairs = getPullUpPairs(rootElement, sourceFile);
				} catch (Exception e) {
					e.printStackTrace();
				}
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
			if (code == null) {
				return;
			}
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
				before += "\n";
			}
			before = before.trim();
			String after = "";
			for (int j = i + 1; j < codeLines.length; j++) {
				after += codeLines[j];
				after += "\n";
			}
			after = after.trim();
			// Get pairs from before
			if (!before.equals("")) {
				RefactoringPair pair = new RefactoringPair();
				pair.action = IJavaRefactorings.EXTRACT_METHOD;
				ICompilationUnit cu = JavaCore
						.createCompilationUnitFrom(sourceFile);
				String fileCode = cu.getSource();
				int index = fileCode.indexOf(before);
				int length = before.length();
				if (index < 0) {
					continue;
				}
				TextFileDocumentProvider provider = new TextFileDocumentProvider();
				try {
					provider.connect(sourceFile);
				} catch (CoreException e) {
					e.printStackTrace();
					return;
				}
				IDocument document = provider.getDocument(sourceFile);
				pair.element = new TextSelection(document, index, length);
				pair.addition = cu;
				pairList.add(pair);
			}

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

	private RefactoringPair[] getPullUpPairs(IJavaElement rootElement,
			IFile sourceFile) throws Exception {
		ArrayList<RefactoringPair> pairList = new ArrayList<RefactoringPair>();
		if (rootElement instanceof IMethod) {
			IType currentType = (IType) rootElement.getParent();
			RefactoringPair pair = new RefactoringPair();
			pair.action = IJavaRefactorings.PULL_UP;
			pair.element = rootElement;
			pair.addition = getSuperType(currentType, sourceFile);
			if (pair.addition == null) {
				return null;
			}
			pairList.add(pair);
		} else if (rootElement instanceof IType) {
			try {
				IType currentType = (IType) rootElement;
				IType superType = getSuperType(currentType, sourceFile);
				if (superType == null) {
					return null;
				}
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

	private IType getSuperType(IType type, IFile sourceFile) throws Exception {
		ITypeHierarchy hierachy = type
				.newSupertypeHierarchy(new NullProgressMonitor());
		IType[] superTypes = hierachy.getAllSupertypes(type);
		if (superTypes.length == 0) {
			return null;
		}
		if (CodeImpUtils.isInProject(superTypes[0], type.getJavaProject()
				.getProject())) {
			return superTypes[0];
		} else {
			return null;
		}
	}

	private RefactoringPair[] getMoveStaticPairs(IJavaElement rootElement) {
		RefactoringPair[] pairs = null;
		final IType dest = getTmpClass(rootElement);
		if (dest == null) {
			CodeImpUtils.printLog("Cannot create destination.");
			return pairs;
		}
		try {
			if (rootElement instanceof IField || rootElement instanceof IMethod) {
				if (CodeImpUtils.isStatic(rootElement)) {
					RefactoringPair p = new RefactoringPair();
					p.action = IJavaRefactorings.MOVE_STATIC_MEMBERS;
					p.element = rootElement;
					p.addition = dest;
					pairs = new RefactoringPair[] { p };
				}
			} else if (rootElement instanceof IType) {
				IField[] fields = ((IType) rootElement).getFields();
				IMethod[] methods = ((IType) rootElement).getMethods();
				ArrayList<RefactoringPair> pairList = new ArrayList<RefactoringPair>();
				for (IField f : fields) {
					if (CodeImpUtils.isStatic(f)) {
						RefactoringPair pair = new RefactoringPair();
						pair.action = IJavaRefactorings.MOVE_STATIC_MEMBERS;
						pair.element = f;
						pair.addition = dest;
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
						pair.addition = dest;
						pairList.add(pair);
					}
				}
				pairs = new RefactoringPair[pairList.size()];
				pairs = pairList.toArray(pairs);
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
			return null;
		}
		return pairs;
	}

	private RefactoringPair[] getMoveMethodPairs(IJavaElement rootElement) {
		RefactoringPair[] pairs = null;
		IType dest = getTmpClass(rootElement);
		if (rootElement instanceof IMethod) {
			RefactoringPair p = new RefactoringPair();
			p.action = IJavaRefactorings.MOVE_METHOD;
			p.element = rootElement;
			p.addition = dest;
			pairs = new RefactoringPair[] { p };
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
					pair.addition = dest;
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
		IType dest = getTmpClass(rootElement);
		if (rootElement instanceof IMember) {
			RefactoringPair p = new RefactoringPair();
			p.action = IJavaRefactorings.MOVE;
			p.element = rootElement;
			p.addition = dest;
			pairs = new RefactoringPair[] { p };
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
				if (e == rootElement) {
					continue; // don't move the class
				}
				RefactoringPair pair = new RefactoringPair();
				pair.action = IJavaRefactorings.MOVE;
				pair.element = e;
				pair.addition = dest;
				pairList.add(pair);
			}
			pairs = new RefactoringPair[pairList.size()];
			pairs = pairList.toArray(pairs);
		}
		return pairs;
	}

	private IType getTmpClass(IJavaElement element) {
		IPackageFragment pkg = null;
		if (element instanceof IType) {
			pkg = ((IType) element).getPackageFragment();
		} else if (element instanceof IField || element instanceof IMethod) {
			pkg = ((IType) (element.getParent())).getPackageFragment();
		}
		if (pkg == null) {
			CodeImpUtils.printLog("Cannot get the package information");
			return null;
		}

		String className = "TmpClass_" + System.currentTimeMillis();
		String filename = className + ".java";
		ICompilationUnit icu = null;
		String contents = pkg.getElementName() + "\n";
		contents += ("public class " + className + "{" + "\n");
		contents += ("\n" + "}");

		try {
			icu = pkg.createCompilationUnit(filename, contents, true, null);
		} catch (JavaModelException e) {
			e.printStackTrace();
			return null;
		}

		return icu == null ? null : icu.getType(className);
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

	// private RefactoringPair[] getDeletePairs(IJavaElement rootElement) {
	// RefactoringPair p = new RefactoringPair();
	// p.action = IJavaRefactorings.DELETE;
	// p.element = rootElement;
	// RefactoringPair[] pairs = new RefactoringPair[] { p };
	// return pairs;
	// }

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
				RefactoringPair p = new RefactoringPair();
				p.action = IJavaRefactorings.RENAME_FIELD;
				p.element = fields[i];
				p.addition = "field" + i;
				pairs[i] = p;

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
	public Refactoring getRefactoring(RefactoringPair pair, IFile sourceFile)
			throws CoreException {
		if (pair.element == null || pair.action == null) {
			return null;
		}
		RefactoringContribution contribution = RefactoringCore
				.getRefactoringContribution(pair.action);
		JavaRefactoringDescriptor descriptor = null;
		Refactoring refactoring = null;
		RefactoringStatus status = new RefactoringStatus();
		descriptor = (JavaRefactoringDescriptor) contribution
				.createDescriptor();
		descriptor.setProject(sourceFile.getProject().getName());

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
			refactoring = createMoveMethodRefactoring(pair, descriptor, status,
					contribution);
			break;
		case IJavaRefactorings.MOVE_STATIC_MEMBERS:
			refactoring = createMoveStaticRefactoring(pair, descriptor, status);
			break;
		case IJavaRefactorings.PULL_UP:
			try {
				refactoring = createPullUpRefactoring(pair, descriptor, status);
			} catch (Exception e) {
				e.printStackTrace();
			}
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
			JavaRefactoringDescriptor descriptor, RefactoringStatus status)
			throws Exception {
		PullUpRefactoringProcessor processor = new PullUpRefactoringProcessor(
				new IMember[] { (IMember) pair.element },
				JavaPreferencesSettings
						.getCodeGenerationSettings(((IMember) pair.element)
								.getJavaProject()));
		IType superType = (IType) pair.addition;
		processor.setDestinationType(superType);

		Refactoring refactoring = new ProcessorBasedRefactoring(processor);
		return refactoring;
	}

	private Refactoring createMoveStaticRefactoring(RefactoringPair pair,
			JavaRefactoringDescriptor descriptor, RefactoringStatus status)
			throws JavaModelException {
		MoveStaticMembersProcessor processor = new MoveStaticMembersProcessor(
				new IMember[] { (IMember) pair.element },
				JavaPreferencesSettings
						.getCodeGenerationSettings(((IMember) pair.element)
								.getJavaProject()));
		processor.setDestinationTypeFullyQualifiedName(((IType) pair.addition)
				.getFullyQualifiedName());
		Refactoring refactoring = new MoveRefactoring(processor);
		return refactoring;
	}

	private Refactoring createMoveMethodRefactoring(RefactoringPair pair,
			JavaRefactoringDescriptor descriptor, RefactoringStatus status,
			RefactoringContribution contribution) throws CoreException {
		IMethod method = (IMethod) pair.element;
		MoveInstanceMethodProcessor processor = new MoveInstanceMethodProcessor(
				method,
				JavaPreferencesSettings.getCodeGenerationSettings(method
						.getJavaProject()));
		// Calculate candidate for the target
		IVariableBinding[] candidateTargets = calculateTarget((IMethod) pair.element);
		if (candidateTargets.length == 0) {
			return null;
		}
		processor.setTarget(candidateTargets[0]);

		return new MoveRefactoring(processor);
	}

	private IVariableBinding[] calculateTarget(IMethod element)
			throws JavaModelException {
		CompilationUnitRewrite fSourceRewrite = new CompilationUnitRewrite(
				element.getCompilationUnit());
		final MethodDeclaration declaration = ASTNodeSearchUtil
				.getMethodDeclarationNode(element, fSourceRewrite.getRoot());

		final List<IVariableBinding> candidateTargets = new ArrayList<IVariableBinding>(
				16);
		final IMethodBinding method = declaration.resolveBinding();
		if (method != null) {
			final ITypeBinding declaring = method.getDeclaringClass();
			IVariableBinding[] bindings = getArgumentBindings(declaration);
			ITypeBinding binding = null;
			for (int index = 0; index < bindings.length; index++) {
				binding = bindings[index].getType();
				if ((binding.isClass() || binding.isEnum())
						&& binding.isFromSource()) {
					candidateTargets.add(bindings[index]);
				}
			}
			final ReadyOnlyFieldFinder visitor = new ReadyOnlyFieldFinder(
					declaring);
			declaration.accept(visitor);
			bindings = visitor.getDeclaredFields();
			for (int index = 0; index < bindings.length; index++) {
				binding = bindings[index].getType();
				if (binding.isClass() && binding.isFromSource())
					candidateTargets.add(bindings[index]);
			}
		}
		IVariableBinding[] fCandidateTargets = new IVariableBinding[candidateTargets
				.size()];
		candidateTargets.toArray(fCandidateTargets);
		return fCandidateTargets;
	}

	@SuppressWarnings("unchecked")
	private IVariableBinding[] getArgumentBindings(MethodDeclaration declaration) {
		final List<IVariableBinding> parameters = new ArrayList<IVariableBinding>(
				declaration.parameters().size());
		for (final Iterator<SingleVariableDeclaration> iterator = declaration
				.parameters().iterator(); iterator.hasNext();) {
			VariableDeclaration variable = iterator.next();
			IVariableBinding binding = variable.resolveBinding();
			if (binding == null)
				return new IVariableBinding[0];
			parameters.add(binding);
		}
		final IVariableBinding[] result = new IVariableBinding[parameters
				.size()];
		parameters.toArray(result);
		return result;
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
