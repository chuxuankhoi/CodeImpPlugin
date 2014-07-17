package codeimp.wizards;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.TableLayoutComposite;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import codeimp.CodeImpUtils;

@SuppressWarnings("restriction")
public class RefactoringSelectionPage extends WizardPage {

	private static final int MAXIMUM_ACTIONS = 5;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#getPreviousPage()
	 */
	@Override
	public IWizardPage getPreviousPage() {
		return null;
	}

	private boolean isFirstTime = true;
	private CheckboxTableViewer checkboxTableViewer = null;
	private String selectedAction = null;

	private Composite container;

	protected RefactoringSelectionPage(String pageName) {
		super(pageName);
		setTitle(pageName);
		setDescription("Select the action to improve");
	}

	public String getSelectedAction() {
		return selectedAction;
	}

	@Override
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		createTable(container);

		setControl(container);
		container.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent arg0) {
				if (isFirstTime) {
					isFirstTime = false;
					AnalysisPage previousPage = (AnalysisPage) getSuperPreviousPage();
					Map<String, Double> results = previousPage.getResults();
					if (results == null || results.size() == 0) {
						MessageDialog
								.openInformation(getShell(), "Message",
										"There is no improvement for the selected item.");
						getShell().close();
					}
					// Get top MAXIMUM_ACTIONS actions to push to the table
					results = CodeImpUtils.sortByComparator(results,
							CodeImpUtils.DESC);
					String[] sortedActions = new String[results.keySet().size()];
					sortedActions = results.keySet().toArray(sortedActions);
					Map<String, Double> topActions = new HashMap<String, Double>();
					int maxAction = results.size() < MAXIMUM_ACTIONS ? results
							.size() : MAXIMUM_ACTIONS;
					for (int i = 0; i < maxAction; i++) {
						double value = results.get(sortedActions[i]);
						if (value > 0) {
							topActions.put(sortedActions[i], value);
						}
					}
					if (topActions.size() == 0) {
						CodeImpUtils.printLog("No improvement found.");
						getWizard().dispose();
					}
					checkboxTableViewer.setInput(topActions);
				}
			}

		});
		setPageComplete(false);
	}

	private IWizardPage getSuperPreviousPage() {
		return super.getPreviousPage();
	}

	private void createTable(Composite container) {
		Composite result = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		result.setLayout(layout);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 2;
		result.setLayoutData(gridData);

		Label l = new Label(result, SWT.NONE);
		l.setText("Recommended Refactoring Actions");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		gridData.verticalIndent = 5;
		l.setLayoutData(gridData);

		TableLayoutComposite layoutComposite = new TableLayoutComposite(result,
				SWT.NONE);
		layoutComposite.addColumnData(new ColumnWeightData(40,
				convertWidthInCharsToPixels(20), true));
		layoutComposite.addColumnData(new ColumnWeightData(60,
				convertWidthInCharsToPixels(20), true));
		checkboxTableViewer = CheckboxTableViewer.newCheckList(layoutComposite,
				SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.FULL_SELECTION
						| SWT.MULTI);
		checkboxTableViewer.setContentProvider(new StringContentProvider());
		createColumns(checkboxTableViewer);

		Table table = checkboxTableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		gridData = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(gridData);
		checkboxTableViewer.refresh(true);
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = SWTUtil.getTableHeightHint(table, 5);
		gridData.widthHint = convertWidthInCharsToPixels(50);
		layoutComposite.setLayoutData(gridData);
		Composite controls = new Composite(result, SWT.NONE);
		gridData = new GridData(GridData.FILL, GridData.FILL, false, false);
		controls.setLayoutData(gridData);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		controls.setLayout(gridLayout);

		checkboxTableViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getChecked() == true) {
					selectedAction = (String) ((Object[]) event.getElement())[0];
					checkboxTableViewer.setAllChecked(false);
					checkboxTableViewer.setChecked(event.getElement(), true);
					setPageComplete(true);
				} else {
					selectedAction = null;
					setPageComplete(false);
				}
			}

		});
	}

	protected int getIndex(Object element) {
		for (int i = 0; i < checkboxTableViewer.getTable().getItemCount(); i++) {
			if (checkboxTableViewer.getElementAt(i) == element) {
				return i;
			}
		}
		return -1;
	}

	private void createColumns(CheckboxTableViewer tv) {
		// For first column
		TableViewerColumn viewerColumn = new TableViewerColumn(tv, SWT.LEAD);
		viewerColumn.setLabelProvider(new StringInfoLabelProvider() {
			@Override
			protected String doGetValue(Object[] pi) {
				return (String) pi[0];
			}
		});

		TableColumn column = viewerColumn.getColumn();
		column.setText("Actions");

		// For column 2
		viewerColumn = new TableViewerColumn(tv, SWT.LEAD);
		viewerColumn.setLabelProvider(new StringInfoLabelProvider() {
			@Override
			protected String doGetValue(Object[] pi) {
				return pi[1].toString();
			}
		});
		column = viewerColumn.getColumn();
		column.setText("Improvement");
	}

}
