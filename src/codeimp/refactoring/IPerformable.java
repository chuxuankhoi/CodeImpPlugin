package codeimp.refactoring;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.IUndoManager;

public interface IPerformable {
	public boolean perform(IUndoManager undoManager, IProgressMonitor monitor);
}
