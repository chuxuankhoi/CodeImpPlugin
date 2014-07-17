/**
 * Original: org.eclipse.ltk.internal.core.refactoring.UndoManager2
 */
package codeimp.undo;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.commands.operations.TriggeredOperations;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.IUndoManagerListener;
import org.eclipse.ltk.core.refactoring.IValidationCheckResultQuery;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.ltk.internal.core.refactoring.RefactoringCorePlugin;
import org.eclipse.ltk.internal.core.refactoring.UndoableOperation2ChangeAdapter;

/**
 * @author chuxuankhoi
 * 
 */
@SuppressWarnings("restriction")
public class CodeImpUndoManager implements IUndoManager {

	private class OperationHistoryListener implements IOperationHistoryListener {
		public void historyNotification(OperationHistoryEvent event) {
			IUndoableOperation op = event.getOperation();
			if (op instanceof TriggeredOperations) {
				op = ((TriggeredOperations) op).getTriggeringOperation();
			}
			UndoableOperation2ChangeAdapter changeOperation = null;
			if (op instanceof UndoableOperation2ChangeAdapter) {
				changeOperation = (UndoableOperation2ChangeAdapter) op;
			}
			if (changeOperation == null)
				return;
			Change change = changeOperation.getChange();
			switch (event.getEventType()) {
			case OperationHistoryEvent.ABOUT_TO_EXECUTE:
			case OperationHistoryEvent.ABOUT_TO_UNDO:
			case OperationHistoryEvent.ABOUT_TO_REDO:
				fireAboutToPerformChange(change);
				break;
			case OperationHistoryEvent.DONE:
			case OperationHistoryEvent.UNDONE:
			case OperationHistoryEvent.REDONE:
				fireChangePerformed(change);
				fireUndoStackChanged();
				fireRedoStackChanged();
				break;
			case OperationHistoryEvent.OPERATION_NOT_OK:
				fireChangePerformed(change);
				break;
			case OperationHistoryEvent.OPERATION_ADDED:
				// would be better to have different events for this
				fireUndoStackChanged();
				fireRedoStackChanged();
				break;
			case OperationHistoryEvent.OPERATION_REMOVED:
				// would be better to have different events for this
				fireUndoStackChanged();
				fireRedoStackChanged();
				break;
			}
		}
	}

	private static class NullQuery implements IValidationCheckResultQuery {
		public boolean proceed(RefactoringStatus status) {
			return true;
		}

		public void stopped(RefactoringStatus status) {
			// do nothing
		}
	}

	private static class QueryAdapter implements IAdaptable {
		private IValidationCheckResultQuery fQuery;

		public QueryAdapter(IValidationCheckResultQuery query) {
			fQuery = query;
		}

		@SuppressWarnings("rawtypes")
		public Object getAdapter(Class adapter) {
			if (IValidationCheckResultQuery.class.equals(adapter))
				return fQuery;
			return null;
		}
	}

	private IOperationHistory fOperationHistory;
	private IOperationHistoryListener fOperationHistoryListener;

	private boolean fIsOpen;
	private TriggeredOperations fActiveOperation;

	private ListenerList fListeners;

	public CodeImpUndoManager() {
		fOperationHistory = new CodeImpOperationHistory();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ltk.core.refactoring.IUndoManager#addListener(org.eclipse
	 * .ltk.core.refactoring.IUndoManagerListener)
	 */
	@Override
	public void addListener(IUndoManagerListener listener) {
		if (fListeners == null) {
			fListeners = new ListenerList(ListenerList.IDENTITY);
			fOperationHistoryListener = new OperationHistoryListener();
			fOperationHistory
					.addOperationHistoryListener(fOperationHistoryListener);
		}
		fListeners.add(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ltk.core.refactoring.IUndoManager#removeListener(org.eclipse
	 * .ltk.core.refactoring.IUndoManagerListener)
	 */
	@Override
	public void removeListener(IUndoManagerListener listener) {
		if (fListeners == null)
			return;
		fListeners.remove(listener);
		if (fListeners.size() == 0) {
			fOperationHistory
					.removeOperationHistoryListener(fOperationHistoryListener);
			fListeners = null;
			fOperationHistoryListener = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ltk.core.refactoring.IUndoManager#aboutToPerformChange(org
	 * .eclipse.ltk.core.refactoring.Change)
	 */
	@Override
	public void aboutToPerformChange(Change change) {
		IUndoableOperation operation = new UndoableOperation2ChangeAdapter(
				change);
		operation.addContext(RefactoringCorePlugin.getUndoContext());
		fActiveOperation = new TriggeredOperations(operation, fOperationHistory);
		fActiveOperation.addContext(RefactoringCorePlugin.getUndoContext());
		fOperationHistory.openOperation(fActiveOperation,
				IOperationHistory.EXECUTE);
		fIsOpen = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ltk.core.refactoring.IUndoManager#changePerformed(org.eclipse
	 * .ltk.core.refactoring.Change)
	 * 
	 * @deprecated use #changePerformed(Change, boolean) instead
	 */
	@Override
	public void changePerformed(Change change) {
		changePerformed(change, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ltk.core.refactoring.IUndoManager#changePerformed(org.eclipse
	 * .ltk.core.refactoring.Change, boolean)
	 */
	@Override
	public void changePerformed(Change change, boolean successful) {
		if (fIsOpen && fActiveOperation != null) {
			fOperationHistory.closeOperation(successful, false,
					IOperationHistory.EXECUTE);
			fIsOpen = false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ltk.core.refactoring.IUndoManager#addUndo(java.lang.String,
	 * org.eclipse.ltk.core.refactoring.Change)
	 */
	@Override
	public void addUndo(String name, Change change) {
		if (fActiveOperation != null) {
			UndoableOperation2ChangeAdapter operation = (UndoableOperation2ChangeAdapter) fActiveOperation
					.getTriggeringOperation();
			operation.setUndoChange(change);
			operation.setLabel(name);
			fOperationHistory.add(fActiveOperation);
			fActiveOperation = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.IUndoManager#anythingToUndo()
	 */
	@Override
	public boolean anythingToUndo() {
		return fOperationHistory
				.canUndo(RefactoringCorePlugin.getUndoContext());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.IUndoManager#peekUndoName()
	 */
	@Override
	public String peekUndoName() {
		IUndoableOperation op = fOperationHistory
				.getUndoOperation(RefactoringCorePlugin.getUndoContext());
		if (op == null)
			return null;
		return op.getLabel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ltk.core.refactoring.IUndoManager#performUndo(org.eclipse
	 * .ltk.core.refactoring.IValidationCheckResultQuery,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void performUndo(IValidationCheckResultQuery query,
			IProgressMonitor pm) throws CoreException {
		IUndoableOperation undo = fOperationHistory
				.getUndoOperation(RefactoringCorePlugin.getUndoContext());
		UndoableOperation2ChangeAdapter changeOperation = getUnwrappedOperation(undo);
		if (changeOperation == null) {
//			throw new CoreException(new Status(IStatus.ERROR,
//					RefactoringCorePlugin.getPluginId(), IStatus.ERROR,
//					RefactoringCoreMessages.UndoManager2_no_change, null));
			return;
		}
		if (query == null)
			query = new NullQuery();
		try {
			fOperationHistory.undoOperation(undo, pm, new QueryAdapter(query));
		} catch (ExecutionException e) {
			handleException(e);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.IUndoManager#anythingToRedo()
	 */
	@Override
	public boolean anythingToRedo() {
		return fOperationHistory
				.canRedo(RefactoringCorePlugin.getUndoContext());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.IUndoManager#peekRedoName()
	 */
	@Override
	public String peekRedoName() {
		IUndoableOperation op = fOperationHistory
				.getRedoOperation(RefactoringCorePlugin.getUndoContext());
		if (op == null)
			return null;
		return op.getLabel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ltk.core.refactoring.IUndoManager#performRedo(org.eclipse
	 * .ltk.core.refactoring.IValidationCheckResultQuery,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void performRedo(IValidationCheckResultQuery query,
			IProgressMonitor pm) throws CoreException {
		IUndoableOperation redo = fOperationHistory
				.getRedoOperation(RefactoringCorePlugin.getUndoContext());
		UndoableOperation2ChangeAdapter changeOperation = getUnwrappedOperation(redo);
		if (changeOperation == null)
			throw new CoreException(new Status(IStatus.ERROR,
					RefactoringCorePlugin.getPluginId(), IStatus.ERROR,
					RefactoringCoreMessages.UndoManager2_no_change, null));
		if (query == null)
			query = new NullQuery();
		try {
			fOperationHistory.redoOperation(redo, pm, new QueryAdapter(query));
		} catch (ExecutionException e) {
			handleException(e);
		}
	}

	private UndoableOperation2ChangeAdapter getUnwrappedOperation(
			IUndoableOperation operation) {
		IUndoableOperation result = operation;
		if (result instanceof TriggeredOperations) {
			result = ((TriggeredOperations) result).getTriggeringOperation();
		}
		if (result instanceof UndoableOperation2ChangeAdapter) {
			return (UndoableOperation2ChangeAdapter) result;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.IUndoManager#flush()
	 */
	@Override
	public void flush() {
		if (fActiveOperation != null) {
			if (fIsOpen) {
				fOperationHistory.closeOperation(false, false,
						IOperationHistory.EXECUTE);
			}
			/*
			 * the triggering operation is invalid, but we must ensure that any
			 * other operations executed while it was open remain in the undo
			 * history. We accomplish this by adding the invalid operation,
			 * since disposing the context later will cause it to be broken up
			 * into its atomic parts.
			 */
			fOperationHistory.add(fActiveOperation);
		}
		fActiveOperation = null;
		fIsOpen = false;
		fOperationHistory.dispose(RefactoringCorePlugin.getUndoContext(), true,
				true, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.IUndoManager#shutdown()
	 */
	@Override
	public void shutdown() {

	}

	private void handleException(ExecutionException e) throws CoreException {
		Throwable cause = e.getCause();
		if (cause instanceof CoreException) {
			throw (CoreException) cause;
		} else {
			throw new CoreException(
					new Status(
							IStatus.ERROR,
							RefactoringCorePlugin.getPluginId(),
							IStatus.ERROR,
							RefactoringCoreMessages.RefactoringCorePlugin_internal_error,
							e));
		}
	}

	// ---- event firing methods
	// -------------------------------------------------

	private void fireAboutToPerformChange(final Change change) {
		if (fListeners == null)
			return;
		Object[] listeners = fListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			final IUndoManagerListener listener = (IUndoManagerListener) listeners[i];
			SafeRunner.run(new ISafeRunnable() {
				public void run() throws Exception {
					listener.aboutToPerformChange(CodeImpUndoManager.this,
							change);
				}

				public void handleException(Throwable exception) {
					RefactoringCorePlugin.log(exception);
				}
			});
		}
	}

	private void fireChangePerformed(final Change change) {
		if (fListeners == null)
			return;
		Object[] listeners = fListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			final IUndoManagerListener listener = (IUndoManagerListener) listeners[i];
			SafeRunner.run(new ISafeRunnable() {
				public void run() throws Exception {
					listener.changePerformed(CodeImpUndoManager.this, change);
				}

				public void handleException(Throwable exception) {
					RefactoringCorePlugin.log(exception);
				}
			});
		}
	}

	private void fireUndoStackChanged() {
		if (fListeners == null)
			return;
		Object[] listeners = fListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			final IUndoManagerListener listener = (IUndoManagerListener) listeners[i];
			SafeRunner.run(new ISafeRunnable() {
				public void run() throws Exception {
					listener.undoStackChanged(CodeImpUndoManager.this);
				}

				public void handleException(Throwable exception) {
					RefactoringCorePlugin.log(exception);
				}
			});
		}
	}

	private void fireRedoStackChanged() {
		if (fListeners == null)
			return;
		Object[] listeners = fListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			final IUndoManagerListener listener = (IUndoManagerListener) listeners[i];
			SafeRunner.run(new ISafeRunnable() {
				public void run() throws Exception {
					listener.redoStackChanged(CodeImpUndoManager.this);
				}

				public void handleException(Throwable exception) {
					RefactoringCorePlugin.log(exception);
				}
			});
		}
	}

	public String getCurrentUndoListString() {
		IUndoableOperation[] allUndoOp = fOperationHistory
				.getUndoHistory(IOperationHistory.GLOBAL_UNDO_CONTEXT);
		String ret = "";
		for(IUndoableOperation uop: allUndoOp) {
			ret += uop.getLabel();
			ret += "\n";
		}
		return ret;
	}

}
