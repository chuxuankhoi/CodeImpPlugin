/**
 * 
 */
package codeimp.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
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
	public CodeImpRefactoring(IJavaElement element, String action,
			IFile file) {
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
	 * @param element
	 * @param action
	 * @throws CoreException
	 */
	public void process(IUndoManager undoMan) throws Exception {
		// Run the generated actions
			refactorElement(pair, undoMan);
	}

	private void refactorElement(RefactoringPair pair, IUndoManager undoMan)
			throws CoreException {
		if (pair == null) {
			throw new OperationCanceledException();
		}
		if (pair.element == null || pair.action == null) {
			throw new OperationCanceledException();
		}

		CodeImpRefactoringManager refactoringManager = CodeImpRefactoringManager
				.getManager();

		Refactoring refactoring = refactoringManager.getRefactoring(pair,
				sourceFile);
		if (refactoring == null) {
			printLog("No refactoring is created for: "
					+ pair.element.toString());
			throw new OperationCanceledException();
		}
		IProgressMonitor monitor = new NullProgressMonitor();
		RefactoringStatus status = new RefactoringStatus();
		status = refactoring.checkInitialConditions(monitor);
		if (status.hasFatalError()) {
			printLog(status.getMessageMatchingSeverity(RefactoringStatus.FATAL));
			throw new OperationCanceledException();
		}
		status = refactoring.checkFinalConditions(monitor);
		if (status.hasFatalError()) {
			printLog(status.getMessageMatchingSeverity(RefactoringStatus.FATAL));
			throw new OperationCanceledException();
		}
		Change change = refactoring.createChange(monitor);
		undoMan.aboutToPerformChange(change);
		Change fUndoChange = change.perform(new SubProgressMonitor(monitor, 9));
		change.dispose();
		if (fUndoChange != null) {
			undoMan.changePerformed(change, true);
			fUndoChange.initializeValidationData(new SubProgressMonitor(
					monitor, 1));
			undoMan.addUndo(refactoring.getName(), fUndoChange);
		}
	}

}
