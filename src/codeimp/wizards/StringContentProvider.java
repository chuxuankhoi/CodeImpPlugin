package codeimp.wizards;

import java.util.Map;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class StringContentProvider implements IStructuredContentProvider {

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	/**
	 * inputElement is input of the table and in Map<String, Double> format
	 */
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof Map) {
			int outputSize = ((Map<?, ?>) inputElement).size();
			String[] actions = new String[outputSize];
			actions = (String[]) ((Map<?, ?>) inputElement).keySet()
					.toArray(actions);
			Object[] output = new Object[outputSize];
			for (int i = 0; i < outputSize; i++) {
				output[i] = new Object[] { actions[i],
						((Map<?, ?>) inputElement).get(actions[i]) };
			}
			return output;
		}
		return null;
	}

}