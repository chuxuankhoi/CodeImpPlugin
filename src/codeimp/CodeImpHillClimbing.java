/**
 * 
 */
package codeimp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;

import codeimp.graders.IGrader;
import codeimp.graders.LCOM2;
import codeimp.graders.LCOM5;
import codeimp.graders.LongMethod;
import codeimp.graders.TCC;
import codeimp.refactoring.CodeImpRefactoring;
import codeimp.refactoring.CodeImpRefactoringManager;
import codeimp.refactoring.RefactoringPair;
import codeimp.undo.CodeImpUndoManager;
import codeimp.wizards.CodeImpProgressBar;

/**
 * @author chuxuankhoi
 * 
 *         Improve selected code using hill-climbing algorithm
 * 
 */
public class CodeImpHillClimbing extends CodeImpAbstract {

	private class ImprovementThread extends Thread {
		private Display display = null;
		private CodeImpProgressBar bar = null;
		private volatile boolean isCancelled = false;
		String[] actionList;

		ImprovementThread(Display disp, CodeImpProgressBar bar2) {
			display = disp;
			bar = bar2;
		}

		public void run() {
			if (codeSelection == null || sourceFile == null) {
				printLog("runImprovement - Lack of information about the experiment.");
				return;
			}
			int successfullRefactoring = 0;
			int totalRefactoring = 0;
			double curScore;
			curScore = calCurrentScore();
			startScore = curScore;
			if (curScore <= 0) {
				printLog("Plug-in cannot judge the source code.");
				return;
			}
			double oldScore = curScore;
			if (undoMan == null)
				undoMan = new CodeImpUndoManager();
			CodeImpRefactoringManager refManager = CodeImpRefactoringManager
					.getManager();
			actionList = refManager.getActionsList();
			if (actionList == null || actionList.length == 0) {
				printLog("No refactoring action found.");
				return;
			}
			IJavaElement[] rootElements = null;
			try {
				rootElements = CodeImpUtils.identifyElements(
						codeSelection.getText(), sourceFile);
			} catch (JavaModelException e1) {
				printLog(e1.toString());
			}
			if (rootElements == null) {
				printLog("Unexpected returned.");
				return;
			}
			if (rootElements.length == 0) {
				printLog("No item identified.");
				return;
			}
			System.out.println("Refatored elements:");
			for (int i = 0; i < rootElements.length; i++) {
				System.out.println("\t" + rootElements[i].getElementName()
						+ " - " + rootElements[i].getElementType());
			}

			display.syncExec(new Runnable() {
				@Override
				public void run() {
					bar.setMaximum(actionList.length);
				}
			});

			for (int i = 0; i < actionList.length; i++) {
				curAction = i;
				for (int j = 0; j < rootElements.length; j++) {
					RefactoringPair[] pairs = refManager.getRefactoringPairs(
							rootElements[j], actionList[i], sourceFile);
					if (pairs == null) {
						continue;
					}
					printLog("Pairs number: " + pairs.length);
					for (int k = 0; k < pairs.length; k++) {
						if (isCancelled) {
							finishAction(actionList[i], display, bar);
							return;
						}
						printLog("Old score: " + oldScore);
						if (pairs[k].element instanceof IJavaElement) {
							printLog("Trying "
									+ actionList[i]
									+ " with root "
									+ rootElements[j].getElementName()
									+ " pair element: "
									+ ((IJavaElement) pairs[k].element)
											.getElementName());
						} else if (pairs[k].action == IJavaRefactorings.EXTRACT_METHOD) {
							printLog("Trying "
									+ actionList[i]
									+ " with root "
									+ rootElements[j].getElementName()
									+ " pair of "
									+ ((ITextSelection) (pairs[k].element))
											.getText());
						}
						CodeImpRefactoring refactoring = new CodeImpRefactoring(
								pairs[k], sourceFile);
						totalRefactoring++;
						try {
							refactoring.process(undoMan,
									new NullProgressMonitor());
							successfullRefactoring++;
							sourceFile.refreshLocal(IFile.DEPTH_INFINITE, null);
							curScore = calCurrentScore();
							if (curScore < oldScore) {
								oldScore = curScore;
								continue;
							} else {
								IProgressMonitor undoMonitor = new NullProgressMonitor();
								undoMan.performUndo(null, undoMonitor);
								if (curScore > oldScore) {
									finishAction(actionList[i], display, bar);

									break;
								}
							}
						} catch (Exception e) {
							printLog("Cannot execute the refactoring");
							e.printStackTrace();
							continue;
						}

					}
				}
				finishAction(actionList[i], display, bar);
			}
			refManager.deleteTmpClass();
			printLog("Improvement completed. Final score: " + calCurrentScore());
			printLog("Successfull refactoring: " + successfullRefactoring + "/"
					+ totalRefactoring);
		}

		public void cancel() {
			isCancelled = true;
		}
	}

	private Map<String, Double> results = new HashMap<String, Double>();
	private double startScore = 0;
	ImprovementThread thread = null;
	int curAction = 0;

	public CodeImpHillClimbing(ITextSelection selectedCode, IFile file,
			IWorkbenchWindow currentWindow) {
		codeSelection = selectedCode;
		sourceFile = file;
		window = currentWindow;
	}

	@Override
	public void runImprovement(CodeImpProgressBar bar, Display disp) {
		thread = new ImprovementThread(disp, bar);
		thread.start();
	}

	/**
	 * Get the improvement of the refactoring action and undo all refactoring
	 * 
	 * @param action
	 */
	private void finishAction(String action, Display display,
			final CodeImpProgressBar bar) {
		double curScore = calCurrentScore();
		double impScore = startScore - curScore;
		results.put(action, new Double(impScore));

		while (undoMan.anythingToUndo()) {
			try {
				undoMan.performUndo(null, null);
			} catch (CoreException e) {
			}
		}
		if (!display.isDisposed()) {
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					if (!bar.isDisposed()) {
						bar.setSelection(curAction + 1);
					}
				}
			});
		}
	}

	protected double scoreElement(IJavaElement element) {
		// TODO Select appropriate scoring function for element based on its
		// type
		IGrader grader = null;
		double score = 0;
		if (element instanceof IType) {
			if (CodeImpUtils.isInProject((IType) element,
					sourceFile.getProject())) {
				grader = new LCOM2((IType) element, sourceFile);
				score += grader == null ? 0 : grader.getScore();
				grader = new LCOM5((IType) element, sourceFile);
				score += grader == null ? 0 : grader.getScore();
				grader = new TCC((IType) element, sourceFile);
				score += grader == null ? 0 : grader.getScore();
			}
		} else if (element instanceof IMethod) {
			if (CodeImpUtils.isInProject((IMethod) element,
					sourceFile.getProject())) {
				grader = new LongMethod((IMethod) element);
				score += grader == null ? 0 : grader.getScore();
			}
		}

		return score;
	}

	/**
	 * @see CodeImpAbstract
	 */
	@Override
	public String getPrintedResults() {
		String ret = "";
		Map<String, Double> sortedResults = CodeImpUtils.sortByComparator(
				results, CodeImpUtils.DESC);
		Iterator<Entry<String, Double>> it = sortedResults.entrySet()
				.iterator();
		while (it.hasNext()) {
			Map.Entry<String, Double> e = it.next();
			String result = e.getKey() + " - " + e.getValue().toString();
			ret += result;
			ret += "\n";
		}
		return ret;
	}

	@Override
	public Map<String, Double> getResults() {
		return results;
	}

	@Override
	public void cancel() {
		if (thread != null && thread.isAlive()) {
			thread.cancel();
		}
	}

}
