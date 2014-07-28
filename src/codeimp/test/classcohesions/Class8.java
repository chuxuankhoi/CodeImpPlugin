package codeimp.test.classcohesions;

public class Class8 {
	// 4 fields
	private int f1 = 0;
	public double f2 = 0.0;
	private static String f4 = "";
	public static boolean f3 = false;

	// 4 methods
	public void m1() {
		if(f1 == f2) {
			System.out.println(f3);
		} else {
			System.out.println(!f3);
		}

	}

	public int m2() {
		if(f3) {
			return f1;
		} else {
			return (int) f2;
		}
	}

	public boolean m3() {
		if(f1 > f2) return f3;
		else
			return !f3;
	}

	public void m4() {
		System.out.println("Do nothing");
	}
}
