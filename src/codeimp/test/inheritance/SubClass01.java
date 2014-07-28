package codeimp.test.inheritance;

public class SubClass01 extends GivenClass {
	
	public void sharedMethod() {
		System.out.println("Shared");
	}
	
	public int m2() {
		if(f4) {
			return f1;
		} else {
			return 0;
		}
	}

	public void m1() {
		if (f1 == 0) {
			System.out.println("No output");
		} else {
			System.out.println("f2 is " + f2);
		}
	}
}
