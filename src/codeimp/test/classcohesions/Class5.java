package codeimp.test.classcohesions;

public class Class5 {
	// 4 fields
	private int f1 = 0;
	public double f2 = 0.0;
	private static String f4 = "";
	public static boolean f3 = false;

	// 4 methods
	public void m1() {
		if(f1 == 0) {
			System.out.println("No output");
		} else {
			System.out.println("f2 is " + f2);
		}
	}

	public int m2() {
		if (m3())
			return f1;
		else {
			return 0;
		}
	}

	private boolean m3() {
		return false;
	}

	public String m4() {
		if(m3())
			return null;
		else
			return f4;
	}
}
