/**
 * 
 */
package codeimp;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * @author chuxuankhoi
 * 
 */
public class CodeImpActionDelegate implements IWorkbenchWindowActionDelegate {
	IWorkbenchWindow window;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(IAction action) {
		MessageDialog.openInformation(window.getShell(),
				"Have not completed yet", "CodeImp is under construction");
		
		// TODO Check Java perspective and editor
		
		// TODO Get the selection code
		
		// TODO Analyse code smell
		
		// TODO Find appropriate improvement for each issue
		
		// TODO Display the analysis and suggestion
		
		// TODO Change the current code as suggested if user accept the solution
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
