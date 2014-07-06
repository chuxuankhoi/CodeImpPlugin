/**
 * 
 */
package codeimp.refactoring;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * @author chuxuankhoi
 * 
 */
public class CodeImpRefactoring {

	private RefactoringPair pair;
	private IResource project;

	/**
	 * @param element
	 * @param action
	 * @param project
	 */
	public CodeImpRefactoring(IJavaElement element, String action,
			IResource project) {
		this.pair = new RefactoringPair();
		this.pair.element = element;
		this.pair.action = action;
		this.project = project;
	}

	public CodeImpRefactoring(RefactoringPair refactoringPair, IProject project) {
		this.pair = refactoringPair;
		this.project = project;
	}

	/**
	 * 
	 * @param element
	 * @param action
	 * @throws CoreException
	 */
	public void process(IUndoManager undoMan) {
		// Run the generated actions
		try {
			refactorElement(pair, undoMan);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private void refactorElement(RefactoringPair pair, IUndoManager undoMan)
			throws CoreException {
		if (pair == null) {
			return;
		}
		if (pair.element == null || pair.action == null) {
			return;
		}
		
		CodeImpRefactoringManager refactoringManager = CodeImpRefactoringManager.getManager();

		JavaRefactoringDescriptor descriptor = refactoringManager.getDescriptor(pair,
				project);

		RefactoringStatus status = new RefactoringStatus();

		Refactoring refactoring = descriptor.createRefactoring(status);
		IProgressMonitor monitor = new NullProgressMonitor();
		refactoring.checkInitialConditions(monitor);
		refactoring.checkFinalConditions(monitor);
		Change change = refactoring.createChange(monitor);
		undoMan.aboutToPerformChange(change);
		Change fUndoChange = change.perform(new SubProgressMonitor(monitor, 9));
		undoMan.changePerformed(change, true);
		change.dispose();
		fUndoChange
				.initializeValidationData(new SubProgressMonitor(monitor, 1));
		undoMan.addUndo(refactoring.getName(), fUndoChange);
	}

}
