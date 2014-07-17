package codeimp.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.jdt.ui.refactoring.RenameSupport;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ui.IWorkbenchWindow;

import codeimp.CodeImpAbstract;
import codeimp.CodeImpHillClimbing;
import codeimp.refactoring.CodeImpRefactoringManager;
import codeimp.refactoring.RefactoringPair;

@SuppressWarnings("restriction")
public class CodeImpWizard extends Wizard {

	private AnalysisPage aPage = null;
	private RefactoringSelectionPage sPage = null;
	private FinalPage iPage = null;
	private ITextSelection selection;
	private IFile file;
	private IWorkbenchWindow mainWindow;
	private CodeImpAbstract improvementJob = null;

	@Override
	public boolean performFinish() {
		RefactoringPair[] pairs = improvementJob.getEffectivePairs(sPage
				.getSelectedAction());
		if (pairs == null || pairs.length == 0) {
			return true;
		}
		// TODO Call standard refactorings for the pairs
		CodeImpRefactoringManager refMan = CodeImpRefactoringManager
				.getManager();
		for (RefactoringPair pair : pairs) {
			if (pair.action == IJavaRefactorings.RENAME_COMPILATION_UNIT
					|| pair.action == IJavaRefactorings.RENAME_ENUM_CONSTANT
					|| pair.action == IJavaRefactorings.RENAME_FIELD
					|| pair.action == IJavaRefactorings.RENAME_JAVA_PROJECT
					|| pair.action == IJavaRefactorings.RENAME_LOCAL_VARIABLE
					|| pair.action == IJavaRefactorings.RENAME_METHOD
					|| pair.action == IJavaRefactorings.RENAME_PACKAGE
					|| pair.action == IJavaRefactorings.RENAME_SOURCE_FOLDER
					|| pair.action == IJavaRefactorings.RENAME_TYPE
					|| pair.action == IJavaRefactorings.RENAME_TYPE_PARAMETER) {
				RenameSupport support;
				try {
					support = createRenameSupport((IJavaElement) pair.element,
							null, RenameSupport.UPDATE_REFERENCES);
					if (support != null && support.preCheck().isOK())
						support.openDialog(getShell());
				} catch (CoreException e) {
					e.printStackTrace();
				}	
			} else {
				RefactoringWizard wizard;
				try {
					wizard = refMan.getWizard(pair, file);
					new RefactoringStarter()
					.activate(
							wizard,
							getShell(),
							RefactoringMessages.OpenRefactoringWizardAction_refactoring,
							RefactoringSaveHelper.SAVE_REFACTORING);
				} catch (CoreException e) {
					e.printStackTrace();
				}				
			}
		}
		return true;
	}

	private RenameSupport createRenameSupport(IJavaElement element, String newName,
			int flags) throws CoreException {
		switch (element.getElementType()) {
		case IJavaElement.JAVA_PROJECT:
			return RenameSupport.create((IJavaProject) element, newName, flags);
		case IJavaElement.PACKAGE_FRAGMENT_ROOT:
			return RenameSupport.create((IPackageFragmentRoot) element, newName);
		case IJavaElement.PACKAGE_FRAGMENT:
			return RenameSupport.create((IPackageFragment) element, newName, flags);
		case IJavaElement.COMPILATION_UNIT:
			return RenameSupport.create((ICompilationUnit) element, newName, flags);
		case IJavaElement.TYPE:
			return RenameSupport.create((IType) element, newName, flags);
		case IJavaElement.METHOD:
			final IMethod method= (IMethod) element;
			if (method.isConstructor())
				return createRenameSupport(method.getDeclaringType(), newName, flags);
			else
				return RenameSupport.create((IMethod) element, newName, flags);
		case IJavaElement.FIELD:
			return RenameSupport.create((IField) element, newName, flags);
		case IJavaElement.TYPE_PARAMETER:
			return RenameSupport.create((ITypeParameter) element, newName, flags);
		case IJavaElement.LOCAL_VARIABLE:
			return RenameSupport.create((ILocalVariable) element, newName, flags);
	}
	return null;
	}

	public CodeImpWizard(ITextSelection textSelection, IFile curEditorFile,
			IWorkbenchWindow window) {
		super();
		setNeedsProgressMonitor(true);
		selection = textSelection;
		file = curEditorFile;
		mainWindow = window;
		improvementJob = new CodeImpHillClimbing(selection, file, mainWindow);
	}

	@Override
	public void addPages() {

		aPage = new AnalysisPage("Analysis", improvementJob);
		sPage = new RefactoringSelectionPage("Selection");
		iPage = new FinalPage("Improve", improvementJob);
		addPage(aPage);
		addPage(sPage);
		addPage(iPage);
	}

	@Override
	public boolean performCancel() {
		if (aPage != null) {
			aPage.tryCancel();
		}
		return super.performCancel();
	}

	@Override
	public boolean canFinish() {
		if (getContainer().getCurrentPage() == iPage) {
			return true;
		} else {
			return false;
		}
	}

}
