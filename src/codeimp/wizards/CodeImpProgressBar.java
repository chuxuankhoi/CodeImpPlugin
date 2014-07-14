package codeimp.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ProgressBar;

public class CodeImpProgressBar {
	
	private ProgressBar bar = null;
	WizardPage usedWizardPage = null;

	public CodeImpProgressBar(Composite parent, int style) {
		bar = new ProgressBar(parent, style);
	}
	
	public void setMaximum(int value) {
		bar.setMaximum(value);
	}
	
	public int getMaximum() {
		return bar.getMaximum();
	}

	public void setSelection(int value) {
		bar.setSelection(value);
		if(value == bar.getMaximum() && usedWizardPage != null) {
			usedWizardPage.setPageComplete(true);
		}
	}
	
	public int getSelection() {
		return bar.getSelection();
	}
	
	public void setParentPage(WizardPage page) {
		usedWizardPage = page;
	}
	
	public boolean isDisposed() {
		return bar.isDisposed();
	}

}
