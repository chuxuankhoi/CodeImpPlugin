/**
 * 
 */
package codeimp.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import codeimp.CodeImpUtils;

/**
 * @author chuxuankhoi
 * 
 */
public class CodeImpRefactoring {

	private void printLog(String log) {
		CodeImpUtils.printLog(this.getClass().getName() + " - " + log);
	}

	private RefactoringPair pair;
	private IFile sourceFile;

	/**
	 * @param element
	 * @param action
	 * @param project
	 */
	public CodeImpRefactoring(IJavaElement element, String action, IFile file) {
		this.pair = new RefactoringPair();
		this.pair.element = element;
		this.pair.action = action;
		this.sourceFile = file;
	}

	public CodeImpRefactoring(RefactoringPair refactoringPair, IFile file) {
		this.pair = refactoringPair;
		this.sourceFile = file;
	}

	/**
	 * 
	 * @param monitor
	 * @param element
	 * @param action
	 * @return 
	 * @throws CoreException
	 */
	public boolean process(IUndoManager undoMan, IProgressMonitor monitor)
			throws Exception {
		// Run the generated actions
		return refactorElement(pair, undoMan, monitor);
	}

	private boolean refactorElement(RefactoringPair pair, IUndoManager undoMan,
			IProgressMonitor monitor){
		if (pair == null) {
			throw new OperationCanceledException();
		}
		if (pair.element == null || pair.action == null) {
			throw new OperationCanceledException();
		}

		monitor.beginTask("Refactoring", 6);
		CodeImpRefactoringManager refactoringManager = CodeImpRefactoringManager
				.getManager();

		Refactoring refactoring = null;
		try {
			refactoring = refactoringManager.getRefactoring(pair,
					sourceFile);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		if (refactoring == null) {
			printLog("No refactoring is created for: "
					+ pair.element.toString());
			return false;
		}
		monitor.worked(1);
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
		if(status.hasWarning()) {
			printLog("WARNING - " + status.getMessageMatchingSeverity(RefactoringStatus.WARNING));
		}
		if (status.hasFatalError()) {
			printLog("FATAL - " + status.getMessageMatchingSeverity(RefactoringStatus.FATAL));
			throw new OperationCanceledException();
		}
		if(status.hasError()) {
			printLog("ERROR - " + status.getMessageMatchingSeverity(RefactoringStatus.ERROR));
//			throw new OperationCanceledException();
		}
		monitor.worked(3);
		Change change;
		try {
			change = refactoring.createChange(monitor);
		} catch (OperationCanceledException | CoreException e) {
			e.printStackTrace();
			return false;
		}
		monitor.worked(4);
		undoMan.aboutToPerformChange(change);
		Change fUndoChange;
		try {
			fUndoChange = change.perform(new SubProgressMonitor(monitor, 1));
		} catch (CoreException e) {
			e.printStackTrace();
			return false;
		}
		monitor.worked(5);
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
}
