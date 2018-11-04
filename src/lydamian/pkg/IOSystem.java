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
		
		for(int i = 0; i <l; i++) {
			for(int j = 0; j < b; j++) {
				this.ldisk[i][j] = -1; // -1 means an empty area.
			}
		}
		for(int i = 0; i < this.b/8; i++) {
			this.ldisk[0][i] = 0;
		}
		
		//initialize the bitmap
		// - set the first 8 bytes or 2 ints as 1 becuase the fd plus bitmap is in use.
		this.ldisk[0][0] = (byte) (this.ldisk[0][0] | (byte) 254);
		
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
	public int read_block(int i, char[] p) {
		int char_copied = 0;
		for(int index = 0; index < this.b; index++) {
			p[index] = (char)this.ldisk[i][index];
			char_copied++;
		}
		return char_copied;
	}
	
	public int read_block(int i, byte[] p) {
		int char_copied = 0;
		for(int index = 0; index < this.b; index++) {
			p[index] = this.ldisk[i][index];
			char_copied++;
		}
		return char_copied;
	}
	
	//This copies the number of character corresponding to the block
	// length, B, from main memory starting at the location specified
	// by the pointer p, into the logical block ldisk[i]
	public int write_block(int i, char[] p) {
		int char_copied = 0;
		for(int index = 0; index < p.length; index++) {
			this.ldisk[i][index] = (byte)p[index];
			char_copied++;
		}
		return char_copied;
	}
	
	public int write_block(int i, byte[] p) {
		int char_copied = 0;
		for(int index = 0; index < p.length; index++) {
			this.ldisk[i][index] = (byte)p[index];
			char_copied++;
		}
		return char_copied;
	}
	
	public int saveLDisk(String textFile) {
		return 1;
	}
	
	//if file exists return 1
	//else if file doesnt exists return 0
	public int restoreLDisk(String textFile) {
		return 1;
	}
	
	// This method displays the values stored at ldisk[i]
	//Input: (int) i - row of data located at ldisk
	public void displayData(int i) {
		for(int index = 0; index < b; index++) {
			System.out.print((byte)this.ldisk[i][index]); 
		}
	}
	
}
