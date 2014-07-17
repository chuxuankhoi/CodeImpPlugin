package codeimp.wizards;

import java.util.HashMap;

import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.TableLayoutComposite;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
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
import codeimp.CodeImpAbstract;

@SuppressWarnings("restriction")
public class FinalPage extends WizardPage {

	private Composite container;
	private CodeImpAbstract improvementJob;

	public FinalPage(String pageName, CodeImpAbstract improvement) {
		super(pageName);
		setTitle(pageName);
		setDescription("List of being-refactored items to confirm. In order to confirm, press Finish button, to skip, press Cancel button");
		improvementJob = improvement;
	}

	private TableViewer tableViewer;

	@Override
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		createTable();
		container.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent arg0) {
				String action = ((RefactoringSelectionPage) getPreviousPage())
						.getSelectedAction();
				HashMap<String, Double> map = improvementJob
						.getEffectiveList(action);
				if (map.size() == 0) {
					MessageDialog.openInformation(getShell(), "Message",
							"There is no improvement for the selected item.");
					getShell().close();
				}
				tableViewer.setInput(map);
			}
		});

		setControl(container);

		setPageComplete(false);
	}

	/**
	 * 
	 */
	private void createTable() {
		Composite result = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		result.setLayout(layout);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 2;
		result.setLayoutData(gridData);

		Label l = new Label(result, SWT.NONE);
		l.setText("Refactored Items List");
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
		tableViewer = new TableViewer(new Table(layoutComposite, SWT.V_SCROLL
				| SWT.H_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI));
		tableViewer.setContentProvider(new StringContentProvider());
		createColumns(tableViewer);

		Table table = tableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		gridData = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(gridData);
		tableViewer.refresh(true);
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
	}

	private void createColumns(TableViewer tableViewer) {
		// For first column
		TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer,
				SWT.LEAD);
		viewerColumn.setLabelProvider(new StringInfoLabelProvider() {
			@Override
			protected String doGetValue(Object[] pi) {
				return (String) pi[0];
			}
		});

		TableColumn column = viewerColumn.getColumn();
		column.setText("Items");

		// For column 2
		viewerColumn = new TableViewerColumn(tableViewer, SWT.LEAD);
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
