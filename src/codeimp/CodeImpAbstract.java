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
		protected CodeImpProgressBar bar = null;
		protected volatile boolean isCancelled = false;

		CancellableThread(CodeImpProgressBar progressBar) {
			bar = progressBar;
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
		 * @param action
		 */
		public EffectiveRefactorings(String action) {
			super();
			this.action = action;
			effectivePairs = new ArrayList<RefactoringPair>();
			improvementScore = new ArrayList<Double>();
		}

		public void put(RefactoringPair pair, double score) {
			effectivePairs.add(pair);
			improvementScore.add(score);
		}

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
		
		public RefactoringPair[] getRefactoringPairs() {
			if(effectivePairs.size() == 0) {
				return null;
			}
			RefactoringPair[] pairs = new RefactoringPair[effectivePairs.size()];
			pairs = effectivePairs.toArray(pairs);
			return pairs;
		}

	}

	protected IWorkbenchWindow window;
	protected ITextSelection codeSelection;
	protected IFile sourceFile;
	protected IUndoManager undoMan = null;

	public CodeImpAbstract() {
		super();
	}

	protected void printLog(String log) {
		CodeImpUtils.printLog(this.getClass().getName() + " - " + log);
	}

	public double calCurrentScore() {
		if (codeSelection == null || sourceFile == null) {
			printLog("calCurrentScore - Lack of information about the experiment.");
			return 0;
		}
		IJavaElement[] elements = null;
		try {
			elements = CodeImpUtils.identifyElements(codeSelection.getText(),
					sourceFile);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		if (elements == null) {
			printLog("calCurrentScore - No element found by identifier.");
			return 0;
		}
		double score = 0;
		for (IJavaElement element : elements) {
			double elementScore = scoreElement(element);
			score += elementScore;
		}
		return score;
	}

	protected abstract double scoreElement(IJavaElement element);

	public String getRefactoringHistory() {
		if (undoMan != null) {
			return ((CodeImpUndoManager) undoMan).getCurrentUndoListString();
		}
		return "";
	}

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

	public abstract HashMap<String, Double> getEffectiveList(String action);
	
	public abstract RefactoringPair[] getEffectivePairs(String action);

}