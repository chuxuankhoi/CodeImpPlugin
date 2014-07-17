package codeimp.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;

public class CodeImpProgressBar {
	
	private ProgressBar bar = null;
	private WizardPage parent = null;
	private Display display;

	public CodeImpProgressBar(Composite parent, int style, Display disp) {
		bar = new ProgressBar(parent, style);
		display = disp;
	}
	
	public void setMaximum(final int value) {
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				bar.setMaximum(value);
			}
		});
	}
	
	public int getMaximum() {
		return bar.getMaximum();
	}

	public void setSelection(final int value) {
		if (!display.isDisposed()) {
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					if (!bar.isDisposed()) {
						bar.setSelection(value);
						if(value == bar.getMaximum() && parent != null) {
							if(parent instanceof AnalysisPage) {
								((AnalysisPage) parent).notifyCompleted();
							}
						}
					}
				}
			});
		}
		
	}
	
	public int getSelection() {
		return bar.getSelection();
	}
	
	public void setParentPage(WizardPage page) {
		parent = page;
	}
	
	public boolean isDisposed() {
		return bar.isDisposed();
	}

}
