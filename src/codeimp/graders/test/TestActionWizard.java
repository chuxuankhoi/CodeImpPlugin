package codeimp.graders.test;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;

public class TestActionWizard extends Wizard {
	
	private WizardPage page = null;

	public TestActionWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		return true;
	}
	
	@Override
	public void addPages() {
		page = new ProgressPage("Test Progress");
		addPage(page);
	}
	
	@Override
	public boolean performCancel() {
		if (page != null && page instanceof ProgressPage) {
			((ProgressPage) page).tryCancel();
		}
		return super.performCancel();
	}

}
