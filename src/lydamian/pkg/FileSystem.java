
package lydamian.pkg;

import java.util.Arrays;
import java.nio.ByteBuffer;

public class FileSystem {
	//data members
	
	int k; //reserved memory space in bytes
	int bitmapLength;//bitmaplength in bytes
	int numDescriptors; //number of file descriptors
	int fileBlockLength; //in blocks
	
	int[] BM;
	int[] MASK;
	
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
		
		BM = new int[this.b/32]; //intially set all values in the bitmap to 0 except first 10 blocks
		MASK = new int[32]; //initialize MASK.
		
		//initializing mask
		int j = 0;
		for (int n = 31; n >= 0; n--) {
		   MASK[j] = 1 << n;
		   j++;
		}
		
		for(int i = 0; i < 7; i++) { //set the first 10 blocks as allocated
			BM[0] = BM[0] | MASK[i];
		}
		
		//initializing fd descriptor for directory
		byte[] tempByte = new byte[this.b];
		ldiskObj.read_block(1, tempByte);
		byte[] seven = toBytes(7);
		byte[] eight = toBytes(8);
		byte[] nine = toBytes(9);
		byte[] dirLength = toBytes(192);
		
		//initialize file length to 0
		//initialize blocks to 7 8 9
		int f = 0;
		for(int i = 0; i < 4; i++ ) {
			tempByte[i] = 0;
			f++;
		}
		
		f = 0;
		for(int i = 4; f < seven.length; i++ ) {
			tempByte[i] = seven[f];
			f++;
		}
		
		f = 0;
		for(int i = 8; f < eight.length; i++ ) {
			tempByte[i] = eight[f];
			f++;
		}
		
		f = 0;
		for(int i = 12; f < nine.length; i++ ) {
			tempByte[i] = nine[f];
			f++;
		}
		
		ldiskObj.write_block(1, tempByte);
		
		setBitmap(7,1);
		setBitmap(8,1);
		setBitmap(9,1); 
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
	
	int setFileLength(int fdIndex, int newFileLength) {
		int fdBlock = 1+(fdIndex*16)/this.b;
		int fdBlockIndex = fdIndex*16%this.b;
		byte[] fileDescriptorBlock = new byte[this.b];
		byte[] numByte = toBytes(newFileLength);
		
		int j = 0;
		ldiskObj.read_block(fdBlock, fileDescriptorBlock);
		for(int i = fdBlockIndex; i < fdBlockIndex+4; i++) {
			fileDescriptorBlock[i] = numByte[j];
			j++;
		}
		ldiskObj.write_block(fdBlockIndex, fileDescriptorBlock);
		return 1;
	}
	
	//converts a 4 byte array into a integer
	int fromByteArray(byte[] bytes) {
	     return ByteBuffer.wrap(bytes).getInt();
	}
	
	// This method finds the max length a specified file from a fdIndex
	// it receives as input a file descriptor
	// and returns the total length used by fdIndex.
	protected int getFileMaxLength(int fileIndex) {
		//local variables
		int maxLength = 0;
		byte[] byteArr = new byte[4];
		byte[] tempArr = new byte[this.b];
		
		//finding the max length
		int fdBlock = 1 + (fileIndex*16/this.b);
		int fdBlockIndex = fileIndex*16%this.b;
		ldiskObj.read_block(fdBlock, tempArr);
		
		for(int i = fdBlockIndex; i < fdBlockIndex + 4; i = i+4) {
			maxLength += fromByteArray(Arrays.copyOfRange(tempArr, i, i + 4));
		}
		
		System.out.println("the maxLength is: " + maxLength);
		return maxLength;
	}
	
	//this method returns the available space in the block
	protected int availableSpaceInBlock(int block){
		if(block == -1) {
			System.out.println("Error in finding the available space in a block");
			return -1;
		}
		
		int availableSpace = 0;
		byte[] blockArr = new byte[this.b];
		this.ldiskObj.read_block(block, blockArr);
		
		for(int i = 0; i < this.b; i++) {
			if(blockArr[i] == -1) {
				availableSpace = this.b - (i-1);
				break;
			}
		}
		
		return availableSpace;
	}
	
	protected void clearBuffer(int index) {
		for(int i = 0; i < this.b; i++) {
			this.oft.openFileArray[index].rwbuffer[i] = 0;
		}
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
				byte[] tempSymbolicName = new byte[4];
				
				//Find the file descriptor by searching the directory
				outerloop: for(int i = this.k/64; i < (this.k/64)+this.fileBlockLength; i++) {
					ldiskObj.read_block(i, dir); // THERE IS AN ERROR HEREE!
						//check if theres a free directory space
						for(int j = 0; j < this.b; j = j+8) {
							
							tempSymbolicName[0] = (byte)dir[j];
							tempSymbolicName[1] = (byte)dir[j+1];
							tempSymbolicName[2] = (byte)dir[j+2];
							tempSymbolicName[3] = (byte)dir[j+3];
							
							if(isMatchSymbolicName(symbolicFileName, tempSymbolicName)) {
								int index = 0;
								for(int a = j+4; a < j+8; a++) {
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
	
	//This method finds the number of blocks allocated for a file
	protected int findBlocksAllocated(int fdIndex) {
		int fdBlock = 1+(fdIndex*16/64);
		int fdBlockIndex = fdIndex*16%64;
		int[] fileBlocks = new int[3];
		int count = 0;
		
		fileBlocks = fileIndexToBlock(fdIndex);
		
		for(int i = 0; i < 3; i++) {
			if(fileBlocks[i] != -1) {
				count++;
			}
		}
		return count;
		
	}
	
	//This method allocates a new block for a corresponding fdIndex
	// returns the newly allocated block
	 protected int allocateNewBlock(int fdIndex) {
		 //local variables
		 int newBlock;
		 int fdBlock = 1+(fdIndex*16/64);
		 int fdBlockIndex = fdIndex*16%64;
		 int freeBlockIndex;
		 byte temp; 
		 
		 if((newBlock = findAvailableBlock()) == -1){
			 System.out.println("No more free blocks");
			 return -1;
		 }
		 
		 //check if this file descriptor has reached its maximum number of blocks allocated
		 if(findBlocksAllocated(fdIndex) > 2) {
			 System.out.println("Max number of blocks allocated for this file descriptor");
			 return -1;
		 }
		 
		 //allocate a new block
		 // - read in ldisk for filedescriptor
		 // - update filedescriptor
		 // - write file descriptor back.
		 freeBlockIndex = findBlocksAllocated(fdIndex);
		 newBlock = findAvailableBlock();
		 byte[] byteBlock = toBytes(newBlock);
		 byte[] fileDescriptorBlock = new byte[this.b];
		 ldiskObj.read_block(fdBlock, fileDescriptorBlock);
		 int j = 0;
		 for(int i = fdBlockIndex + 4 + (freeBlockIndex*4); i < fdBlockIndex+4+(freeBlockIndex*4) + 4; i++){
			 fileDescriptorBlock[i] = byteBlock[j];
			 j++;
		 }
		 ldiskObj.write_block(fdBlock, fileDescriptorBlock);
		 
	     //update bitmap
		 setBitmap(newBlock, 1);
		 
		 //return block that was allocated
		 return newBlock;
	 }
	
	// This method sets the corresponding block in the bitmap
	// to the value specified by the argument value.
	protected int setBitmap(int block, int value) {
		if(block > 64 ||  value > 1 || value < 0) {
			System.out.println("Error Parameters out of range in method setBitmap");
			return -1;
		}
		
		if(block > 31) {
			if(value == 0) {
				BM[1] = BM[1] & ~MASK[block%32];
			}
			else {
				BM[1] = BM[1] | MASK[block%32];
			}
		}
		else {
			if(value == 0) {
				BM[0] = BM[0] & ~MASK[block%32];
			}
			else {
				BM[0] = BM[0] | MASK[block%32];
			}
		}
		return 1;
	}
	
	//this method gets the bitmap value at a specified block mapping.
	protected int getBitmapValue(int block) {
		return 1;
	}
	
	//this method writes the current bitmap cached in a datastructure to ldisk.
	protected void writeBitmap() {
		
	}
	
	//Finds to the next available block by searching through the bitmap
	int findAvailableBlock() {
		int test;
		int counter = 0;
		
		for(int i = 0; i < 2; i++) {
			for(int j = 0; j < 32; j++, counter++) {
				test = BM[i] & MASK[j];
				if(test == 0) {
					return counter;
				}
			}
		}
		
		return -1; // no blocks are available! ahhh
	}
	
	//This method returns the appropriate block index corresponding
	// to the given file index and file block index
	protected int getAppropriateBlock(int fdIndex, int curr_file_index){
		//local variables
		byte[] tempBlock = new byte[this.b];
		int appropiateBlock;
		byte[] byteInt = new byte[4];
		
		int fdBlock = 1 + ((fdIndex * 16)/this.b);
		int fdBlockIndex = (fdIndex * 16)%this.b;
		
		this.ldiskObj.read_block(fdBlock, tempBlock);
		
		int start = fdBlockIndex + 4 + curr_file_index*4;
		
		int j = 0;
		for(int i = start; i < start + 4; i++) {
			if(tempBlock[i] == -1) {
				System.out.println("Error, block requested to read is empty...");
				return -1;
			}
			byteInt[j] = tempBlock[i];
			j++;
		}
		
		return fromByteArray(byteInt);
	}
	
	//getCorrespondingBlocks where fileIndex points to
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
	
	//destroy the named file.
	public int destroy(String symbolic_file_name) {
		System.out.println("deSstroy function called..\n");
		//local variables
		byte[] dir = new byte[this.b];
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
						for(int a = j+4; a < j+8; a++) {
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
			dir[i] = -1;
		}
		ldiskObj.write_block(freeDirI,dir);
		
		//Free the file descriptor
		int fdBlock = bitmapLength/64 + fdIndex*16/this.b;
		int fdBlockIndex = fdIndex*16%this.b;
		
		System.out.println("fdBlock is: " + fdBlock);
		System.out.println("fdBlockIndex is: " + fdBlockIndex);
		
		byte[] tempArr = new byte[this.b];
		ldiskObj.read_block(fdBlock, tempArr);
		for(int i = fdBlockIndex; i < fdBlockIndex + 16; i++) {
			tempArr[i] = -1;
		}
		ldiskObj.write_block(fdBlock, tempArr);
		
		//Update the bitmap to reflect the freed blocks ( set to 0 )
		//  - get the blocks that the file descriptor pointed to.
		int[] freedBlocks = fileIndexToBlock(fdIndex);
		for(int i = 0; i < freedBlocks.length; i++) {
			if(freedBlocks[i] > 0) {
				setBitmap(freedBlocks[i], 0);
			}
		}
		
		//Return status
		return 1;
	}
	
	
	// open the named file for reading and writing; return an
	// index value which is used by subsequent read, write, 
	// lseek, or close operations.
	public int open(String symbolic_file_name) {
		System.out.println("open function called..\n");
		//local variables
		int freeTableIndex = 1;
		
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
						for(int a = j+4; a < j+8; a++) {
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
			for(int i = 1; i < 4; i++) {
				if(this.oft.openFileArray[i].fileDescriptorIndex == -1) {
					freeTableIndex = 1;
				}
			}

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
		System.out.println("close method called...\n");
		
		//local variables
		int ldiskBlockIndex;
		int fdIndex = this.oft.openFileArray[index].fileDescriptorIndex;
		int lastBlockIndex = 0;
		int newBlock;
		
		//Write the buffer to disk
		
		//writing the buffer to the lastBlockIndex
		int[] fdBlocks = fileIndexToBlock(fdIndex);
		lastBlockIndex = this.oft.openFileArray[index].position/this.b;
			
		int lastBlock = fdBlocks[lastBlockIndex];
		if(lastBlock == -1) {
			//need to allocate a new block
			newBlock = allocateNewBlock(fdIndex);
			ldiskObj.write_block(newBlock, this.oft.openFileArray[index].rwbuffer);
			System.out.println("a newly allocated block at block: " + newBlock);
			System.out.println("This is the contents of the written block: " );
			ldiskObj.displayData(newBlock);
			//Update the file length in descriptor
		}
		else {
			ldiskObj.write_block(lastBlock, this.oft.openFileArray[index].rwbuffer);
			System.out.println("contents were written to block : " + lastBlock);
			System.out.println("This is the contents of the written block: " );
			ldiskObj.displayData(lastBlock);
			//Update the file length in descriptor
		}
		
		//Free the OFT entry
		this.oft.openFileArray[index].position = 0;
		this.oft.openFileArray[index].fileDescriptorIndex = -1;
		for(int i = 0; i < this.b; i++) {
			this.oft.openFileArray[index].rwbuffer[i] = 0;
		}
		this.oft.availableSpace++;
		

		
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
		int position;
		int numCounted = 0;
		int fdIndex = this.oft.openFileArray[index].fileDescriptorIndex;
		int maxFileLength = 64*3;
		int j = 0;
		int curr_file_index = this.oft.openFileArray[index].position/this.b;
		
		//compute the position within the read/write buffer that
		// corresponds to the current position within the file (i.e file length
		// modulo buffer length)
		position = this.oft.openFileArray[index].position; //reading from the current position in the file
		
		//Start copying bytes from the buffer into the specified main
		//memory location until one of the following happens:
		for(int i = this.oft.openFileArray[index].position%this.b;
				numCounted < count && position < maxFileLength;
					i++, numCounted++, position++, j++) {
			if(i/this.b >= 1) { //end of buffer is reached
				   
				//write the buffer into the appropriate block on disk (if modified)
				int appropriate_block = getAppropriateBlock(fdIndex,curr_file_index);
				if(appropriate_block == -1) {
					System.out.println("cannot write to block, does not exist");
					return -1;
				}
				ldiskObj.write_block(appropriate_block,this.oft.openFileArray[index].rwbuffer);
				
				//read the next sequential block from the disk into the buffer
				appropriate_block = getAppropriateBlock(fdIndex, curr_file_index + 1);
				if(appropriate_block == -1) {
					System.out.println("cannot read anymore from the file...too many bytes requested..\n");
					return -1;
				}
				this.ldiskObj.read_block(appropriate_block,this.oft.openFileArray[index].rwbuffer);
				
				i = 0;
				//continue with step 2
			}
			
			mem_area[j] = this.oft.openFileArray[index].rwbuffer[i];
		}
		
		//a) The desired count or the end of the file is reached; in this
		// case, update the current position and return status.
		
		//b) read the next sequential block from the disk into the buffer;
		//continue with step 2)
		
		this.oft.openFileArray[index].position = position;
		return 1;
	}
	
	// sequentially write a number of bytes from main memory starting
	// at mem_area into the specified file. As with the read operation, 
	// the number of bytes is given in count and the writing begins with
	// the current position in the file.
	public int write(int index, byte[] mem_area, int count) {
		System.out.println("write function called..\n");
		
		//local variables
		int currPosition;
		int counter = 0;
		int fdIndex = this.oft.openFileArray[index].fileDescriptorIndex;
		int currBlock;
		int newBlock = -1;
		int maxFileLength = this.b*3;
		int j = 0;
		
		currPosition = this.oft.openFileArray[index].position;
		currBlock = currPosition/this.b; 
		
		for(int i = currPosition%this.b;
				counter < count && currPosition < maxFileLength && j < mem_area.length
				;counter++, currPosition++, i++, j++
					) {
			if(i/this.b >= 1) { //we went through a block already, need to allocate new block
				//buffer is written to disk - check if there is space on disk
				if(currBlock >= findBlocksAllocated(fdIndex)) { //need to allocate a new block
					newBlock = allocateNewBlock(fdIndex);
					ldiskObj.write_block(newBlock, this.oft.openFileArray[index].rwbuffer);
					i = 0;
					
					//update bitmap
					setBitmap(newBlock, 1);
				}
				else {//just write block
					ldiskObj.write_block(getAppropriateBlock(fdIndex, currBlock), this.oft.openFileArray[index].rwbuffer);
					i = 0;
				}
			}
			
			this.oft.openFileArray[index].rwbuffer[i] = mem_area[j];
		}
		
		this.oft.openFileArray[index].position = currPosition;
		return counter;
	}
	
	// sequentially read a number of bytes from the specified
	// file into main memory. The number of bytes to be read is
	// specified in count and the starting memory address
	// in mem_area. The reading starts with the current position
	// in the file.
	public int read(int index, char mem_area, int count) {
		System.out.println("read function called...");
	
		return 1;
	}
	
	// sequentially write a number of bytes from main memory starting
	// at mem_area into the specified file. As with the read operation, 
	// the number of bytes is given in count and the writing begins with
	// the current position in the file.
	public int write(int index, char mem_area, int count) {
		System.out.println("write function called...");
		int counter;
		int currPosition = this.oft.openFileArray[index].position;
		int maxFileLength = this.b*3;
		int i; //computes the position within the buffer
		int currBlock = currPosition/this.b;
		int fdIndex = this.oft.openFileArray[index].fileDescriptorIndex;
		int newBlock;
		int fileLength = getFileMaxLength(fdIndex);
		
		for(i = currPosition%this.b, counter = 0 ;
				counter < count && currPosition < maxFileLength;
					counter++, currPosition++,i++
				) {
			if(i/this.b >= 1) {
				//buffer is written to disk - check if there is space on disk
				if(currBlock >= findBlocksAllocated(fdIndex)) { //need to allocate a new block
					newBlock = allocateNewBlock(fdIndex);
					ldiskObj.write_block(newBlock, this.oft.openFileArray[index].rwbuffer);
					
					currBlock++;
					
					//clearing buffer
					clearBuffer(index);
					
					//update bitmap
					setBitmap(newBlock, 1);
					
					//update file length in descriptor
					fileLength = fileLength + i;
					setFileLength(fdIndex, fileLength);
					
					i = 0;
				}
				else {//just write block
					ldiskObj.write_block(getAppropriateBlock(fdIndex, currBlock), this.oft.openFileArray[index].rwbuffer);
					
					//clearing buffer
					clearBuffer(index);
					
					//update file length in descriptor
					fileLength = fileLength + i;
					
					i = 0;
				}
			}
			else {
				this.oft.openFileArray[index].rwbuffer[i] = (byte)mem_area;
			}
			
		}	
		
		this.oft.openFileArray[index].position = currPosition;
		return counter;
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
		//local variables
		int currPosition = this.oft.openFileArray[index].position;
		int currBlock = currPosition/this.b;
		int toPos = pos/this.b;
		int fdIndex = this.oft.openFileArray[index].fileDescriptorIndex;
		
		// If the new position is not within the current data block,
		//	- write the buffer into the appropriate block on disk
		//	- read the new data block from disk onto the buffer
		
		if(currBlock != toPos) {
			ldiskObj.write_block(getAppropriateBlock(fdIndex, currBlock), this.oft.openFileArray[index].rwbuffer);
			clearBuffer(index);
		}

		ldiskObj.read_block(getAppropriateBlock(fdIndex, toPos), this.oft.openFileArray[index].rwbuffer);
		
		// Set current position to new position
		this.oft.openFileArray[index].position = pos;
		
		// return status
		return 1;
	}
	
	// list the names of all files and their lengths
	public int directory() {
		System.out.println("directory method called.\n");
		//local variables
		byte[] dir = new byte[this.b];
		char[] temp = new char[4];
		String name;
		int fileLength;
		int m = 0;
		
		//Read the directory file
		
		//For each entry, 
		// - find file descriptor
		// - print file name and file length
		for(int i = 7; i < 10; i++) {
			ldiskObj.read_block(i, dir);
			for(int j = 0; j < this.b; j=j+8) {
				if(dir[j] != -1) {
					//get symbolic name
					m = 0;
					for(int k = j; dir[k] != -1 && k < j+4; k++) {
						temp[m] = (char)dir[k];
						m++;
					}
					name = new String(temp);
					System.out.print(name);
							
					//get file length
					fileLength = fromByteArray(Arrays.copyOfRange(dir, j+4, j+8));
					System.out.println(" - " + fileLength);
				}
				
			}
		}
		
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
				System.out.println("\npositions: " + openFileArray[i].position);
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
