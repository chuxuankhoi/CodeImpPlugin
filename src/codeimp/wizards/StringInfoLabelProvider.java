package codeimp.wizards;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

abstract class StringInfoLabelProvider extends CellLabelProvider {
	@Override
	public void update(ViewerCell cell) {
		Object[] pi = (Object[]) cell.getElement();
		cell.setText(doGetValue(pi));
	}

	protected abstract String doGetValue(Object[] pi);
}