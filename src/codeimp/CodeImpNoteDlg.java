/**
 * 
 */
package codeimp;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * @author chuxuankhoi
 *
 */
public class CodeImpNoteDlg extends Dialog {

	protected CodeImpNoteDlg(IShellProvider parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		// TODO Add items to the dialog
		try 
		{
			composite.setLayout(new FormLayout());
			{
				createLabels(composite);
				createButtons(composite);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Set dialog dimensions
		composite.getShell().setSize(800, 600);
		
		// Set dialog positions
		setDialogLocation();
		
		return composite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButton(org.eclipse.swt.widgets.Composite, int, java.lang.String, boolean)
	 */
	@Override
	protected Button createButton(Composite parent, int id, String label,
			boolean defaultButton) {
		return null;
	}

	private void setDialogLocation()
	{
		Rectangle monitorArea = getShell().getDisplay().getPrimaryMonitor().getBounds();
		Rectangle shellArea = getShell().getBounds();
		int x = monitorArea.x + (monitorArea.width - shellArea.width)/2;
		int y = monitorArea.y + (monitorArea.height - shellArea.height)/2;
		getShell().setLocation(x,y);
	}
	
	private void createLabels( Composite composite )
	{
		Label label = new Label( composite , SWT.None );
		label.setText("Label 1");
		FormData lblData = new FormData();
		lblData.width = 40;
		lblData.height = 40;
		lblData.left =  new FormAttachment(0, 1000, 6);//x co-ordinate
		lblData.top =  new FormAttachment(0, 1000, 17);//y co-ordinate
		label.setLayoutData(lblData);
	}

	
	private void createButtons( Composite composite )
	{
		Button btn = new Button( composite , SWT.PUSH );
		btn.setText("Press to close");
		FormData btnData = new FormData();
		btnData.width = 120;
		btnData.height = 30;
		btnData.left = new FormAttachment(0,1000,0);//x co-ordinate
		btnData.top = new FormAttachment(0,1000,0);//y co-ordinate
		btn.setLayoutData(btnData);
		//Write listener for button
		btn.addSelectionListener( new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent se) 
			{
				close();
			}
		}
		);
	}

}
