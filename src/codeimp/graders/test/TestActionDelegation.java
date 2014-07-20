package codeimp.graders.test;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class TestActionDelegation implements IWorkbenchWindowActionDelegate {
	
	private IWorkbenchWindow window = null;
	private Wizard wizard = null;

	@Override
	public void run(IAction action) {		
		wizard = new TestActionWizard();
		WizardDialog wDlg = new WizardDialog(window.getShell(), wizard);
		wDlg.open();
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
