package codeimp.test;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPartSite;
//import org.eclipse.jface.wizard.Wizard;
//import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import codeimp.CodeImpUtils;
import codeimp.refactoring.AtomicRefactoring;
import codeimp.refactoring.FieldUsedMethodFilter;
import codeimp.refactoring.IPerformable;
import codeimp.refactoring.Scheduler;
import codeimp.settings.Configuration;

public class TestActionDelegation implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window = null;
	// private Wizard wizard = null;
	private IFile curEditorFile;
	private ITextSelection textSelection;

	@Override
	public void run(IAction action) {
		/*
		 * wizard = new TestActionWizard(); WizardDialog wDlg = new
		 * WizardDialog(window.getShell(), wizard); wDlg.open();
		 */

		getCurrentEditorFile();
		getSelectedCode();
		IJavaElement[] rootElements = null;
		try {
			rootElements = CodeImpUtils.identifyElements(
					textSelection.getText(), curEditorFile);
		} catch (JavaModelException e1) {
			CodeImpUtils.printLog(e1.toString());
		}
		if (rootElements == null) {
			CodeImpUtils.printLog("Unexpected returned.");
			return;
		}
		if (rootElements.length == 0) {
			CodeImpUtils.printLog("No item identified.");
			return;
		}

		Scheduler sched = new Scheduler();
		IPerformable performable1 = Configuration.getRefactoring(
				rootElements[0], IJavaRefactorings.EXTRACT_CLASS,
				rootElements[0].getResource());
		sched.pushRefactoring(performable1, 0);
		FieldUsedMethodFilter filter = new FieldUsedMethodFilter(
				rootElements[0]);
		Object[] methods = filter.getOutput();
		if (methods != null) {
			for (int i = 0; i < methods.length; i++) {
				IPerformable performable = Configuration.getRefactoring(
						methods[0], IJavaRefactorings.MOVE_METHOD,
						((IJavaElement) methods[0]).getResource());
				sched.pushRefactoring(performable, 1);
			}
		}
		IPerformable performable4 = null;
		IPerformable performable5 = null;
		sched.pushRefactoring(performable4, 2);
		sched.pushRefactoring(performable5, 2);
		sched.perform(null, null);
	}

	private void getCurrentEditorFile() {
		IFile curFile = null;
		IEditorPart editorPart = window.getActivePage().getActiveEditor();
		if (editorPart != null) {
			IFileEditorInput input = (IFileEditorInput) editorPart
					.getEditorInput();
			curFile = input.getFile();
		}
		curEditorFile = curFile;
	}

	private void getSelectedCode() {
		IWorkbenchPartSite site = window.getPartService().getActivePart()
				.getSite();
		if (site == null) {
			MessageDialog.openInformation(window.getShell(),
					"Getting selected text ...", "No workbench part site.");
			return;
		}
		ISelectionProvider provider;
		ISelection selection;
		provider = site.getSelectionProvider();
		if (provider == null) {
			MessageDialog.openInformation(window.getShell(),
					"Getting selected text ...", "No selection provider.");
			return;
		}
		selection = provider.getSelection();
		if (selection == null) {
			MessageDialog.openInformation(window.getShell(),
					"Getting selected text ...", "No selection is get.");
			return;
		}
		if (!(selection instanceof ITextSelection)) {
			MessageDialog.openInformation(window.getShell(),
					"Getting selected text ...", "No text selection is get.");
			return;
		}
		textSelection = (ITextSelection) selection;
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {

	}

	@Override
	public void dispose() {

	}

	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

}
