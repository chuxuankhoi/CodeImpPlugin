<<<<<<< HEAD
package codeimp.refactoring;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
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

public class AtomicRefactoring implements IPerformable {
	protected Object object;
	protected String action;
	protected Map<String, String> additions;
	protected IResource resource;

	public AtomicRefactoring(Object object, String action, IResource resource,
			String... additions) {
		this.object = object;
		this.action = action;
		this.additions = new HashMap<String, String>();
		if (additions != null) {
			for (int i = 0; i < additions.length; i += 2) {
				this.additions.put((String) additions[i],
						(String) additions[i + 1]);
			}
		}
		this.resource = resource;
	}

	public boolean perform(IUndoManager undoManager, IProgressMonitor monitor) {
		if (object == null || action == null) {
			throw new OperationCanceledException();
		}
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		RefactoringPair pair = null;
		if (additions == null || additions.size() == 0) {
			pair = CodeImpRefactoringManager.getManager()
					.getDefaultRefactoringPairs((IJavaElement) object, action)[0];
		} else {
			pair = new RefactoringPair();
			pair.element = object;
			pair.action = action;
			pair.addition = additions;
			pair.resource = resource;
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
			CodeImpUtils.printLog("No refactoring is created for: "
					+ object.toString());
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
			CodeImpUtils.printLog(status
					.getMessageMatchingSeverity(RefactoringStatus.FATAL));
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
			CodeImpUtils
					.printLog("WARNING - "
							+ status.getMessageMatchingSeverity(RefactoringStatus.WARNING));
		}
		if (status.hasFatalError()) {
			CodeImpUtils
					.printLog("FATAL - "
							+ status.getMessageMatchingSeverity(RefactoringStatus.FATAL));
			throw new OperationCanceledException();
		}
		if (status.hasError()) {
			CodeImpUtils
					.printLog("ERROR - "
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
		if (undoManager != null) {
			undoManager.aboutToPerformChange(change);
		}
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
		if (undoManager != null && fUndoChange != null) {
			undoManager.changePerformed(change, true);
			fUndoChange.initializeValidationData(new SubProgressMonitor(
					monitor, 1));
			undoManager.addUndo(refactoring.getName(), fUndoChange);
		}
		monitor.worked(6);
		return true;
	}
}
=======
package codeimp.refactoring;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
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

public class AtomicRefactoring implements IPerformable {
	protected Object object;
	protected String action;
	protected Map<String, String> additions;
	protected IResource resource;

	public AtomicRefactoring(Object object, String action, IResource resource,
			String... additions) {
		this.object = object;
		this.action = action;
		this.additions = new HashMap<String, String>();
		if (additions != null) {
			for (int i = 0; i < additions.length; i += 2) {
				this.additions.put((String) additions[i],
						(String) additions[i + 1]);
			}
		}
		this.resource = resource;
	}

	public boolean perform(IUndoManager undoManager, IProgressMonitor monitor) {
		if (object == null || action == null) {
			throw new OperationCanceledException();
		}
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		RefactoringPair pair = null;
		if (additions == null || additions.size() == 0) {
			pair = CodeImpRefactoringManager.getManager()
					.getDefaultRefactoringPairs((IJavaElement) object, action)[0];
		} else {
			pair = new RefactoringPair();
			pair.element = object;
			pair.action = action;
			pair.addition = additions;
			pair.resource = resource;
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
			CodeImpUtils.printLog("No refactoring is created for: "
					+ object.toString());
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
			CodeImpUtils.printLog(status
					.getMessageMatchingSeverity(RefactoringStatus.FATAL));
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
			CodeImpUtils
					.printLog("WARNING - "
							+ status.getMessageMatchingSeverity(RefactoringStatus.WARNING));
		}
		if (status.hasFatalError()) {
			CodeImpUtils
					.printLog("FATAL - "
							+ status.getMessageMatchingSeverity(RefactoringStatus.FATAL));
			throw new OperationCanceledException();
		}
		if (status.hasError()) {
			CodeImpUtils
					.printLog("ERROR - "
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
		
		Change fUndoChange;
		try {
			if (undoManager != null) {
				undoManager.aboutToPerformChange(change);
			}
			fUndoChange = change.perform(new SubProgressMonitor(monitor, 1));
			monitor.worked(5);
			// Add the changes to the undo manager to record
			change.dispose();
			if (undoManager != null && fUndoChange != null) {
				undoManager.changePerformed(change, true);
				fUndoChange.initializeValidationData(new SubProgressMonitor(
						monitor, 1));
				undoManager.addUndo(refactoring.getName(), fUndoChange);
			}
			monitor.worked(6);
		} catch (CoreException e) {
			e.printStackTrace();
			// Add the changes to the undo manager to record
			change.dispose();
			if (undoManager != null) {
				undoManager.changePerformed(change, false);				
			}
			monitor.worked(6);
			return false;
		}
		
		
		return true;
	}
}
>>>>>>> origin/master
