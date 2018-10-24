
package lydamian.pkg;

public class FileSystem {
	//data members
	
	int k; //reserved memory space in bytes
	int bitmapLength;//bitmaplength in bytes
	int numDescriptors; //number of file descriptors
	int fileBlockLength; //in blocks
	
	
	
	//constructors
	FileSystem(int bitmapLength, int numDescriptors,
			int fileBlockLength){

		this.bitmapLength = bitmapLength; 
		this.numDescriptors = numDescriptors; //in bytes
		int k = bitmapLength + numDescriptors*4*4; // in bytes
		this.k = k;
		this.fileBlockLength = fileBlockLength;
	}
	
	//class methods
	
	// create a new file with the specified name
	public int create(String[] symbolic_file_name) {
		System.out.println("create function called..\n");
		
		//find a free file descriptor
		for(int i = this.bitmapLength; i < k; i++ ) {
		}
		
		// Find a free entry in the directory ( this is done by rewinding
		// the directory and reading it until a free slot is found; recall
		// that the directory is treated just like any other file. At
		// the same time, verify that the file does not already exists.
		// If it does, return a status error.
		
		//enter the symbolic name and the descriptor index into the found
		//directory entry
		
		//return status
		return -1;
	}
	
	//destroy the named file.
	public int destroy(String[] symbolic_file_name) {
		System.out.println("destroy function called..\n");
		
		//Find the file descriptor by searching the directory
		
		//Remove the directory entry
		
		//Update the bitmap to reflect the freed blocks
		
		//Free the file descriptor
		
		//Return status
		return -1;
	}
	
	
	// open the named file for reading and writing; return an
	// index value which is used by subsequent read, write, 
	// lseek, or close operations.
	public int open(String[] symbolic_file_name) {
		System.out.println("open function called..\n");
		
		//Search the directory to find the index of the file descriptor
		
		//Allocate a free OFT entry(if possible)
		
		//Fill in the current position (zero) and the file descriptor index
		
		//Read the first block of the file into the buffer (read-ahead)
		
		//Return the OFT index (or error status)
		return -1;
	}
	
	// close the specified file
	public int close(int index) {
		//Write the buffer to disk
		
		//Update the file length in descriptor
		
		//Free the OFT entry
		
		//Return status
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
		
		//Read the directory file
		
		//For each entry, 
		// - find file descriptor
		// - print file name and file length
		
		return -1;
	}
	
	//Inner Class
	public class FileDescriptor{
		int fileLength;
		int[] blockNumbers;
		
		FileDescriptor(int fileLength, int blockLengthMax){
			this.fileLength = fileLength;
			this.blockNumbers = new int[blockLengthMax];
		}
	}
	
}
