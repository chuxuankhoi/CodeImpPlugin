package codeimp.refactoring;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.IUndoManager;

import codeimp.CodeImpUtils;
import codeimp.IComparator;

/**
 * <p>
 * Performing refactorings as scheduled by refactorings's order. There are some
 * schemes of performing:
 * </p>
 * <ul>
 * <li>Parallelization: some refactorings will be performed simultaneously</li>
 * <li>Serialization: some refactorings will be performed step by step</li>
 * <li>Mix: some refactorings will be performed simultaneously and some step by
 * step</li>
 * </ul>
 * 
 * @author khoicx
 * 
 */

public class Scheduler implements IPerformable {
	private class PassedObject {
		public boolean performingResults = false;
	}

	private class PerformingThread extends Thread {
		private SharedObject sharedObject;
		private PassedObject passedObject;
		private IPerformable performable;

		public PerformingThread(IPerformable performable,
				SharedObject sharedObject, PassedObject passedObject) {
			this.sharedObject = sharedObject;
			this.performable = performable;
			this.passedObject = passedObject;
		}

		@Override
		public synchronized void run() {
			passedObject.performingResults = this.performable.perform(
					sharedObject.undoManager, sharedObject.monitor);
			super.run();
		}

	}

	private class SharedObject {
		public IUndoManager undoManager;
		public IProgressMonitor monitor;
	}

	private HashMap<Integer, ArrayList<IPerformable>> items = null;
	private int performedNumber = 0;
	private String action = "";

	public Scheduler() {
		items = new HashMap<Integer, ArrayList<IPerformable>>();
	}

	public boolean perform(IUndoManager undoManager, IProgressMonitor monitor) {
		if (items == null || items.size() == 0) {
			return true;
		}

		ArrayList<Integer> ordersList = new ArrayList<Integer>();
		ordersList.addAll(items.keySet());
		ordersList = (ArrayList<Integer>) CodeImpUtils.sortArrayListByComparator(
				ordersList, new IComparator<Integer>() {

					@Override
					public int compare(Integer a, Integer b) {
						return a.compareTo(b);
					}

				});
		for (int i = 0; i < ordersList.size(); i++) {
			System.out.println("Current order: " + ordersList.get(i));
			ArrayList<IPerformable> performableList = items.get(ordersList
					.get(i));
			if (performableList == null || performableList.size() == 0) {
				continue;
			}
			IPerformable[] performable = new IPerformable[performableList
					.size()];
			performable = performableList.toArray(performable);
			try {
				if (performParallel(performable, undoManager, monitor) == false) {
					return false;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	private boolean performParallel(IPerformable[] performable,
			IUndoManager undoManager, IProgressMonitor monitor)
			throws InterruptedException {
		if (performable == null || performable.length == 0) {
			return true;
		}
		if(performable.length == 1) {
			if(performable[0].perform(undoManager, monitor)) {
				performedNumber++;
				return true;
			}
			return false;
		}
		final SharedObject sharedObject = new SharedObject();
		final PassedObject[] passedObjects = new PassedObject[performable.length];
		sharedObject.undoManager = undoManager;
		sharedObject.monitor = monitor;
		Thread[] threads = new Thread[performable.length];
		for (int i = 0; i < performable.length; i++) {
			passedObjects[i] = new PassedObject();
			threads[i] = new PerformingThread(performable[i], sharedObject,
					passedObjects[i]);
			threads[i].start();
		}
		for (int i = 0; i < performable.length; i++) {
			threads[i].join();
		}
		
		boolean result = false;
		for (int i = 0; i < performable.length; i++) {
			if(passedObjects[i].performingResults == false) {
				result = false;
			} else {
				performedNumber++;
			}
		}
		return result;
	}

	public void pushRefactoring(IPerformable refactoring, int order) {
		if(refactoring == null) {
			return;
		}
		ArrayList<IPerformable> performable = null;
		performable = items.get(new Integer(order));
		if (performable == null) {
			performable = new ArrayList<IPerformable>();
			performable.add(refactoring);
			items.put(new Integer(order), performable);
		} else {
			performable.add(refactoring);
		}
	}

	public void printPerformingOrders(PrintStream out) {
		ArrayList<Integer> ordersList = new ArrayList<Integer>();
		ordersList.addAll(items.keySet());
		ordersList = (ArrayList<Integer>) CodeImpUtils.sortArrayListByComparator(
				ordersList, new IComparator<Integer>() {

					@Override
					public int compare(Integer a, Integer b) {
						return a.compareTo(b);
					}

				});
		for (int i = 0; i < ordersList.size(); i++) {
			ArrayList<IPerformable> performableList = items.get(ordersList
					.get(i));
			for (IPerformable p : performableList) {
				for (int j = 0; j < i; j++) {
					out.print("\t");
				}
				out.println(p.toString());
			}
		}
	}

	public boolean removeRefactoring(IPerformable refactoring) {
		if(refactoring == null) {
			return true;
		}
		for (Entry<Integer, ArrayList<IPerformable>> e : items.entrySet()) {
			ArrayList<IPerformable> list = e.getValue();
			if (list.remove(refactoring)) {
				return true;
			}
		}
		return false;
	}

	public void undo(IUndoManager undoManager, int undoStepsNumber) {
		if (undoStepsNumber <= 0) {
			return;
		} else {
			int undoNum = Math.min(undoStepsNumber, performedNumber);
			for (int i = 0; i < undoNum; i++) {
				try {
					undoManager.performUndo(null, null);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void undoAll(IUndoManager undoManager) {
		for (int i = 0; i < performedNumber; i++) {
			try {
				undoManager.performUndo(null, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public String toString() {
		return action;
	}
}
