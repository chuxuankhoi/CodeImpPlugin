package codeimp.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.IWorkbenchWindow;

import codeimp.CodeImpAbstract;
import codeimp.CodeImpHillClimbing;

public class AnalysisPage extends WizardPage {

	private Composite container = null;

	private Composite progressBarComposite = null;
	private ProgressBar progressBar = null;
	protected int processBarStyle = SWT.SMOOTH; // process bar style

	private ITextSelection textSelection;
	private IFile curEditorFile;
	private IWorkbenchWindow window;

	protected CodeImpAbstract improvementJob = null;

	private Display display;

	protected AnalysisPage(String pageName, ITextSelection selection,
			IFile file, IWorkbenchWindow win) {
		super(pageName);
		setTitle(pageName);
		setDescription("Analyse current source code to get the effectiveness of refactoring actions");
		textSelection = selection;
		curEditorFile = file;
		window = win;
	}

	@Override
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(GridData.FILL, GridData.CENTER,
				true, false));
		GridLayout gLayout = new GridLayout();
		container.setLayout(gLayout);
		display = container.getDisplay();

		progressBarComposite = new Composite(container, SWT.NONE);
		progressBarComposite.setLayoutData(new GridData(GridData.FILL,
				GridData.CENTER, true, false));
		progressBarComposite.setLayout(new FillLayout());

		progressBar = new ProgressBar(progressBarComposite, processBarStyle);

		setControl(container);
		
		improvementJob = new CodeImpHillClimbing(textSelection, curEditorFile, window);
		improvementJob.runImprovement(progressBar, display);

		setPageComplete(true);
	}

	public void tryCancel() {
		Thread.currentThread().interrupt();
		if(improvementJob != null) {
			improvementJob.cancel();
		}
	}

}
