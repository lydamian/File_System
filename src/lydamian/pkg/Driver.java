package lydamian.pkg;

import lydamian.pkg.*;

public class Driver {

	public static void main(String[] args) {
		System.out.println("main function called...\n");
		/*
		 * 	ldisk: 64 blocks
			block = 64B = 16 integers
			block 0 holds bitmap: 64 bits (one per block) = 2 integers
			descriptor: 4 integers
			file length (1 int)
			3 block #s
			# of descriptors: 24 = 6 blocks
			descriptor 0 describes the directory
			each directory entry: 2 integers
			file name: maximum 4 chars, no extension (1 int)
			descriptor index: 1 integer
			ldisk can be saved into a text file at any point with the sv command
			ldisk can be restored from a previously saved text file ising the in command
			a new empty ldisk is created if no saved file is given
			directory is opened automatically with in command (OFT index = 0) 
			OFT has 4 entries: directory plus up to 3 other open files
			all files (including directory) must close with sv command
			destroying an open file is ok (not an error)
			every open command must use the first available slot in the OFT (smallest index)
		 */
		
		// local variables
		int l = 64; //64 blocks
		int b = 64; //64 bytes per block;
		int bitmapLength = 8; //64 bytes reserved for bitmap but only 8 bytes needed.
		int k; // reserved memory location.
		
		
		//Initialize Ldisk
		IOSystem ldisk = new IOSystem(64,64);
		ldisk.testIOSystem();
		char[] main_memory =  {'h','e','l','l','o'};
		ldisk.write_block(1, main_memory);
		ldisk.displayData(1);
		
		//Initializing FileSystem.java
		FileSystem fileSystem = new FileSystem(64, 24, 3);
		

		
		

	}

}
