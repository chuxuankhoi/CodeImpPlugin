package codeimp.wizards;

import java.util.Map;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import codeimp.CodeImpAbstract;
import codeimp.CodeImpUtils;

public class AnalysisPage extends WizardPage {

	private Composite container = null;

	private Composite progressBarComposite = null;
	private CodeImpProgressBar progressBar = null;
	protected int processBarStyle = SWT.SMOOTH; // process bar style
	protected CodeImpAbstract improvementJob = null;
	private Display display;
	private long startTime = 0;

	protected AnalysisPage(String pageName, CodeImpAbstract improvement) {
		super(pageName);
		setTitle(pageName);
		setDescription("Analyse current source code to get the effectiveness of refactoring actions");
		improvementJob = improvement;
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

		// progressBar = new ProgressBar(progressBarComposite, processBarStyle);
		progressBar = new CodeImpProgressBar(progressBarComposite,
				processBarStyle, display);
		progressBar.setParentPage(this);

		setControl(container);

		startTime = System.currentTimeMillis();
		improvementJob.runImprovement(progressBar);

		setPageComplete(false);
	}

	public void tryCancel() {
		if (improvementJob != null) {
			improvementJob.cancel();
		}
	}

	@Override
	public boolean canFlipToNextPage() {
		if (progressBar.getSelection() < progressBar.getMaximum()) {
			return false;
		} else {
			return super.canFlipToNextPage();
		}
	}

	public Map<String, Double> getResults() {
		if (improvementJob == null) {
			return null;
		}
		return improvementJob.getResults();
	}

	public void notifyCompleted() {
		long curTime = System.currentTimeMillis();
		CodeImpUtils.printLog("Elapsed time: " + ((curTime - startTime) / 60000.0));
		setPageComplete(true);
		getWizard().getPages();
		getContainer().showPage(getNextPage());
	}

}
