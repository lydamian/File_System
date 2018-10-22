
package lydamian.pkg;

public class FileSystem {
	//data members
	
	int k; //reserved memory space
	int bitmapLength;//bitmaplength
	int numDescriptors; //number of file descriptors
	int fileBlockLength;
	
	
	
	//constructors
	FileSystem(int k, int bitmapLength, int numDescriptors,
			int fileBlockLength){
		this.k = k;
		this.bitmapLength = bitmapLength;
		this.numDescriptors = numDescriptors;
		

		
	}
	
	//class methods
	
	// create a new file with the specified name
	public int create(String[] symbolic_file_name) {
		System.out.println("create function called..\n");
		return -1;
	}
	
	//destroy the named file.
	public int destroy(String[] symbolic_file_name) {
		System.out.println("destroy function called..\n");
		return -1;
	}
	
	
	// open the named file for reading and writing; return an
	// index value which is used by subsequent read, write, 
	// lseek, or close operations.
	public int open(String[] symbolic_file_name) {
		System.out.println("open function called..\n");
		return -1;
	}
	
	// close the specified file
	public int close(int index) {
		return -1;
	}
	
	// sequentially read a number of bytes from the specified
	// file into main meory. The number of bytes to be read is
	// specified in count and the starting memory adress
	// in mem_area. The reading starts with the current position
	// in the file.
	public int read(int index, byte[] mem_area, int count) {
		System.out.println("read function called..\n");
		return -1;
	}
	
	// sequentially write a number of bytes from main memory starting
	// at mem_area into the specified file. As with the read operation, 
	// the number of bytes is given in count and the writing begins with
	// the current position in the file.
	public int write(int index, byte[] mem_area, int count) {
		System.out.println("write function called..\n");
		return -1;
	}
	
	/*
	 * Move the current position of the file to pos, where pos is an
	 * integer specifying the number of bytes from the beginning of the
	 * file. When a file is initially opened, the current position is
	 * automatically set to zero. After each read or write operation. it
	 * points to the byte immediately following the one that was accessed last.
	 * lseek permits the posiition to be explicitely changed without reading
	 * or writing the data. Seeking to position 0 implements a reset command,
	 * so that the entire file can be reread or rewritten from the beginning.
	 */	
	public int lseek(int index, int pos) {
		System.out.println("lseek function called..\n");
		return -1;
	}
	
	// list the names of all files and their lengths
	public int directory() {
		System.out.println("directory method called.\n");
		return -1;
	}
	
}
