package codeimp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ui.IWorkbenchWindow;

import codeimp.refactoring.RefactoringPair;
import codeimp.undo.CodeImpUndoManager;
import codeimp.wizards.CodeImpProgressBar;

public abstract class CodeImpAbstract {

	protected abstract class CancellableThread extends Thread {
		protected CodeImpProgressBar bar = null; // major progress bar to
													// reflect the progress of
													// running
		protected boolean isCancelled = false; // determine that the thread is
												// cancelled or not. If the
												// thread is cancelled, the
												// function run() should be stop
												// immediately

		CancellableThread(CodeImpProgressBar progressBar) {
			bar = progressBar;
			isCancelled = false;
		}

		public abstract void run();

		public void cancel() {
			isCancelled = true;
		}
	}

	/**
	 * Store the effective items for the given refactoring actions
	 * 
	 * @author chuxuankhoi
	 * 
	 */
	protected class EffectiveRefactorings {
		protected String action;
		protected ArrayList<RefactoringPair> effectivePairs;
		protected ArrayList<Double> improvementScore;

		/**
		 * @return the action
		 */
		public String getAction() {
			return action;
		}

		/**
		 * @param action
		 *            the action to set
		 */
		public void setAction(String action) {
			this.action = action;
		}

		/**
		 * Constructor, which initializes the private fields
		 * 
		 * @param action
		 *            string indicating the refactoring action applied to the
		 *            items to achieve the improvement
		 */
		public EffectiveRefactorings(String action) {
			super();
			this.action = action;
			effectivePairs = new ArrayList<RefactoringPair>();
			improvementScore = new ArrayList<Double>();
		}

		/**
		 * Add new refactoring item and its improvement score to the last
		 * position
		 * 
		 * @param pair
		 *            RefactoringPair which stores the information about the
		 *            refactoring {@link RefactoringPair}
		 * @param score
		 *            double number indicating the improvement if the
		 *            refactoring action is applied to the item
		 */
		public void put(RefactoringPair pair, double score) {
			effectivePairs.add(pair);
			improvementScore.add(score);
		}

		/**
		 * Return a map that summarized the item and its corresponding
		 * improvement
		 * 
		 * @return
		 */
		public HashMap<String, Double> getRefactoringMap() {
			HashMap<String, Double> map = new HashMap<String, Double>();

			for (int i = 0; i < effectivePairs.size(); i++) {
				Object element = effectivePairs.get(i).element;
				if (element instanceof IJavaElement) {
					map.put(((IJavaElement) element).getElementName(),
							improvementScore.get(i));
				} else {
					map.put(element.toString(), improvementScore.get(i));
				}
			}

			return map;
		}

		public int size() {
			return effectivePairs.size();
		}

		/**
		 * Return all refactoring pairs that make improvement
		 * 
		 * @return
		 */
		public RefactoringPair[] getRefactoringPairs() {
			if (effectivePairs.size() == 0) {
				return null;
			}
			RefactoringPair[] pairs = new RefactoringPair[effectivePairs.size()];
			pairs = effectivePairs.toArray(pairs);
			return pairs;
		}

	}

	protected IWorkbenchWindow window; // current workbench window
	protected ITextSelection codeSelection; // the code selected which we will
											// try to improve the items in it.
	protected IFile sourceFile; // the opened file which contains the selected
								// code
	protected IUndoManager undoMan = null; // the manager managing undo, redo
											// and history of the refactoring

	public CodeImpAbstract() {
		super();
	}

	protected void printLog(String log) {
		CodeImpUtils.printLog(this.getClass().getName() + " - " + log);
	}

	/**
	 * Calculate overall score of the selected code in the current circumstance
	 * 
	 * @return
	 */
	public double calCurrentScore() {
		if (codeSelection == null || sourceFile == null) {
			printLog("calCurrentScore - Lack of information about the experiment.");
			return Double.MAX_VALUE;
		}
		IJavaElement[] elements = null;
		try {
			elements = CodeImpUtils.identifyElements(codeSelection.getText(),
					sourceFile);
		} catch (JavaModelException e) {
			// e.printStackTrace();
		}
		if (elements == null) {
			printLog("calCurrentScore - No element found by identifier.");
			return Double.MAX_VALUE;
		}
		double score = 0;
		for (IJavaElement element : elements) {
			double elementScore = scoreElement(element);
			score += elementScore;
		}
		return score;
	}

	/**
	 * Calculate the score of a Java element in the current circumstance
	 * 
	 * @param element
	 * @return
	 */
	protected abstract double scoreElement(IJavaElement element);

	/**
	 * Return a string that lists all actions that improve the code quality
	 * 
	 * @return
	 */
	public String getRefactoringHistory() {
		if (undoMan != null) {
			return ((CodeImpUndoManager) undoMan).getCurrentUndoListString();
		}
		return "";
	}

	/**
	 * Function which is expected to stop the improvement action immediately
	 */
	public abstract void cancel();

	/**
	 * Get refactoring actions' result in printable format
	 * 
	 * @return
	 */
	public abstract String getPrintedResults();

	/**
	 * Get refactoring actions' result
	 * 
	 * @return
	 */
	public abstract Map<String, Double> getResults();

	/**
	 * Other version of runImprovement(IProgressMonitor) which reports progress
	 * via progress bar
	 * 
	 * @param progressBar
	 */
	public abstract void runImprovement(CodeImpProgressBar progressBar);

	/**
	 * Return a map that contains all refactoring items which helps us improving
	 * the code quality when we apply the action on them and their effectiveness
	 * 
	 * @param action
	 * @return
	 */
	public abstract HashMap<String, Double> getEffectiveList(String action);

	/**
	 * Get a list of items that helps us improving the code quality when we
	 * apply the action on them
	 * 
	 * @param action
	 * @return
	 */
	public abstract RefactoringPair[] getEffectivePairs(String action);

}