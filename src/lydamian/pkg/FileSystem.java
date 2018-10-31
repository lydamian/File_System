
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
	
	OFT oft;
	
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
		oft = new OFT(4);
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
	protected byte[] toBytes(int i)
	{
	  byte[] result = new byte[4];

	  result[0] = (byte) (i >> 24);
	  result[1] = (byte) (i >> 16);
	  result[2] = (byte) (i >> 8);
	  result[3] = (byte) (i /*>> 0*/);

	  return result;
	}
	
	int fromByteArray(byte[] bytes) {
	     return ByteBuffer.wrap(bytes).getInt();
	}
	
	//this method checks whether the corresponding file name exists or not
	// if it does exists return the corresponding fileDescriptorIndex, else -1
	protected int doesFileExist(String symbolicFileName) {
		//local variables
				char[] dir = new char[this.b];
				int freeDirI = 0;
				int freeDirJ = 0;
				int fdIndex = -1;
				byte[] tempByte = new byte[4];
				
				//Find the file descriptor by searching the directory
				outerloop: for(int i = this.k/64; i < (this.k/64)+this.fileBlockLength; i++) {
					ldiskObj.read_block(i, dir); // THERE IS AN ERROR HEREE!
						//check if theres a free directory space
						for(int j = 0; j < this.b; j = j+8) {
							byte[] tempSymbolicName = new byte[4];
							tempSymbolicName[0] = (byte)dir[j];
							tempSymbolicName[1] = (byte)dir[j+1];
							tempSymbolicName[2] = (byte)dir[j+2];
							tempSymbolicName[3] = (byte)dir[j+3];
							
							if(isMatchSymbolicName(symbolicFileName, tempSymbolicName)) {
								int index = 0;
								for(int a = j+4; a < 8; a++) {
									tempByte[index] = (byte)dir[a];
									index++;
								}
								fdIndex = java.nio.ByteBuffer.wrap(tempByte).getInt();
								System.out.println("fdIndex is: " + fdIndex);
								freeDirI = i;
								freeDirJ = j;
								break outerloop;
							}
						}
				}
				
				if(fdIndex == -1) {
					System.out.println("Could not find a file that matches symbolic_name");
					return -1;
				}
				return fdIndex;
	}
	
	
	// This method sets the corresponding block in the bitmap
	// to the value specified by the argument value.
	protected int modifyBitmap(int block, int value) {
		return 1;
	}
	
	//getCorrespondingBlocks where fileIndex reside
	protected int[] fileIndexToBlock(int fileIndex) {
		int fdBlock = 1 + (fileIndex*16/this.b);
		int fdBlockIndex = fileIndex*16%this.b;
		byte[] tempArr = new byte[this.b];
		int[] blockPointers = new int[3];
		
		int j = 0;
		for(int i = fdBlockIndex+4; i < fdBlockIndex + 16; i = i+4) {
			ldiskObj.read_block(fdBlock, tempArr);
			if(Arrays.copyOfRange(tempArr, i, i + 4)[0] != -1) {
				blockPointers[j] = fromByteArray(Arrays.copyOfRange(tempArr, i, i + 4));
			}
			else {
				blockPointers[j] = -1;
			}
			j++;
		}
		return blockPointers;
		
	}
	
	//This method finds the file descriptors from a directory given a symbolic name
	protected int findFileDescriptorIndex(String symbolicName) {
		return 1; 
	}
	
	// create a new file with the specified name
	public int create(String symbolic_file_name) {
		System.out.println("create function called..\n");
		
		//checking if file exists
		if(this.doesFileExist(symbolic_file_name) != -1) {
			System.out.println("Error, file already exists...");
			return -1;
		}		

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
			
			for(int j = 0; j < this.b; j = j+16) {
				if((byte)fileDescriptorBlock[j] < 0) {
					freeFdI = i;
					freeFdJ = j;
					break outerloop;
				}
			}
		}
		
		//set fileDescriptor file length to 0
		for(int i = freeFdJ; i < freeFdJ + 4; i++){
			fileDescriptorBlock[i] =  0;
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
		byte[] symbolic_name = symbolic_file_name.getBytes();
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
		
		int count = 0;
		for(int i = 0; i < 4; i++) {
			if(p[i] > -1) {
				count++;
			}
		}
		byte[] buf = new byte[count];
		
		for(int i = 0; i < 4; i++) {
			if(p[i] > -1) {
				buf[i] = p[i];
			}
		}
		
		if (Arrays.equals(buf, symbolic_name.getBytes()))
		{
		    System.out.println("Yup, they're the same!");
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
		
		//Find the file descriptor by searching the directory
		outerloop: for(int i = this.k/64; i < (this.k/64)+this.fileBlockLength; i++) {
			ldiskObj.read_block(i, dir); // THERE IS AN ERROR HEREE!
				//check if theres a free directory space
				for(int j = 0; j < this.b; j = j+8) {
					byte[] tempSymbolicName = new byte[4];
					tempSymbolicName[0] = (byte)dir[j];
					tempSymbolicName[1] = (byte)dir[j+1];
					tempSymbolicName[2] = (byte)dir[j+2];
					tempSymbolicName[3] = (byte)dir[j+3];
					
					if(isMatchSymbolicName(symbolic_file_name, tempSymbolicName)) {
						int index = 0;
						for(int a = j+4; a < 8; a++) {
							tempByte[index] = (byte)dir[a];
							index++;
						}
						fdIndex = java.nio.ByteBuffer.wrap(tempByte).getInt();
						System.out.println("fdIndex is: " + fdIndex);
						freeDirI = i;
						freeDirJ = j;
						System.out.println(symbolic_file_name + " matches with " + tempSymbolicName);
						break outerloop;
					}
				}
		}
		
		if(fdIndex == -1) {
			System.out.println("Could not find a file that matches symbolic_name");
			return -1;
		}
		
		//Remove the directory entry (set corresponding bytes to -1)
		for(int i = freeDirJ; i < freeDirJ + 8; i++) {
			dir[i] = (char)-1;
		}
		ldiskObj.write_block(freeDirI,dir);
		
		//Update the bitmap to reflect the freed blocks ( set to 0 )
		// - get the blocks that the file descriptor pointed to.
		// - read in the bitmap and set each bit corresponding to block to 0;
		char[] bitmap = new char[64]; 
		ldiskObj.read_block(0, bitmap);
		
		//Free the file descriptor
		int fdBlock = bitmapLength/64 + fdIndex*16/this.b;
		int fdBlockIndex = fdIndex*16%this.b;
		byte[] tempArr = new byte[this.b];
		ldiskObj.read_block(fdBlock, tempArr);
		for(int i = fdBlockIndex; i < fdBlockIndex + 16; i++) {
			tempArr[i] = -1;
		}
		ldiskObj.write_block(fdBlock, tempArr);
		//Return status
		return 1;
	}
	
	
	// open the named file for reading and writing; return an
	// index value which is used by subsequent read, write, 
	// lseek, or close operations.
	public int open(String symbolic_file_name) {
		System.out.println("open function called..\n");
		//local variables
		int freeTableIndex = 0;
		
		//Search the directory to find  the index of the file descriptor
		char[] dir = new char[this.b];
		int freeDirI = 0;
		int freeDirJ = 0;
		int fdIndex = -1;
		byte[] tempByte = new byte[4];
		int tempBlockIndex;
		
		//Find the file descriptor by searching the directory
		outerloop: for(int i = this.k/64; i < (this.k/64)+this.fileBlockLength; i++) {
			ldiskObj.read_block(i, dir); // THERE IS AN ERROR HEREE!
				//check if theres a free directory space
				for(int j = 0; j < this.b; j = j+8) {
					byte[] tempSymbolicName = new byte[4];
					tempSymbolicName[0] = (byte)dir[j];
					tempSymbolicName[1] = (byte)dir[j+1];
					tempSymbolicName[2] = (byte)dir[j+2];
					tempSymbolicName[3] = (byte)dir[j+3];
					
					if(isMatchSymbolicName(symbolic_file_name, tempSymbolicName)) {
						int index = 0;
						for(int a = j+4; a < 8; a++) {
							tempByte[index] = (byte)dir[a];
							index++;
						}
						fdIndex = java.nio.ByteBuffer.wrap(tempByte).getInt();
						System.out.println("fdIndex is: " + fdIndex);
						freeDirI = i;
						freeDirJ = j;
						System.out.println(symbolic_file_name + " matches with " + tempSymbolicName);
						break outerloop;
					}
				}
		}
		
		if(fdIndex == -1) {
			System.out.println("Could not find a file that matches symbolic_name");
			return -1;
		}
		
		//Allocate a free OFT entry(if possible)
		if(this.oft.availableSpace <= 0) {
			System.out.println("ERROR, Too many files open, please close a file...\n");
			return -1;
		}
		else {
			freeTableIndex = this.oft.maxLength - this.oft.availableSpace;
			this.oft.availableSpace--;
		}
		
		//Fill in the current position (zero) and the file descriptor index
		this.oft.openFileArray[freeTableIndex].fileDescriptorIndex = fdIndex;
		this.oft.openFileArray[freeTableIndex].position = 0;
		
		//Read the first block of the file into the buffer (read-ahead)
		int[] getBlocks = this.fileIndexToBlock(fdIndex);
		if(getBlocks[0] > 0) {
			ldiskObj.read_block(getBlocks[0], this.oft.openFileArray[freeTableIndex].rwbuffer);
		}
		
		//Return the OFT index (or error status)
		return freeTableIndex;
	}
	
	// close the specified file
	public int close(int index) {
		//local variables
		int ldiskBlockIndex;
		
		//Write the buffer to disk
		ldiskBlockIndex = fileIndexToBlock(index)[0];
		ldiskObj.write_block(ldiskBlockIndex, this.oft.openFileArray[index].rwbuffer); 
		//Update the file length in descriptor
		
		//Free the OFT entry
		this.oft.openFileArray[index].position = 0;
		this.oft.openFileArray[index].fileDescriptorIndex = -1;
		for(int i = 0; i < this.b; i++) {
			this.oft.openFileArray[index].rwbuffer[i] = 0;
		}
		
		//Return status
		return 1;
	}
	
	// sequentially read a number of bytes from the specified
	// file into main memory. The number of bytes to be read is
	// specified in count and the starting memory address
	// in mem_area. The reading starts with the current position
	// in the file.
	public int read(int index, byte[] mem_area, int count) {
		System.out.println("read function called..\n");
		//local variables
		
		//computer the position within the read/write buffer that
		// corresponds to the current position within (i.e file length
		// modulo buffer length)
		
		//Start copying bytes from the buffer into the specified main
		//memory location until one of the following happens:
		
		//a) The desired count or the end of the file is reached; in this
		// case, update the current position and return status.
		
		//b) read the next sequential block from the disk into the buffer;
		//continue with step 2)
		
		
		return 1;
	}
	
	// sequentially write a number of bytes from main memory starting
	// at mem_area into the specified file. As with the read operation, 
	// the number of bytes is given in count and the writing begins with
	// the current position in the file.
	public int write(int index, byte[] mem_area, int count) {
		System.out.println("write function called..\n");
		
		//transfer data specified in mem_area to index in OFT 
		// until
		
		//a) desired byte count is satisfied or the end of the buffer
		// is reached.
		
		//b) Else, the buffer is written to disk, the file descriptor
		// and the bitmap are then updated to reflect the new block 
		// and the writing continues at the beggining of the buffer.
		// If file length expands past the last allocated block,
		// a new block must be allocated;
		return 1;
		
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
		int maxLength;
		int availableSpace;
		
		// CLASS CONSTRUCTORS
		OFT(int numElems){
			openFileArray = new OpenFile[numElems];
			this.maxLength = numElems;
			this.availableSpace = 3; // allocate first element for directory
			for(int i = 0; i < numElems; i++) {
				openFileArray[i] = new OpenFile();
			}
		}
		
		// CLASS METHODS
		void displayOFT() {
			for(int i = 0; i < maxLength; i++) {
				System.out.println("Buffer: ");
				for(int j = 0; j < 64; j++) {
					System.out.print(this.openFileArray[i].rwbuffer[j] + " ");
				}
				System.out.println("positions: \n" + openFileArray[i].position);
				System.out.println("file Descriptor Index: \n" + this.openFileArray[i].fileDescriptorIndex);
			}
		}
		
		public class OpenFile{
			byte[] rwbuffer;
			int position;
			int fileDescriptorIndex;
			
			OpenFile(){
				this.rwbuffer = new byte[64];
				this.fileDescriptorIndex = -1 ;
				this.position = 0;
			}
		}
	}
	
}
