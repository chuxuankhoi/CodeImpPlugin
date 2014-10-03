package codeimp.test;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class ScoresCollection {

	private static final int METRIC_NUM = 7;

	private static boolean isInitial = false;

	// For evaluating metrics
	private static ArrayList<ArrayList<Double>> scores = new ArrayList<ArrayList<Double>>();
	private static ArrayList<String> actions = new ArrayList<String>();
	
	public static void initialize() {
		if (isInitial) {
			return;
		}
		// LCOM2 LCOM5 TCC InheritedRatio SharedMethodsInChildren
		// SharedMethods EmptyClass
		scores.clear();
		for (int i = 0; i < METRIC_NUM; i++) {
			ArrayList<Double> metricScores = new ArrayList<Double>();
			scores.add(metricScores);
		}
		isInitial = true;
	}
	
	public static void clear() {
		if(!isInitial) {
			return;
		}
		for(ArrayList<Double> list:scores) {
			list.clear();
		}
	}
	
	public static ArrayList<String> getActionsList() {
		return actions;
	}

	public static ArrayList<Double> getLCOM2ScoresList() {
		return scores.get(0);
	}

	public static ArrayList<Double> getLCOM5ScoresList() {
		return scores.get(1);
	}

	public static ArrayList<Double> getTCCScoresList() {
		return scores.get(2);
	}

	public static ArrayList<Double> getSharedMethodsInChildrenScoresList() {
		return scores.get(3);
	}

	public static ArrayList<Double> getSharedMethodScoresList() {
		return scores.get(4);
	}

	public static ArrayList<Double> getInheritanceRatioScoresList() {
		return scores.get(5);
	}

	public static ArrayList<Double> getEmptyClassScoresList() {
		return scores.get(6);
	}
	
	public static ArrayList<Double> getList(int index) {
		if(index >=0 && index < scores.size()) {
			return scores.get(index);
		} else {
			return null;
		}
	}

	public static void exportCSV(String filename) {
		PrintWriter writer;
		try {
			writer = new PrintWriter(filename, "UTF-8");

			// Print title
			writer.println(",LCOM2,LCOM5,TCC,InheritanceRatio,SharedMethodsInChildren, SharedMethods,EmptyClass");

			// Print each line
			int maxLine = getMaxLine();
			for (int i = 0; i < maxLine; i++) {
				writer.print(actions.get(i));
				for (int j = 0; j < scores.size(); j++) {
					writer.print(",");
					if (i < scores.get(j).size()) {
						writer.print(scores.get(j).get(i));
					}
				}
				writer.print("\n");
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	}

	private static int getMaxLine() {
		int max = 0;
		for (ArrayList<Double> metricScores : scores) {
			if (max < metricScores.size()) {
				max = metricScores.size();
			}
		}
		return max;
	}
}
