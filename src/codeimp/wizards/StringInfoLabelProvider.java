package codeimp.wizards;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

public abstract class StringInfoLabelProvider extends CellLabelProvider {
	@Override
	public void update(ViewerCell cell) {
		Object[] element = (Object[]) cell.getElement();
		cell.setText(doGetValue(element));
	}

	protected abstract String doGetValue(Object[] element);
}