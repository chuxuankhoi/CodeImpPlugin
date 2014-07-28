package codeimp.test.classcohesions;

public class Class3 {
	// 4 fields
	private int f1 = 0;
	public double f2 = 0.0;
	private static String f3 = "It is zero";
	public static boolean f4 = false;

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
		} else {
			return f1;
		}
	}

	public String m3() {
		return f3;
	}

	public boolean m4() {
		return f4;
	}
}
