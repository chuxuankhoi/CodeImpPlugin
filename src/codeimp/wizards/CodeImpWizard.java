package codeimp.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbenchWindow;

public class CodeImpWizard extends Wizard {

	private AnalysisPage aPage = null;
	private RefactoringSelectionPage sPage = null;
	private ImprovePage iPage = null;
	private ITextSelection selection;
	private IFile file;
	private IWorkbenchWindow mainWindow;

	@Override
	public boolean performFinish() {
		return true;
	}

	public CodeImpWizard(ITextSelection textSelection, IFile curEditorFile,
			IWorkbenchWindow window) {
		super();
		setNeedsProgressMonitor(true);
		selection = textSelection;
		file = curEditorFile;
		mainWindow = window;
	}

	@Override
	public void addPages() {
		aPage = new AnalysisPage("Analysis", selection, file, mainWindow);
		sPage = new RefactoringSelectionPage("Selection");
		iPage = new ImprovePage("Improve");
		addPage(aPage);
		addPage(sPage);
		addPage(iPage);
	}

	@Override
	public boolean performCancel() {
		if(aPage != null) {
			aPage.tryCancel();
		}
		return super.performCancel();
	}

}
