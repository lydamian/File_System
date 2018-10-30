
package lydamian.pkg;

import java.util.Arrays;
import java.nio.ByteBuffer;

public class FileSystem {
	//data members
	
	int k; //reserved memory space in bytes
	int bitmapLength;//bitmaplength in bytes
	int numDescriptors; //number of file descriptors
	int fileBlockLength; //in blocks
	
	int l;
	int b;
	
	IOSystem ldiskObj;
	static PackableMemory packMem = new PackableMemory(4);
	
	//constructors
	FileSystem(int bitmapLength, int numDescriptors,
			int fileBlockLength, IOSystem ldiskRef, int l, int b){

		this.bitmapLength = bitmapLength; 
		this.numDescriptors = numDescriptors; //in bytes
		this.k = bitmapLength + (numDescriptors*16); // in bytes
		this.fileBlockLength = fileBlockLength;
		this.ldiskObj = ldiskRef;
		this.l = l;
		this.b = b;
	}
	
	//class methods
	
	//converts byte block to char block
	char[] byteToChar(byte[] block) {
		char[] charBlock = new char[this.b];
		for(int i = 0; i < this.b; i++) {
			charBlock[i] = (char)block[i];
		}
		return charBlock;
	}
	
	//converts char block to byte block
	byte[] charToByte(char[] block) {
		byte[] byteBlock = new byte[this.b];
		for(int i = 0; i < this.b; i++) {
			byteBlock[i] = (byte)block[i];
		}
		return byteBlock;
	}
	
	//converts an integer to a byte array
	byte[] toBytes(int i)
	{
	  byte[] result = new byte[4];

	  result[0] = (byte) (i >> 24);
	  result[1] = (byte) (i >> 16);
	  result[2] = (byte) (i >> 8);
	  result[3] = (byte) (i /*>> 0*/);

	  return result;
	}
	
	// This method sets the corresponding block in the bitmap
	// to the value specified by the argument value.
	int modifyBitmap(int block, int value) {
		return 1;
	}
	
	
	// create a new file with the specified name
	public int create(String symbolic_file_name) {
		System.out.println("create function called..\n");
		
		//local variables
		int fdBlockIndex = this.bitmapLength/this.b;
		int start = bitmapLength/64; //since we skip 0 for directory
		int freeFdI = -1;
		int freeFdJ = -1;

		char[] fileDescriptorBlock = new char[this.b];
		
		//find a free file descriptor
		outerloop:
		for(int i = start; i <= k/64; i++ ) {
			ldiskObj.read_block(i, fileDescriptorBlock);
			
			for(int j = 0; i < this.b; j = j+16) {
				if((byte)fileDescriptorBlock[j] < 0) {
					freeFdI = i;
					freeFdJ = j;
					break outerloop;
				}
			}
		}
		
		System.out.println("freeFdI is: " + freeFdI);
		System.out.println("freeFdJ is: " + freeFdJ);
		
		// Find a free entry in the directory ( this is done by rewinding
		// the directory and reading it until a free slot is found; recall
		// that the directory is treated just like any other file. At
		// the same time, verify that the file does not already exists.
		// If it does, return a status error
		char[] tempBlock =  new char[64];
		ldiskObj.read_block(k/64, tempBlock);
		char[] dir = new char[this.b];
		int freeDirI = 0;
		int freeDirJ = 0;
		
		getout: for(int i = k/64; i < k/64+this.fileBlockLength; i++) {
			ldiskObj.read_block(i, dir);
			
			//check if theres a free directory space
			for(int j = 0; j < this.b; j = j+8) {
				if((byte)dir[j] < 0) {
					freeDirI = i;
					freeDirJ = j;
					break getout;
				}
			}
		}
		
		System.out.println("freeDirI is: " + freeDirI);
		System.out.println("freeDirJ is: " + freeDirJ);

		//enter the symbolic name and the descriptor index into the found
		//directory entry
		byte[] symbolic_name = {0,0,0,0};
		symbolic_name = symbolic_file_name.getBytes();
		int j = 0;
		for(int i = freeDirJ; i < (freeDirJ + symbolic_file_name.length()); i++) {
			dir[i] = (char)symbolic_name[j];
			j++;
		}
		
		int descriptorIndex = (freeFdI*freeFdJ)/16;
		System.out.println("descriptorIndex is: " + descriptorIndex);
		byte[] fdIndex = this.toBytes(descriptorIndex);
		
		j = 0;
		for(int i = freeDirJ+4; i < freeDirJ + 4 + fdIndex.length; i++) {
			dir[i] = (char)fdIndex[j];
			j++;
		}

		//store back into ldisk
		byte[] result = this.toBytes(0);
		j = 0;
		for(int i = freeFdJ; i < freeFdJ + 4; i++) {
			fileDescriptorBlock[i] = (char)result[j];
			j++;
		}
		ldiskObj.write_block(freeFdI, fileDescriptorBlock);
		ldiskObj.write_block(freeDirI, dir);
		
		//return status
		return 1;
	}
	
	//Method checks whether a symbolic name matches a character array.
	boolean isMatchSymbolicName(String symbolic_name, byte[] p) {
		byte[] buf = new byte[4];
		buf = symbolic_name.getBytes();
		if(buf[0] == p[0] && buf[1] == p[1] && buf[2] == p[2] && buf[3] == p[3]) {
			return true;
		}
		return false;
	}
	
	//destroy the named file.
	public int destroy(String symbolic_file_name) {
		System.out.println("deSstroy function called..\n");
		//local variables
		char[] dir = new char[this.b];
		int freeDirI = 0;
		int freeDirJ = 0;
		int fdIndex = -1;
		byte[] tempByte = new byte[4];
		ByteBuffer buf = ByteBuffer.allocate(4);
		
		//Find the file descriptor by searching the directory
		for(int i = this.k; i < this.k+this.fileBlockLength; i++) {
			ldiskObj.read_block(i, dir);
				//check if theres a free directory space
				for(int j = 0; j < this.b; j = j+8) {
					byte[] tempSymbolicName = new byte[4];
					tempSymbolicName[0] = (byte)dir[j];
					tempSymbolicName[1] = (byte)dir[j+1];
					tempSymbolicName[2] = (byte)dir[j+2];
					tempSymbolicName[3] = (byte)dir[j+3];
					
					if(isMatchSymbolicName(symbolic_file_name, tempSymbolicName)) {
						int index = 0;
						for(int a = j+4; j < 8; a++) {
							tempByte[index] = (byte)dir[a];
							index++;
						}
						fdIndex = java.nio.ByteBuffer.wrap(tempByte).getInt();
						System.out.println("fdIndex is: " + fdIndex);
						freeDirI = i;
						freeDirJ = j;
						System.out.println(symbolic_file_name + " matches with " + tempSymbolicName);
						break;
					}
				}
		}
		
		//Remove the directory entry (set corresponding bytes to -1)
		for(int i = freeDirJ; i < freeDirJ + 8; i++) {
			dir[i] = (char)-1;
		}
		ldiskObj.write_block(freeDirI,dir);
		
		//Update the bitmap to reflect the freed blocks ( set to 0 )
		char[] bitmap = new char[64]; 
		ldiskObj.read_block(0, bitmap);
		
		
		//Free the file descriptor
		int fdBlock = fdIndex*16/this.b;
		int fdBlockIndex = fdIndex*16%this.b;
		char[] tempArr = new char[this.b];
		ldiskObj.read_block(fdBlock, tempArr);
		for(int i = fdBlockIndex; i < fdBlockIndex + 16; i++) {
			tempArr[i] = (char)-1;
		}
		ldiskObj.write_block(fdBlock, tempArr);
		//Return status
		return 1;
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
	public class OFT{
		// CLASS VARIABLES
		OpenFile[] openFileArray;
		
		// CLASS CONSTRUCTORS
		OFT(int numElems){
			openFileArray = new OpenFile[numElems];
			
		}
		
		// CLASS METHODS
		
		public class OpenFile{
			byte[] rwbuffer;
			int position;
			int fileDescriptorIndex;
			
			OpenFile(){
				this.rwbuffer = new byte[64];
				this.position = 0;
				this.fileDescriptorIndex = 0;
			}
		}
	}
	
}
