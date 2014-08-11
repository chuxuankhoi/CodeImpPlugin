package codeimp.settings;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import codeimp.refactoring.CodeImpRefactoringManager;

public class CustomizingView extends Dialog {

	private static final int BUTTON_WIDTH = 140;
	private static final int BUTTON_LAYOUT_WIDTH = 150;
	private static final int BUTTON_LAYOUT_TOP_MARGIN = 20;

	private abstract class ButtonListener implements SelectionListener {

		@Override
		public void widgetSelected(SelectionEvent e) {
			performButtonPushed();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			performButtonPushed();
		}

		protected abstract void performButtonPushed();

	}

	private static final int SETTING_DIALOG_HEIGHT = 700;
	private static final int SETTING_DIALOG_WIDTH = 600;
	private static final int LIST_BKGR_STYLE = SWT.BORDER;
	private List metricList;
	private List refactoringList;

	protected CustomizingView(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		Composite bkgrContainer = new Composite(container, SWT.NONE);
		bkgrContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout(1, false);
		bkgrContainer
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		bkgrContainer.setLayout(layout);

		createRefactoringList(bkgrContainer);
		createMetricList(bkgrContainer);
		return container;
	}

	private void createMetricList(Composite bkgrContainer) {
		Composite metricsContainer = new Composite(bkgrContainer,
				LIST_BKGR_STYLE);
		metricsContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout(2, false);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		metricsContainer.setLayoutData(data);
		metricsContainer.setLayout(layout);

		metricList = createList(metricsContainer, "All supported metrics");
		setMetricListData();

		createMetricButtons(metricsContainer);
	}

	private void setMetricListData() {
		// TODO Get metrics list and push to metricList
		metricList.add("LCOM2");
		metricList.add("LCOM5");
		metricList.add("TCC");
		metricList.add("InheritedRatio");
		metricList.add("SharedMethodsInChildren");
		metricList.add("SharedMethods");
		metricList.add("EmptyClass");
	}

	private void createMetricButtons(Composite container) {
		Composite buttonsContainer = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginTop = BUTTON_LAYOUT_TOP_MARGIN;
		GridData data = new GridData();
		data.widthHint = BUTTON_LAYOUT_WIDTH;
		data.verticalAlignment = SWT.FILL;
		buttonsContainer.setLayout(layout);
		buttonsContainer.setLayoutData(data);

		GridData btnLayoutData = new GridData();
		btnLayoutData.widthHint = BUTTON_WIDTH;

		final Button buttonNew = new Button(buttonsContainer, SWT.PUSH);
		buttonNew.setText("New Metric");

		buttonNew.setLayoutData(btnLayoutData);
		buttonNew.addSelectionListener(new ButtonListener() {

			@Override
			protected void performButtonPushed() {
				// TODO Perform dialog to create new metric

			}
		});

		final Button buttonRemove = new Button(buttonsContainer, SWT.PUSH);
		buttonRemove.setText("Remove Metric");
		buttonRemove.setLayoutData(btnLayoutData);
		buttonRemove.addSelectionListener(new ButtonListener() {

			@Override
			protected void performButtonPushed() {
				// TODO Remove metric from list

			}
		});

		final Button buttonEdit = new Button(buttonsContainer, SWT.PUSH);
		buttonEdit.setText("Edit Metric");
		buttonEdit.setLayoutData(btnLayoutData);
		buttonEdit.addSelectionListener(new ButtonListener() {

			@Override
			protected void performButtonPushed() {
				// TODO Perform dialog to edit the selected metric

			}
		});
	}

	private void createRefactoringList(Composite bkgrContainer) {
		Composite refactoringContainer = new Composite(bkgrContainer,
				LIST_BKGR_STYLE);
		refactoringContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout(2, false);
		GridData data = new GridData();
		data.horizontalAlignment = SWT.FILL;
		data.heightHint = 300;
		refactoringContainer.setLayoutData(data);
		refactoringContainer.setLayout(layout);
		
		refactoringList = createList(refactoringContainer, "All supported refactorings:");
		setRefactoringListData();
		
		createRefactoringButtons(refactoringContainer);
	}

	private List createList(Composite container, String labelStr) {
		Composite listContainer = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout(1, true);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		listContainer.setLayout(layout);
		listContainer.setLayoutData(data);
		
		Label label = new Label(listContainer, SWT.NONE);
		label.setText(labelStr);

		List createdList = new List(listContainer, SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER);
		GridData listLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		createdList.setLayoutData(listLayoutData);
		
		return createdList;
	}

	private void setRefactoringListData() {
		CodeImpRefactoringManager manager = CodeImpRefactoringManager
				.getManager();
		String[] actions = manager.getActionsList();
		for (String action : actions) {
			refactoringList.add(action);
		}
	}

	private void createRefactoringButtons(Composite container) {
		Composite buttonsContainer = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginTop = BUTTON_LAYOUT_TOP_MARGIN;
		GridData data = new GridData();
		data.widthHint = BUTTON_LAYOUT_WIDTH;
		data.verticalAlignment = SWT.FILL;
		buttonsContainer.setLayout(layout);
		buttonsContainer.setLayoutData(data);

		GridData btnLayoutData = new GridData();
		btnLayoutData.widthHint = BUTTON_WIDTH;

		final Button buttonNew = new Button(buttonsContainer, SWT.PUSH);
		buttonNew.setText("New Refactoring");

		buttonNew.setLayoutData(btnLayoutData);
		buttonNew.addSelectionListener(new ButtonListener() {

			@Override
			protected void performButtonPushed() {
				// TODO Perform dialog to add new refactoring

			}
		});

		final Button buttonRemove = new Button(buttonsContainer, SWT.PUSH);
		buttonRemove.setText("Remove Refactoring");
		buttonRemove.setLayoutData(btnLayoutData);
		buttonRemove.addSelectionListener(new ButtonListener() {

			@Override
			protected void performButtonPushed() {
				// TODO Delete the selected custom refactoring

			}
		});

		final Button buttonEdit = new Button(buttonsContainer, SWT.PUSH);
		buttonEdit.setText("Edit Refactoring");
		buttonEdit.setLayoutData(btnLayoutData);
		buttonEdit.addSelectionListener(new ButtonListener() {

			@Override
			protected void performButtonPushed() {
				// TODO Perform dialog to edit the custom refactoring

			}
		});
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Settings");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(SETTING_DIALOG_WIDTH, SETTING_DIALOG_HEIGHT);
	}

}
