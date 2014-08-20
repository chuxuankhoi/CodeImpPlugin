package codeimp.settings;

import java.util.HashMap;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.TableLayoutComposite;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import codeimp.refactoring.CodeImpRefactoringManager;
import codeimp.wizards.StringContentProvider;
import codeimp.wizards.StringInfoLabelProvider;

@SuppressWarnings("restriction")
public class CustomizingView extends Dialog {

	private static final int BUTTON_WIDTH = 140;
	private static final int BUTTON_LAYOUT_WIDTH = 150;
	private static final int BUTTON_LAYOUT_TOP_MARGIN = 20;

	private class InfoEditingSupport extends EditingSupport {
		private CellEditor fTextEditor;

		private InfoEditingSupport(CellEditor cellEditor, ColumnViewer viewer) {
			super(viewer);
			fTextEditor = cellEditor;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return fTextEditor;
		}

		@Override
		protected Object getValue(Object element) {
			Object[] elem = (Object[]) element;
			if (elem != null && elem.length > 1) {
				return elem[1].toString();
			} else {
				return null;
			}
		}

		@Override
		protected void setValue(Object element, Object value) {
			Object[] elem = (Object[]) element;
			if (elem != null && elem.length > 1) {
				elem[1] = value;
				getViewer().update(element, null);
			}
		}

		@Override
		protected boolean canEdit(Object element) {
			Object[] elem = (Object[]) element;
			if (elem != null && elem.length > 1 && fTextEditor != null) {
				return true;
			}
			return false;
		}
	}

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
	private CheckboxTableViewer metricTable;
	private List refactoringList;
	private Button buttonMetricCancel;
	private Button buttonMetricSave;
	private Button buttonRefactoringDefault;
	private Button buttonRefactoringCancel;
	private Button buttonRefactoringNew;
	private Button buttonRefactoringRemove;
	private Button buttonRefactoringEdit;
	private Button buttonRefactoringSave;

	protected CustomizingView(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Configuration.initialize();
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

		createMetricTable(metricsContainer, "All supported metrics");
		setMetricTableData();

		createMetricButtons(metricsContainer);
	}

	private void createMetricTable(Composite metricsContainer, String string) {
		Composite result = new Composite(metricsContainer, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.verticalAlignment = SWT.FILL;
		gridData.heightHint = 260;
		gridData.widthHint = 392;
		result.setLayout(layout);
		result.setLayoutData(gridData);

		Label l = new Label(result, SWT.NONE);
		l.setText(string);

		TableLayoutComposite layoutComposite = new TableLayoutComposite(result,
				SWT.NONE);
		layout = new GridLayout(1, true);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layoutComposite.setLayout(layout);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		layoutComposite.setData(data);
		layoutComposite.addColumnData(new ColumnWeightData(70,
				convertWidthInCharsToPixels(10), true));
		layoutComposite.addColumnData(new ColumnWeightData(30,
				convertWidthInCharsToPixels(10), true));
		metricTable = CheckboxTableViewer.newCheckList(layoutComposite,
				SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.FULL_SELECTION
						| SWT.MULTI);
		metricTable.setContentProvider(new StringContentProvider());
		createColumns(metricTable);

		Table table = metricTable.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		gridData = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(gridData);
		metricTable.refresh(true);
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

		metricTable.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				buttonMetricCancel.setEnabled(true);
				buttonMetricSave.setEnabled(true);
			}

		});
	}

	private void createColumns(CheckboxTableViewer checkboxTableViewer) {
		// For first column
		TableViewerColumn viewerColumn = new TableViewerColumn(
				checkboxTableViewer, SWT.LEAD);
		viewerColumn.setLabelProvider(new StringInfoLabelProvider() {
			@Override
			protected String doGetValue(Object[] pi) {
				return (String) pi[0];
			}
		});

		TableColumn column = viewerColumn.getColumn();
		column.setText("Metric");

		// For column 2
		viewerColumn = new TableViewerColumn(checkboxTableViewer, SWT.LEAD);
		viewerColumn.setLabelProvider(new StringInfoLabelProvider() {
			@Override
			protected String doGetValue(Object[] element) {
				return element[1].toString();
			}
		});

		CellEditor editor = new TextCellEditor(checkboxTableViewer.getTable());
		editor.addListener(new ICellEditorListener() {

			@Override
			public void editorValueChanged(boolean oldValidState,
					boolean newValidState) {
				buttonMetricCancel.setEnabled(true);
				buttonMetricSave.setEnabled(true);
			}

			@Override
			public void cancelEditor() {
			}

			@Override
			public void applyEditorValue() {
			}
		});
		viewerColumn.setEditingSupport(new InfoEditingSupport(editor,
				checkboxTableViewer));
		column = viewerColumn.getColumn();
		column.setText("Weight");

	}

	public void setMetricTableData() {
		HashMap<String, Double> wMap = new HashMap<>();
		HashMap<String, Boolean> uMap = new HashMap<>();

		String[] metrics = Configuration.getIDsOfTagName("metrics", "",
				"metric");
		for (String metric : metrics) {
			wMap.put(metric, Double.parseDouble(Configuration.getInfoInElement(
					"metric", metric, "weight")));
			uMap.put(metric, Boolean.parseBoolean(Configuration
					.getInfoInElement("metric", metric, "used")));
		}

		metricTable.setInput(wMap);
		for (int i = 0; i < wMap.size(); i++) {
			Object[] element = (Object[]) metricTable.getElementAt(i);
			String elementName = (String) element[0];
			boolean checked = uMap.get(elementName);
			metricTable.setChecked(element, checked);
		}
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

		buttonMetricCancel = new Button(buttonsContainer, SWT.PUSH);
		buttonMetricCancel.setText("Cancel");

		buttonMetricCancel.setLayoutData(btnLayoutData);
		buttonMetricCancel.addSelectionListener(new ButtonListener() {

			@Override
			protected void performButtonPushed() {
				setMetricTableData();
				buttonMetricSave.setEnabled(false);
				buttonMetricCancel.setEnabled(false);
			}
		});
		buttonMetricCancel.setEnabled(false);

		buttonMetricSave = new Button(buttonsContainer, SWT.PUSH);
		buttonMetricSave.setText("Save");
		buttonMetricSave.setLayoutData(btnLayoutData);
		buttonMetricSave.addSelectionListener(new ButtonListener() {

			@Override
			protected void performButtonPushed() {
				saveMetricsChanges();
				buttonMetricSave.setEnabled(false);
				buttonMetricCancel.setEnabled(false);
			}
		});
		buttonMetricSave.setEnabled(false);
	}

	@SuppressWarnings("unchecked")
	private void saveMetricsChanges() {
		int elementsNum = ((HashMap<String, Double>) metricTable.getInput())
				.size();
		for (int i = 0; i < elementsNum; i++) {
			String elemName = "";
			String elemWeight = "0";
			boolean elemUsed = false;
			Object[] element = (Object[]) metricTable.getElementAt(i);
			if (element != null && element.length > 1) {
				elemName = element[0].toString();
				elemWeight = element[1].toString();
				elemUsed = metricTable.getChecked(metricTable.getElementAt(i));
				Configuration.updateElementInfo("metric", elemName, "weight",
						elemWeight, "used", new Boolean(elemUsed).toString());
			}
		}
		Configuration.saveChanged();
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

		refactoringList = createList(refactoringContainer,
				"All supported refactorings:");
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

		List createdList = new List(listContainer, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.BORDER);
		GridData listLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		createdList.setLayoutData(listLayoutData);

		return createdList;
	}

	private void setRefactoringListData() {
		String[] actions = Configuration.getIDsOfTagName("refactorings", "",
				"refactoring");
		if (actions == null) {
			return;
		}
		refactoringList.removeAll();
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

		buttonRefactoringDefault = new Button(buttonsContainer, SWT.PUSH);
		buttonRefactoringDefault.setText("Load Default");
		buttonRefactoringDefault.setLayoutData(btnLayoutData);
		buttonRefactoringDefault.addSelectionListener(new ButtonListener() {

			@Override
			protected void performButtonPushed() {
				String[] actions = CodeImpRefactoringManager.getManager()
						.getActionsList();
				if (actions == null) {
					return;
				}
				refactoringList.removeAll();
				for (String action : actions) {
					refactoringList.add(action);
				}
			}
		});
		buttonRefactoringDefault.setEnabled(false);

		buttonRefactoringCancel = new Button(buttonsContainer, SWT.PUSH);
		buttonRefactoringCancel.setText("Reload");
		buttonRefactoringCancel.setLayoutData(btnLayoutData);
		buttonRefactoringCancel.addSelectionListener(new ButtonListener() {

			@Override
			protected void performButtonPushed() {
				setRefactoringListData();
			}
		});
		buttonRefactoringCancel.setEnabled(false);

		buttonRefactoringNew = new Button(buttonsContainer, SWT.PUSH);
		buttonRefactoringNew.setText("New Refactoring");
		buttonRefactoringNew.setLayoutData(btnLayoutData);
		buttonRefactoringNew.addSelectionListener(new ButtonListener() {

			@Override
			protected void performButtonPushed() {
				// TODO Perform dialog to add new refactoring

			}
		});

		buttonRefactoringRemove = new Button(buttonsContainer, SWT.PUSH);
		buttonRefactoringRemove.setText("Remove Refactoring");
		buttonRefactoringRemove.setLayoutData(btnLayoutData);
		buttonRefactoringRemove.addSelectionListener(new ButtonListener() {

			@Override
			protected void performButtonPushed() {
				// TODO Delete the selected custom refactoring

			}
		});
		buttonRefactoringRemove.setEnabled(false);

		buttonRefactoringEdit = new Button(buttonsContainer, SWT.PUSH);
		buttonRefactoringEdit.setText("Edit Refactoring");
		buttonRefactoringEdit.setLayoutData(btnLayoutData);
		buttonRefactoringEdit.addSelectionListener(new ButtonListener() {

			@Override
			protected void performButtonPushed() {
				// TODO Perform dialog to edit the custom refactoring

			}
		});
		buttonRefactoringEdit.setEnabled(false);

		buttonRefactoringSave = new Button(buttonsContainer, SWT.PUSH);
		buttonRefactoringSave.setText("Save");
		buttonRefactoringSave.setLayoutData(btnLayoutData);
		buttonRefactoringSave.addSelectionListener(new ButtonListener() {

			@Override
			protected void performButtonPushed() {
				saveRefactoringsChanges();
			}
		});
		buttonRefactoringSave.setEnabled(false);
	}

	private void saveRefactoringsChanges() {
		// TODO update state of all refactoring items to DOM
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

	@Override
	protected void okPressed() {
		saveMetricsChanges();
		saveRefactoringsChanges();
		super.okPressed();
	}

}
