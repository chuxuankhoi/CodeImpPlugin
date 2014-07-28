package codeimp.test.inheritance;

public class GivenClass extends SuperClass0 {
	protected int f1 = 0;
	public double f2 = 0.0;
	protected static String f3 = "";
	public static boolean f4 = false;
	
	public void m1() {
		if (f1 == 0) {
			System.out.println("No output");
		} else {
			System.out.println("f2 is " + f2);
		}
	}
	
	public int m2() {
		return f1;
	}

	public String m3() {
		return f3;
	}

	public boolean m4() {
		return f4;
	}
}
