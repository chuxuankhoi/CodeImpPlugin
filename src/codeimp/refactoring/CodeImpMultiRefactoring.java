package codeimp.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.IUndoManager;

public class CodeImpMultiRefactoring extends CodeImpRefactoring {

	private RefactoringPair[] pairs = null;
	private int refactoringsDone = 0; // number of the last call to process()
	private int totalRefactoringDone = 0; // accumulated number of refactorings
											// performed by the instance

	@Override
	public boolean process(IUndoManager undoMan, IProgressMonitor monitor)
			throws Exception {
		if (pairs == null) {
			return false;
		}
		refactoringsDone = 0;
		for (RefactoringPair pair : pairs) {
			CodeImpSingleRefactoring single = new CodeImpSingleRefactoring(
					pair, sourceFile);
			if (!single.process(undoMan, monitor)) {
				// recover the system
				for (int i = 0; i < refactoringsDone; i++) {
					undoMan.performUndo(null, null);
				}
				return false;
			}
			refactoringsDone++;
		}
		totalRefactoringDone += refactoringsDone;
		return true;
	}

	@Override
	public void performUndo(IUndoManager undoMan) {
		for(int i = 0; i < totalRefactoringDone; i++) {
			try {
				undoMan.performUndo(null, new NullProgressMonitor());
			} catch (CoreException e) {
			}
		}
	}

}
