package codeimp.test.classcohesions;

public class Class7 {
	// 4 fields
	private int f1 = 0;
	public double f2 = 0.0;
	private static String f4 = "";
	public static boolean f3 = false;

	// 4 methods
	public void m1() {
		System.out.println("f1 is " + f1);
	}

	public int m2() {
		return f1;
	}

	public boolean m3() {
		return (f1 >= 0);
	}

	public void m4() {
		System.out.println("Do nothing");
	}
}
