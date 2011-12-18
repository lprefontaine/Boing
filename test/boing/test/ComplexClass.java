package boing.test;

public class ComplexClass {
	private static String COLON = ":";
	private SimpleClass simpleBeanOne;
	private SimpleClass simpleBeanTwo;

	public ComplexClass() {
		super();
		this.simpleBeanOne = simpleBeanOne;
	}

	public ComplexClass(SimpleClass simpleBeanOne) {
		super();
		this.simpleBeanOne = simpleBeanOne;
	}

	public ComplexClass(SimpleClass simpleBeanOne, SimpleClass simpleBeanTwo) {
		super();
		this.simpleBeanOne = simpleBeanOne;
		this.simpleBeanTwo = simpleBeanTwo;
	}

	public void setSimpleBeanOne(SimpleClass simpleBeanOne) {
		this.simpleBeanOne = simpleBeanOne;
	}

	public void setSimpleBeanTwo(SimpleClass simpleBeanTwo) {
		this.simpleBeanTwo = simpleBeanTwo;
	}

	public String toString() {
		return simpleBeanOne + COLON + simpleBeanTwo;
	}

	public void init() {
		System.out.println("In init() of class ComplexClass");
	}

	public void destroy() {
		System.out.println("In destroy() of class ComplexClass");
	}
}
