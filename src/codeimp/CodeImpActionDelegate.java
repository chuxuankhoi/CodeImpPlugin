/**
 * 
 */
package codeimp;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * @author chuxuankhoi
 * 
 */
public class CodeImpActionDelegate implements IWorkbenchWindowActionDelegate {
	IWorkbenchWindow window = null;
	IFile file = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(IAction action) {
		MessageDialog.openInformation(window.getShell(),
				"Have not completed yet", "CodeImp is under construction");

		// Check Java perspective and editor
		if (!isPerspective("Java")) {
			MessageDialog.openInformation(window.getShell(),
					"Checking current perspective ...",
					"Current version have not support the language.");
			return;
		}

		// Check opening file to make sure that it is a java file
		file = getCurrentEditorFile();
		if (!file.getFileExtension().equals("java")) {
			MessageDialog.openInformation(window.getShell(),
					"Checking file extension ...",
					"Current version have not support the file extension.");
		}

		// Get the selected code in the current editor
		String selectedCode = getSelectedCode();
		if (selectedCode.isEmpty()) {
			MessageDialog.openInformation(window.getShell(),
					"Getting selected text ...", "No text is selected.");
			return;
		}

		// Print current status of the selected code
		System.out.println("Selected Code: " + selectedCode);
		System.out.println("Language: Java");
		System.out.println("File: " + file.getName());
		System.out.println("Project: " + file.getProject().getName());

		// TODO Analyse code smell

		// TODO Display the analysis and confirm the changed blocks
		// ConfirmationDlg dlg = new ConfirmationDlg(window);
		// dlg.open();

		// TODO Call refactoring module to change the code
		ICompilationUnit cu = JavaCore.createCompilationUnitFrom(file);
		// Find the type of the selected code in the file
		int selectedCodeType = findCodeType(selectedCode, cu);
		IJavaElement selectedCodeElement = findCodeElement(selectedCode, cu);

		if (selectedCodeElement == null) {
			return;
		}
		RefactoringContribution contribution = RefactoringCore
				.getRefactoringContribution(IJavaRefactorings.RENAME_FIELD);
		RenameJavaElementDescriptor descriptor = (RenameJavaElementDescriptor) contribution
				.createDescriptor();
		descriptor.setProject(file.getProject().getName());
		descriptor.setNewName("testRefactoring");
		descriptor.setJavaElement(selectedCodeElement);

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

	private boolean isPerspective(String expectedPerspective) {
		String curPerspective = window.getActivePage().getPerspective()
				.getLabel();
		if (curPerspective.equals(expectedPerspective)) {
			return true;
		}
		return false;
	}

	private IFile getCurrentEditorFile() {
		IFile curFile = null;
		IEditorPart editorPart = window.getActivePage().getActiveEditor();
		if (editorPart != null) {
			IFileEditorInput input = (IFileEditorInput) editorPart
					.getEditorInput();
			curFile = input.getFile();
		}
		return curFile;
	}

	private String getSelectedCode() {
		IWorkbenchPartSite site = window.getPartService().getActivePart()
				.getSite();
		if (site == null) {
			MessageDialog.openInformation(window.getShell(),
					"Getting selected text ...", "No workbench part site.");
			return "";
		}
		ISelectionProvider provider;
		ISelection selection;
		provider = site.getSelectionProvider();
		if (provider == null) {
			MessageDialog.openInformation(window.getShell(),
					"Getting selected text ...", "No selection provider.");
			return "";
		}
		selection = provider.getSelection();
		if (selection == null) {
			MessageDialog.openInformation(window.getShell(),
					"Getting selected text ...", "No selection is get.");
			return "";
		}
		if (!(selection instanceof ITextSelection)) {
			MessageDialog.openInformation(window.getShell(),
					"Getting selected text ...", "No text selection is get.");
			return "";
		}
		ITextSelection ts = (ITextSelection) selection;
		return ts.getText();
	}

	private int findCodeType(String code, IJavaElement element) {
		if (element.getElementName().equals(code)) {
			return element.getElementType();
		}
		// try to cast the element to IParent to find deeper
		try {
			IParent parent = (IParent) element;
			IJavaElement[] elements = parent.getChildren();
			System.out.println("Number of children: " + elements.length);
			for (IJavaElement e : elements) {
				int ret = findCodeType(code, e);
				if (ret != 0) {
					return ret;
				}
			}
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return 0;
	}

	private IJavaElement findCodeElement(String code, IJavaElement element) {
		if(element.getElementName().equals(code)) {
			return element;
		}
		try {
			IParent parent = (IParent) element;
			IJavaElement[] elements = parent.getChildren();
			for (IJavaElement e : elements) {
				IJavaElement ret = findCodeElement(code, e);
				if (ret != null) {
					return ret;
				}
			}
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action
	 * .IAction, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	@Override
	public void dispose() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.
	 * IWorkbenchWindow)
	 */
	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

}
