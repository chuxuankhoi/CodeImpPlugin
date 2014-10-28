package codeimp.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import codeimp.CodeImpUtils;

public abstract class CodeImpRefactoring {

	protected IFile sourceFile;

	/**
	 * Perform the refactoring following the specific circumstance
	 * 
	 * @param undoMan
	 *            the manager storing all refactoring actions
	 * @param monitor
	 *            to monitor the progress of refactoring
	 * @return
	 * @throws Exception
	 */
	public abstract boolean process(IUndoManager undoMan,
			IProgressMonitor monitor) throws Exception;

	/**
	 * This perform undo based on the number of refactoring action the instance
	 * performed. Thus, it should be called right after calling process()
	 * function
	 * 
	 * @param undoMan
	 *            the manager storing all refactoring actions
	 */
	public abstract void performUndo(IUndoManager undoMan);

	protected void printLog(String log) {
		CodeImpUtils.printLog(this.getClass().getName() + " - " + log);
	}

	protected boolean refactorElement(RefactoringPair pair,
			IUndoManager undoMan, IProgressMonitor monitor) {
		if (pair == null) {
			throw new OperationCanceledException();
		}
		if (pair.element == null || pair.action == null) {
			throw new OperationCanceledException();
		}

		monitor.beginTask("Refactoring", 6);
		CodeImpRefactoringManager refactoringManager = CodeImpRefactoringManager
				.getManager();

		// Get Refactoring instance for the item
		Refactoring refactoring = null;
		try {
			refactoring = refactoringManager.getRefactoring(pair);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		if (refactoring == null) {
			printLog("No refactoring is created for: "
					+ pair.element.toString());
			return false;
		}
		monitor.worked(1);

		// Check the availability of the refactoring
		RefactoringStatus status = new RefactoringStatus();
		try {
			status = refactoring.checkInitialConditions(monitor);
		} catch (OperationCanceledException | CoreException e) {
			e.printStackTrace();
		}
		if (status.hasFatalError()) {
			printLog(status.getMessageMatchingSeverity(RefactoringStatus.FATAL));
			throw new OperationCanceledException();
		}
		monitor.worked(2);
		try {
			status = refactoring.checkFinalConditions(monitor);
		} catch (OperationCanceledException | CoreException e) {
			e.printStackTrace();
			throw new OperationCanceledException();
		}
		if (status.hasWarning()) {
			printLog("WARNING - "
					+ status.getMessageMatchingSeverity(RefactoringStatus.WARNING));
		}
		if (status.hasFatalError()) {
			printLog("FATAL - "
					+ status.getMessageMatchingSeverity(RefactoringStatus.FATAL));
			throw new OperationCanceledException();
		}
		if (status.hasError()) {
			printLog("ERROR - "
					+ status.getMessageMatchingSeverity(RefactoringStatus.ERROR));
			throw new OperationCanceledException();
		}
		monitor.worked(3);
		// Create appropriate changes in the refactoring
		Change change;
		try {
			change = refactoring.createChange(monitor);
		} catch (OperationCanceledException | CoreException e) {
			e.printStackTrace();
			return false;
		}
		monitor.worked(4);
		// Start actions of the refactoring
		undoMan.aboutToPerformChange(change);
		Change fUndoChange;
		try {
			fUndoChange = change.perform(new SubProgressMonitor(monitor, 1));
		} catch (CoreException e) {
			e.printStackTrace();
			return false;
		}
		monitor.worked(5);
		// Add the changes to the undo manager to record
		change.dispose();
		if (fUndoChange != null) {
			undoMan.changePerformed(change, true);
			fUndoChange.initializeValidationData(new SubProgressMonitor(
					monitor, 1));
			undoMan.addUndo(refactoring.getName(), fUndoChange);
		}
		monitor.worked(6);
		return true;
	}

	public CodeImpRefactoring() {
		super();
	}

}