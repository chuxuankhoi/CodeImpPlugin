/**
 * 
 */
package codeimp.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
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
import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractClassRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.PushDownRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor.ReadyOnlyFieldFinder;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.ExtractClassWizard;
import org.eclipse.jdt.internal.ui.refactoring.MoveInstanceMethodWizard;
import org.eclipse.jdt.internal.ui.refactoring.MoveMembersWizard;
import org.eclipse.jdt.internal.ui.refactoring.PullUpWizard;
import org.eclipse.jdt.internal.ui.refactoring.PushDownWizard;
import org.eclipse.jdt.internal.ui.refactoring.code.ExtractMethodWizard;
import org.eclipse.jdt.internal.ui.refactoring.reorg.CreateTargetQueries;
import org.eclipse.jdt.internal.ui.refactoring.reorg.ReorgMoveWizard;
import org.eclipse.jdt.internal.ui.refactoring.reorg.ReorgQueries;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveProcessor;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.internal.core.refactoring.RefactoringCorePlugin;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

import codeimp.CodeImpUtils;

/**
 * @author Chu Xuan Khoi
 * 
 */
@SuppressWarnings("restriction")
public class CodeImpRefactoringManager {

	/** The class attribute */
	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$

	/** The id attribute */
	private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$

	/** The refactoring contributions extension point */
	private static final String REFACTORING_CONTRIBUTIONS_EXTENSION_POINT = "refactoringContributions"; //$NON-NLS-1$

	private static CodeImpRefactoringManager manager = null;
	private static IType tmpClass = null;

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
		// Collect all refactoring actions Eclipse supports
		getActionList(actionsList);
		// actionsList.add(IJavaRefactorings.EXTRACT_METHOD);

	}

	private void getActionList(ArrayList<String> out) {
		final IConfigurationElement[] elements = Platform
				.getExtensionRegistry().getConfigurationElementsFor(
						RefactoringCore.ID_PLUGIN,
						REFACTORING_CONTRIBUTIONS_EXTENSION_POINT);
		for (int index = 0; index < elements.length; index++) {
			final IConfigurationElement element = elements[index];
			final String attributeId = element.getAttribute(ATTRIBUTE_ID);
			if (attributeId != null && !"".equals(attributeId)) { //$NON-NLS-1$
				final String className = element.getAttribute(ATTRIBUTE_CLASS);
				if (className != null && !"".equals(className)) { //$NON-NLS-1$
					try {
						final Object implementation = element
								.createExecutableExtension(ATTRIBUTE_CLASS);
						if (implementation instanceof RefactoringContribution) {
							out.add(attributeId);
						}
					} catch (CoreException exception) {
						RefactoringCorePlugin.log(exception);
					}
				}
			}
		}
	}

	/**
	 * Get all refactoring actions name
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
	 * TODO automate the Refactoring generation
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
	public RefactoringPair[] getDefaultRefactoringPairs(IJavaElement rootElement,
			String action) {
		RefactoringPair[] pairs = null;
		try {
			switch (action) {
			case IJavaRefactorings.RENAME_FIELD:
				// Get all fields declared in the root element (class)
				// pairs = getRenameFieldPairs(rootElement);
				// break;
			case IJavaRefactorings.RENAME_LOCAL_VARIABLE:
				// Get all local variables declared in the root element (method)
				// pairs = getRenameLocalVariablePairs(rootElement, sourceFile);
				// break;
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
				pairs = getExtractMethodPairs(rootElement);
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
				pairs = getMovePairs(rootElement);
				break;
			case IJavaRefactorings.MOVE_METHOD:
				pairs = getMoveMethodPairs(rootElement);
				break;
			case IJavaRefactorings.MOVE_STATIC_MEMBERS:
				pairs = getMoveStaticPairs(rootElement);
				break;
			case IJavaRefactorings.PULL_UP:
				try {
					pairs = getPullUpPairs(rootElement);
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

	private RefactoringPair[] getExtractMethodPairs(IJavaElement rootElement)
			throws JavaModelException {
		ArrayList<RefactoringPair> pairList = new ArrayList<RefactoringPair>();
		// Find the source file of the rootElement
		IFile file = getRootElementFile(rootElement);
		if (rootElement instanceof IMethod) {
			getExtractMethodPairsInMethod((IMethod) rootElement, pairList, file);
		} else if (rootElement instanceof IType) {
			getExtractMethodPairsInClass((IType) rootElement, pairList, file);
		}
		RefactoringPair[] pairs = new RefactoringPair[pairList.size()];
		pairs = pairList.toArray(pairs);
		return pairs;
	}

	private IFile getRootElementFile(IJavaElement rootElement) {
		IResource res = rootElement.getResource();
		if (res == null || !(res instanceof IFile)) {
			return null;
		} else {
			return (IFile) res;
		}
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
				pair.element = before;
				pair.addition = cu;
				pair.resource = sourceFile;
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
			pair.resource = rootElement.getResource();
			pairList.add(pair);
		} else if (rootElement instanceof IType) {
			IResource resource = rootElement.getResource();
			try {
				IField[] fields = ((IType) rootElement).getFields();
				IMethod[] methods = ((IType) rootElement).getMethods();
				for (IField f : fields) {
					RefactoringPair pair = new RefactoringPair();
					pair.action = IJavaRefactorings.PUSH_DOWN;
					pair.element = f;
					pair.resource = resource;
					pairList.add(pair);
				}
				for (IMethod m : methods) {
					RefactoringPair pair = new RefactoringPair();
					pair.action = IJavaRefactorings.PUSH_DOWN;
					pair.element = m;
					pair.resource = resource;
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

	private RefactoringPair[] getPullUpPairs(IJavaElement rootElement)
			throws Exception {
		IFile sourceFile = getRootElementFile(rootElement);
		ArrayList<RefactoringPair> pairList = new ArrayList<RefactoringPair>();
		if (rootElement instanceof IMethod) {
			IType currentType = (IType) rootElement.getParent();
			RefactoringPair pair = new RefactoringPair();
			pair.action = IJavaRefactorings.PULL_UP;
			pair.element = rootElement;
			pair.addition = getSuperType(currentType, sourceFile);
			pair.resource = sourceFile;
			if (pair.addition == null) {
				return null;
			}
			pairList.add(pair);
		} else if (rootElement instanceof IType) {
			try {
				IType currentType = (IType) rootElement;
				IType superType = getSuperType(currentType, sourceFile);
				if (superType == null) {
					System.out.println("No super class");
					return null;
				}
				IField[] fields = ((IType) rootElement).getFields();
				IMethod[] methods = ((IType) rootElement).getMethods();
				for (IField f : fields) {
					RefactoringPair pair = new RefactoringPair();
					pair.action = IJavaRefactorings.PULL_UP;
					pair.element = f;
					pair.addition = superType;
					pair.resource = sourceFile;
					pairList.add(pair);
				}
				for (IMethod m : methods) {
					RefactoringPair pair = new RefactoringPair();
					pair.action = IJavaRefactorings.PULL_UP;
					pair.element = m;
					pair.addition = superType;
					pair.resource = sourceFile;
					pairList.add(pair);
				}
			} catch (JavaModelException e) {
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
					p.resource = rootElement.getResource();
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
						pair.resource = rootElement.getResource();
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
						pair.resource = rootElement.getResource();
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
			p.resource = rootElement.getResource();
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
					pair.resource = rootElement.getResource();
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

	// Not for methods and static elements
	private RefactoringPair[] getMovePairs(IJavaElement rootElement)
			throws JavaModelException {
		RefactoringPair[] pairs = null;
		IType dest = getTmpClass(rootElement);
		IFile sourceFile = getRootElementFile(rootElement);
		if (rootElement instanceof IField) {
			RefactoringPair p = new RefactoringPair();
			p.action = IJavaRefactorings.MOVE;
			p.element = rootElement;
			p.addition = dest;
			p.resource = rootElement.getResource();
			pairs = new RefactoringPair[] { p };
		} else if (rootElement instanceof IType) {
			IJavaElement[] elements = CodeImpUtils.getJElementTreeElements(
					rootElement, sourceFile);
			ArrayList<RefactoringPair> pairList = new ArrayList<RefactoringPair>();
			for (IJavaElement e : elements) {
				if (e instanceof IType) {
					continue; // don't move class
				}
				if (!CodeImpUtils.isInProject(e, sourceFile.getProject())) {
					continue;
				}
				if (e instanceof IMethod) {
					continue;
				}
				if (CodeImpUtils.isStatic(e)) {
					continue;
				}

				RefactoringPair pair = new RefactoringPair();
				pair.action = IJavaRefactorings.MOVE;
				pair.element = e;
				pair.addition = dest;
				pair.resource = rootElement.getResource();
				pairList.add(pair);
			}
			pairs = new RefactoringPair[pairList.size()];
			pairs = pairList.toArray(pairs);
		}
		return pairs;
	}

	private IType getTmpClass(IJavaElement element) {
		if (tmpClass == null) {
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
				tmpClass = icu == null ? null : icu.getType(className);
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return tmpClass;
	}

	public void deleteTmpClass() {
		if (tmpClass == null) {
			return;
		}
		ICompilationUnit icu = tmpClass.getCompilationUnit();
		try {
			icu.delete(true, null);
		} catch (JavaModelException e) {
			// e.printStackTrace();
		}
		tmpClass = null;
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
					pair.resource = rootElement.getResource();
					pairList.add(pair);
				}
				for (IMethod m : methods) {
					if (m.isMainMethod()) {
						continue;
					}
					RefactoringPair pair = new RefactoringPair();
					pair.action = IJavaRefactorings.EXTRACT_CLASS;
					pair.element = m;
					pair.resource = rootElement.getResource();
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

	// /**
	// * @param rootElement
	// * @param sourceFile
	// * @return
	// * @throws JavaModelException
	// */
	// private RefactoringPair[] getRenameLocalVariablePairs(
	// IJavaElement rootElement, IFile sourceFile)
	// throws JavaModelException {
	// if (rootElement instanceof IMethod) {
	// IJavaElement[] allElements = CodeImpUtils.identifyElements(
	// CodeImpUtils.getBody((IMethod) rootElement), sourceFile);
	// if (allElements == null || allElements.length == 0) {
	// return null;
	// }
	// ArrayList<RefactoringPair> pairList = new ArrayList<RefactoringPair>();
	// for (int i = 0; i < allElements.length; i++) {
	// if (allElements[i] instanceof ILocalVariable) {
	// RefactoringPair pair = new RefactoringPair();
	// pair.element = allElements[i];
	// pair.action = IJavaRefactorings.RENAME_LOCAL_VARIABLE;
	// pair.addition = "local" + i;
	// pair.resource = rootElement.getResource();
	// pairList.add(pair);
	// }
	// }
	// if (pairList.size() == 0) {
	// return null;
	// }
	// RefactoringPair[] pairs = new RefactoringPair[pairList.size()];
	// pairList.toArray(pairs);
	// return pairs;
	// }
	// return null;
	// }
	//
	// /**
	// * @param rootElement
	// * @return
	// * @throws JavaModelException
	// */
	// private RefactoringPair[] getRenameFieldPairs(IJavaElement rootElement)
	// throws JavaModelException {
	// if (rootElement instanceof IType) {
	// IField[] fields = ((IType) rootElement).getFields();
	// RefactoringPair[] pairs = new RefactoringPair[fields.length];
	// for (int i = 0; i < fields.length; i++) {
	// RefactoringPair p = new RefactoringPair();
	// p.action = IJavaRefactorings.RENAME_FIELD;
	// p.element = fields[i];
	// p.addition = "field" + i;
	// p.resource = rootElement.getResource();
	// pairs[i] = p;
	//
	// }
	// return pairs;
	// }
	// return null;
	// }

	/**
	 * Depend on the action user expected, the function creates a descriptor
	 * which contains enough information for the refactoring
	 * 
	 * @param pair
	 * @param project
	 * @return
	 * @throws CoreException
	 */
	public Refactoring getRefactoring(RefactoringPair pair)
			throws CoreException {
		if (pair.element == null || pair.action == null) {
			System.out.println("No element or no action.");
			return null;
		}
		RefactoringContribution contribution = RefactoringCore
				.getRefactoringContribution(pair.action);
		JavaRefactoringDescriptor descriptor = null;
		Refactoring refactoring = null;
		RefactoringStatus status = new RefactoringStatus();
		descriptor = (JavaRefactoringDescriptor) contribution
				.createDescriptor();
		descriptor.setProject(pair.resource.getProject().getName());

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
			// refactoring = createRenameJavaElementRefactoring(pair,
			// descriptor,
			// status);
			// break;
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
		// Assign methods that should be deleted
		if (pair.element instanceof IMethod) {
			// Get all brothers of current type
			ITypeHierarchy hierachy = superType
					.newTypeHierarchy(new NullProgressMonitor());
			IType[] types = hierachy.getSubtypes(superType);
			// Get all methods that the same with the element
			IMethod[] deletedMethods = getDuplicatedMethods(
					(IMethod) pair.element, types);
			// Set deleted items
			if (deletedMethods != null) {
				processor.setDeletedMethods(deletedMethods);
			}
		}

		Refactoring refactoring = new ProcessorBasedRefactoring(processor);
		return refactoring;
	}

	private IMethod[] getDuplicatedMethods(IMethod method, IType[] types) {
		if (method == null) {
			return null;
		}
		ArrayList<IMethod> retMethods = new ArrayList<IMethod>();
		for (IType t : types) {
			IMethod[] methods = null;
			try {
				methods = t.getMethods();
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			if (methods != null) {
				for (IMethod m : methods) {
					try {
						if (CodeImpUtils.getBody(m).equals(
								CodeImpUtils.getBody(method))) {
							retMethods.add(m);
						}
					} catch (JavaModelException e) {
						// e.printStackTrace();
					}
				}
			}
		}
		if (retMethods.size() == 0) {
			return null;
		} else {
			return retMethods.toArray(new IMethod[retMethods.size()]);
		}
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
			CodeImpUtils.printLog("No candidate target for " + pair.action);
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
		if (pair.element instanceof IField) {
			IField[] members = { (IField) pair.element };
			moveDescriptor.setMoveMembers(members);
		} else if (pair.element instanceof IPackageFragmentRoot) {
			// Have not supported
		} else if (pair.element instanceof IPackageFragment) {
			// Have not supported
		} else if (pair.element instanceof IResource) {
			// Have not supported
		} else {
			// Do nothing
		}
		Refactoring refactoring = moveDescriptor.createRefactoring(status);
		return refactoring;
	}

	private Refactoring createExtractMethodRefactoring(RefactoringPair pair,
			JavaRefactoringDescriptor descriptor, RefactoringStatus status) {
		ICompilationUnit cu = (ICompilationUnit) pair.addition;
		String fileCode = null;
		try {
			fileCode = cu.getSource();
		} catch (JavaModelException e1) {
			e1.printStackTrace();
			return null;
		}
		String str = (String) pair.element;
		// System.out.println("Finding string: " + str + " in "
		// + cu.getResource().getName());
		int index = fileCode.indexOf(str);
		int length = str.length();
		if (index < 0) {
			System.out.println("Cannot find the string in the code");
			return null;
		}
		TextFileDocumentProvider provider = new TextFileDocumentProvider();
		try {
			provider.connect(cu.getResource());
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}

		IDocument document = provider.getDocument(cu.getResource());
		ITextSelection selection = new TextSelection(document, index, length);
		selection = completeSelection(selection, cu, document);
		ExtractMethodRefactoring refactoring = new ExtractMethodRefactoring(cu,
				selection.getOffset(), selection.getLength());
		refactoring.setMethodName("extract_" + System.currentTimeMillis());
		refactoring.setReplaceDuplicates(true);
		return refactoring;
	}

	private ITextSelection completeSelection(ITextSelection selection,
			ITypeRoot typeRoot, IDocument doc) {
		ISourceRange sourceRange;
		try {
			sourceRange = typeRoot.getSourceRange();
			if (sourceRange == null || sourceRange.getLength() == 0) {
				return null;
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
			return null;
		}
		ISourceRange newRange = null;
		try {
			newRange = getNewSelectionRange(createSourceRange(selection),
					typeRoot);
		} catch (JavaModelException e) {
			e.printStackTrace();
			return null;
		}
		// Check if new selection differs from current selection
		if (selection.getOffset() == newRange.getOffset()
				&& selection.getLength() == newRange.getLength()) {
			System.out.println("The same as input");
			return selection;
		}
		return new TextSelection(doc, newRange.getOffset(),
				newRange.getLength());
	}

	private ISourceRange getNewSelectionRange(ISourceRange oldSourceRange,
			ITypeRoot typeRoot) throws JavaModelException {
		CompilationUnit root = getAST(typeRoot);
		if (root == null)
			return oldSourceRange;
		Selection selection = Selection.createFromStartLength(
				oldSourceRange.getOffset(), oldSourceRange.getLength());
		SelectionAnalyzer selAnalyzer = new SelectionAnalyzer(selection, true);
		root.accept(selAnalyzer);
		return internalGetNewSelectionRange(oldSourceRange, typeRoot,
				selAnalyzer);
	}

	private ISourceRange internalGetNewSelectionRange(
			ISourceRange oldSourceRange, ISourceReference sr,
			SelectionAnalyzer selAnalyzer) throws JavaModelException {
		ASTNode first = selAnalyzer.getFirstSelectedNode();
		if (first == null || first.getParent() == null)
			return getLastCoveringNodeRange(oldSourceRange, sr, selAnalyzer);

		return getSelectedNodeSourceRange(sr, first.getParent());
	}

	private ISourceRange getLastCoveringNodeRange(ISourceRange oldSourceRange,
			ISourceReference sr, SelectionAnalyzer selAnalyzer)
			throws JavaModelException {
		if (selAnalyzer.getLastCoveringNode() == null)
			return oldSourceRange;
		else
			return getSelectedNodeSourceRange(sr,
					selAnalyzer.getLastCoveringNode());
	}

	private ISourceRange getSelectedNodeSourceRange(ISourceReference sr,
			ASTNode nodeToSelect) throws JavaModelException {
		int offset = nodeToSelect.getStartPosition();
		int end = Math.min(sr.getSourceRange().getLength(),
				nodeToSelect.getStartPosition() + nodeToSelect.getLength() - 1);
		return createSourceRange(offset, end);
	}

	private ISourceRange createSourceRange(int offset, int end) {
		int length = end - offset + 1;
		if (length == 0) // to allow 0-length selection
			length = 1;
		return new SourceRange(Math.max(0, offset), length);
	}

	private CompilationUnit getAST(ITypeRoot sr) {
		return SharedASTProvider.getAST(sr, SharedASTProvider.WAIT_YES, null);
	}

	private ISourceRange createSourceRange(ITextSelection selection) {
		return new SourceRange(selection.getOffset(), selection.getLength());
	}

	private Refactoring createExtractClassRefactoring(RefactoringPair pair,
			JavaRefactoringDescriptor descriptor, RefactoringStatus status)
			throws CoreException {
		ExtractClassDescriptor extractClassDescriptor = (ExtractClassDescriptor) descriptor;
		if (pair.addition == null) {
			extractClassDescriptor.setClassName(((IJavaElement) pair.element)
					.getElementName() + "Class");
			extractClassDescriptor.setCreateTopLevel(true);
			IType rootType = (IType) ((IJavaElement) pair.element).getParent();
			extractClassDescriptor.setType(rootType);
			if (pair.element instanceof IField) {
				ExtractClassDescriptor.Field[] fields = ExtractClassDescriptor
						.getFields(rootType);
				for (int i = 0; i < fields.length; i++) {
					if (fields[i].getFieldName().equals(
							((IField) pair.element).getElementName())) {
						fields[i].setCreateField(true);
					} else {
						fields[i].setCreateField(false);
					}
				}
				extractClassDescriptor.setFields(fields);
			} else {
				return null;
			}
		}
		return new ExtractClassRefactoring(extractClassDescriptor);
	}

	// /**
	// * @param pair
	// * @param descriptor
	// * @param status
	// * @return
	// * @throws CoreException
	// */
	// private Refactoring createRenameJavaElementRefactoring(
	// RefactoringPair pair, JavaRefactoringDescriptor descriptor,
	// RefactoringStatus status) throws CoreException {
	// RenameJavaElementDescriptor renameDescriptor =
	// (RenameJavaElementDescriptor) descriptor;
	// renameDescriptor.setJavaElement((IJavaElement) pair.element);
	// renameDescriptor
	// .setNewName(pair.addition == null ? ((IJavaElement) pair.element)
	// .getElementName() + "NewName"
	// : (String) pair.addition);
	// renameDescriptor.setDeprecateDelegate(true);
	// renameDescriptor.setKeepOriginal(false);
	// renameDescriptor.setUpdateReferences(true);
	// renameDescriptor.setUpdateTextualOccurrences(true);
	// return renameDescriptor.createRefactoring(status);
	// }

	public RefactoringWizard getWizard(RefactoringPair pair)
			throws CoreException {
		RefactoringWizard wizard = null;
		Refactoring refactoring = null;
		RefactoringProcessor processor = null;
		IJavaElement[] elements = null;
		IJavaProject project = null;

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
			ExtractClassDescriptor descriptor = RefactoringSignatureDescriptorFactory
					.createExtractClassDescriptor();
			IType rootType = (IType) ((IJavaElement) pair.element).getParent();
			descriptor.setType(rootType);
			descriptor.setProject(pair.resource.getProject().getName());
			if (pair.element instanceof IField) {
				ExtractClassDescriptor.Field[] fields = ExtractClassDescriptor
						.getFields(rootType);
				for (int i = 0; i < fields.length; i++) {
					if (fields[i].getFieldName().equals(
							((IField) pair.element).getElementName())) {
						fields[i].setCreateField(true);
					} else {
						fields[i].setCreateField(false);
					}
				}
				descriptor.setFields(fields);
			}
			refactoring = new ExtractClassRefactoring(descriptor);
			wizard = new ExtractClassWizard(descriptor, refactoring);
			break;
		case IJavaRefactorings.EXTRACT_CONSTANT:
		case IJavaRefactorings.EXTRACT_INTERFACE:
		case IJavaRefactorings.EXTRACT_LOCAL_VARIABLE:
		case IJavaRefactorings.EXTRACT_METHOD:
			refactoring = getRefactoring(pair);
			wizard = new ExtractMethodWizard(
					(ExtractMethodRefactoring) refactoring);
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
			System.out.println("file: " + pair.resource.getName()
					+ ", element: "
					+ ((IJavaElement) pair.element).getElementName());
			IResource[] resources = new IResource[] { pair.resource };
			elements = new IJavaElement[] { (IJavaElement) pair.element };
			IMovePolicy policy = ReorgPolicyFactory.createMovePolicy(resources,
					elements);
			if (policy.canEnable()) {
				processor = new JavaMoveProcessor(policy);
				refactoring = new MoveRefactoring((MoveProcessor) processor);
				wizard = new ReorgMoveWizard((JavaMoveProcessor) processor,
						refactoring);
				((JavaMoveProcessor) processor)
						.setCreateTargetQueries(new CreateTargetQueries(wizard));
				((JavaMoveProcessor) processor)
						.setReorgQueries(new ReorgQueries(wizard));
			} else {
				System.out.println("policy.canEnable() == false");
			}
			break;
		case IJavaRefactorings.MOVE_METHOD:
			processor = new MoveInstanceMethodProcessor(
					(IMethod) pair.element,
					JavaPreferencesSettings
							.getCodeGenerationSettings(((IJavaElement) pair.element)
									.getJavaProject()));
			refactoring = new MoveRefactoring((MoveProcessor) processor);
			wizard = new MoveInstanceMethodWizard(
					(MoveInstanceMethodProcessor) processor, refactoring);
			break;
		case IJavaRefactorings.MOVE_STATIC_MEMBERS:
			IMember[] moveStaticMembers = new IMember[] { (IMember) pair.element };
			if (!RefactoringAvailabilityTester
					.isMoveStaticAvailable(moveStaticMembers))
				return null;
			final Set<IMember> set = new HashSet<IMember>();
			set.addAll(Arrays.asList(moveStaticMembers));
			elements = set.toArray(new IMember[set.size()]);
			if (elements.length > 0)
				project = elements[0].getJavaProject();
			processor = new MoveStaticMembersProcessor((IMember[]) elements,
					JavaPreferencesSettings.getCodeGenerationSettings(project));
			refactoring = new MoveRefactoring((MoveProcessor) processor);
			wizard = new MoveMembersWizard(
					(MoveStaticMembersProcessor) processor, refactoring);
			break;
		case IJavaRefactorings.PULL_UP:
			IMember[] pullUpMembers = new IMember[] { (IMember) pair.element };
			if (!RefactoringAvailabilityTester.isPullUpAvailable(pullUpMembers))
				return null;
			if (pullUpMembers != null && pullUpMembers.length > 0)
				project = pullUpMembers[0].getJavaProject();
			processor = new PullUpRefactoringProcessor(pullUpMembers,
					JavaPreferencesSettings.getCodeGenerationSettings(project));
			refactoring = new ProcessorBasedRefactoring(processor);
			wizard = new PullUpWizard((PullUpRefactoringProcessor) processor,
					refactoring);
			break;
		case IJavaRefactorings.PUSH_DOWN:
			IMember[] pushDownMembers = new IMember[] { (IMember) pair.element };
			if (!RefactoringAvailabilityTester
					.isPushDownAvailable(pushDownMembers))
				return null;
			processor = new PushDownRefactoringProcessor(pushDownMembers);
			refactoring = new ProcessorBasedRefactoring(processor);
			wizard = new PushDownWizard(
					(PushDownRefactoringProcessor) processor, refactoring);
			break;
		default:
			break;
		}
		return wizard;
	}

}
