package codeimp.settings;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class CustomizeActionDelegation implements IWorkbenchWindowActionDelegate {
	
	private IWorkbenchWindow window = null;
	private Dialog view = null;

	@Override
	public void run(IAction action) {
		view = new CustomizingView(window.getShell());
		view.open();
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
