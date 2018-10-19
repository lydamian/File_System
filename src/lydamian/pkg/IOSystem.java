package lydamian.pkg;

public class IOSystem {
	//Data Members
	private int l;
	private int b;
	
	private byte[][] ldisk;
	
	//constructors
	public IOSystem(int l, int b){
		System.out.println("l is " + l + "\n");
		System.out.println("b is " + b + "\n");
		this.l = l;
		this.b = b;
		this.ldisk = new byte[l][b];
	}
	
	public IOSystem(){
		this.l = 0;
		this.b = 0;
		System.out.println("ldisk is initialized with 0 blocks and 0 bytes.\n");
	}
	
	//class methods
	public void testIOSystem() {
		System.out.println("object of IOSystem is working properly\n");
		System.out.println("array size is: " + ldisk.length + " x " + ldisk[0].length);
	}
	
	//This method copies the logical block ldisk[i] into main memory
	// starting at the location specified by the pointer p.
	// The number of characters copied corresponding to to
	// the block length, B
	public int read_block(int i, byte &p) {
		int char_copied = 0;
		for(int index = 0; index < this.b; index++) {
			p = this.ldisk[i][index];
			char_copied++;
		}
		return char_copied;
	}
	
	//This copies the number of character corresponding to the block
	// length, B, from main memory starting at the location specified
	// by the pointer p, into the logical block ldisk[i]
	public int write_block(int i, char p) {
		int char_copied = 0;
	}
	
	public int saveLDisk() {
		return 1;
	}
	
	public int restoreLDisk() {
		return 1;
	}
	
}
