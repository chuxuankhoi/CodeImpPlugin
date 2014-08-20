/**
 * 
 */
package codeimp.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.ltk.core.refactoring.IUndoManager;


/**
 * @author chuxuankhoi
 * Perform a single default refactoring (implemented by Eclipse)
 */
public class CodeImpSingleRefactoring extends CodeImpRefactoring {

	private RefactoringPair pair;
	/**
	 * @param element
	 * @param action
	 * @param project
	 */
	public CodeImpSingleRefactoring(IJavaElement element, String action, IFile file) {
		this.pair = new RefactoringPair();
		this.pair.element = element;
		this.pair.action = action;
		this.sourceFile = file;
	}

	public CodeImpSingleRefactoring(RefactoringPair refactoringPair, IFile file) {
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
	@Override
	public boolean process(IUndoManager undoMan, IProgressMonitor monitor)
			throws Exception {
		// Run the generated actions
		return refactorElement(pair, undoMan, monitor);
	}

	@Override
	public void performUndo(IUndoManager undoMan) {
		try {
			undoMan.performUndo(null, new NullProgressMonitor());
		} catch (CoreException e) {
		}
	}
}
