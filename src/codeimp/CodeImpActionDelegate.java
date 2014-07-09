/**
 * 
 */
package codeimp;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
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
	IFile curEditorFile = null;
	ITextSelection textSelection = null;
	IJavaElement[] extractedResults = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(IAction action) {
		MessageDialog.openInformation(window.getShell(),
				"Have not completed yet",
				"CodeImpHillClimbing is under construction");

		// Check Java perspective and editor
		if (!isPerspective("Java")) {
			MessageDialog.openInformation(window.getShell(),
					"Checking current perspective ...",
					"Current version have not support the language.");
			return;
		}

		// Check opening file to make sure that it is a java file
		getCurrentEditorFile();
		if (curEditorFile == null) {
			MessageDialog.openInformation(window.getShell(),
					"Checking file extension ...", "No editor is opened.");
			return;
		}
		String ext = curEditorFile.getFileExtension();
		if (ext == null || !ext.equals("java")) {
			MessageDialog.openInformation(window.getShell(),
					"Checking file extension ...",
					"Current version have not support the file extension.");
			return;
		}

		// Get the selected code in the current editor
		getSelectedCode();
		if (textSelection == null || textSelection.getText().equals("")) {
			MessageDialog.openInformation(window.getShell(),
					"Getting selected text ...", "No text is selected.");
			return;
		}

		// Run the improvement
		final CodeImpAbstract improvementJob = new CodeImpHillClimbing(
				textSelection, curEditorFile, window);
		Job job = new Job("Code improvement") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				improvementJob.runImprovement();
				// Display the analysis and confirm the changed blocks
				System.out.println("Refactoring used: ");
				System.out.println(improvementJob.getRefactoringHistory());
				System.out.println("Code improvement completed.");
				return Status.OK_STATUS;
			}
		};
		job.schedule();

	}

	private boolean isPerspective(String expectedPerspective) {
		String curPerspective = window.getActivePage().getPerspective()
				.getLabel();
		if (curPerspective == null) {
			return false;
		}
		if (curPerspective.equals(expectedPerspective)) {
			return true;
		}
		return false;
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
