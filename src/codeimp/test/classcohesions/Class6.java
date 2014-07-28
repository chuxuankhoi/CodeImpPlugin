package codeimp.test.classcohesions;

public class Class6 {
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
		if(f1 == 0) {
			System.out.println(f3);
			return 0;
		} else
			return f1;
	}

	public boolean m3() {
		if(f4.equals("")) {
			return f3;
		} else {
			return !f3;
		}
	}

	public String m4() {
		if(f2 > 0.0) {
			return f4;
		} else {
			return null;
		}
	}
}
