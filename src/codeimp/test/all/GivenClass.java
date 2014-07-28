package codeimp.test.all;

public class GivenClass extends Superclass {

	protected double givenAttr1;
	protected boolean givenAttr2;
	private static final String givenAttr3 = "ATTRIBUTE 3";

	protected int givenAttr4 = 0;
	public double givenAttr5 = 0.0;
	protected static String givenAttr6 = "";
	public static boolean givenAttr7 = false;

	protected void pullup1(String param1) {
		System.out.println(param1);
	}

	public void method1() {
		if (super.superMethod2())
			super.superMethod1();
		else {
			pullup1(givenAttr3);
		}
	}

	public void method2() {
		if (givenAttr4 == 0) {
			System.out.println("No output");
		} else {
			System.out.println("f2 is " + givenAttr5);
		}
	}

	public int method3() {
		if (givenAttr7) {
			return givenAttr4;
		} else {
			return (int) givenAttr5;
		}
	}

	public String method4() {
		return givenAttr6;
	}

	public boolean method5() {
		if (givenAttr4 > givenAttr5)
			return givenAttr7;
		else
			return !givenAttr7;
	}

	protected void pushDown2() {
		System.out.println("This is given class");
	}

	public void pushDown1() {
		System.out.println("We are children of the given class");
	}
}
