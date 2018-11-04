 package lydamian.pkg;

import lydamian.pkg.*;
import java.util.Scanner;

public class Driver {
	public static boolean isNumeric(String str)
	{
	  return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
	}
	
	public static void shell(FileSystem fileSystem, IOSystem ldisk) {
		//local variables
		boolean bVal = true;
		Scanner in = new Scanner(System.in);
		
		while(bVal) {
			//get input
			String command = in.nextLine();
			
			//tokenize input
			String[] tokenizedCommand = command.split(" ");
			String commandName = tokenizedCommand[0];
			
			//decide what to do with input
			if(commandName.equals("cr")) {
				String name;
				//get Next Token, Error if not found		
				if(tokenizedCommand.length < 2) {
					System.out.println("Error, not enough arguments supplied");
				}
				else {
					name = tokenizedCommand[1];
					if(fileSystem.create(name) == 1) {
						System.out.println(name + " created");
					}
					else {
						System.out.println("Error creating file");
					}
				}
			}
			else if(commandName.equals("de")) {
				String name;
				//get Next Token, Error if not found		
				if(tokenizedCommand.length < 2) {
					System.out.println("Error, not enough arguments supplied");
				}
				else {
					name = tokenizedCommand[1];
					if(fileSystem.destroy(name) == 1) {
						System.out.println(name + " destroyed");
					}
					else {
						System.out.println("Error destroying file");
					}
				}
			}
			else if(commandName.equals("op")) {
				String name;
				int index;
				//get Next Token, Error if not found		
				if(tokenizedCommand.length < 2) {
					System.out.println("Error, not enough arguments supplied");
				}
				else {
					name = tokenizedCommand[1];
					if((index = fileSystem.open(name)) != -1) {
						System.out.println(name + " opened " + index );
					}
					else {
						System.out.println("Error opening file");
					}
				}
			}
			else if(commandName.equals("cl")) {
				int index;
				//get Next Token, Error if not found		
				if(tokenizedCommand.length < 2) {
					System.out.println("Error, not enough arguments supplied");
				}
				else {
					if(isNumeric(tokenizedCommand[1])) {
						index = Integer.parseInt(tokenizedCommand[1]);
						if(fileSystem.close(index) == -1) {
							System.out.println("Error closing file");
						}
						else {
							System.out.println(index + " closed");
						}
					}
					else {
						System.out.println("Invalid argument passed, please supply a file index");
					}

				}
			}
			else if(commandName.equals("rd")) {
				int index;
				int count;
				byte[] mem_area = new byte[64*3];
				for(int i = 0; i < 64*3; i++) {
					mem_area[i] = -1;
				}
				
				//get Next Token, Error if not found		
				if(tokenizedCommand.length < 3) {
					System.out.println("Error, not enough arguments supplied");
				}
				else {
					if(!isNumeric(tokenizedCommand[1]) || !isNumeric(tokenizedCommand[2])) {
						System.out.println("Error, invalid argument passed.");
					}
					else {
						index = Integer.parseInt(tokenizedCommand[1]);
						count = Integer.parseInt(tokenizedCommand[2]);
						
						if(fileSystem.read(index, mem_area, count) == -1) {
							System.out.println("Error reading from file");
						}
						else {
							for(int i = 0; mem_area[i] != -1; i++) {
								System.out.println((char)mem_area[i]);
							}
						}
						
					}

				}
			}
			else if(commandName.equals("wr")) {
				int index;
				int count;
				char c;
				
				int numWritten = 0;
				
				//get Next Token, Error if not found		
				if(tokenizedCommand.length < 4) {
					System.out.println("Error, not enough arguments supplied");
				}
				else {
					index = Integer.parseInt(tokenizedCommand[1]);
					c = tokenizedCommand[2].charAt(0);
					count = Integer.parseInt(tokenizedCommand[3]);
					
					//error checking
					if(!isNumeric(tokenizedCommand[1]) || !(isNumeric(tokenizedCommand[3]))){
						System.out.println("Error in arguments supplied.");
					}
					else {
						if((numWritten = fileSystem.write(index, c, count)) == -1) {
							System.out.println("Error writing to index: " + index);
						}
						else {
							System.out.println(numWritten + " bytes written");
						}
					}
				}
			}
			else if(commandName.equals("sk")) {
				int index;
				int pos;
				
				if(tokenizedCommand.length < 2) {
					System.out.println("Error, to few arguments to command sk");
				}
				else{
					index = Integer.parseInt(tokenizedCommand[1]);
					pos = Integer.parseInt(tokenizedCommand[2]);
					
					if(fileSystem.lseek(index,  pos) == -1) {
						System.out.println("Error seeking file " );
					}
					else { 
						System.out.println("position is " + pos);
					}
				}
			}
			else if(commandName.equals("dr")) {
				fileSystem.directory();
			}
			else if(commandName.equals("in")) {
				String textFile;
				
				if(tokenizedCommand.length < 2) {
					System.out.println("Error, to few arguments supplied");
				}
				else {
					textFile = tokenizedCommand[1];
					
					if(ldisk.restoreLDisk(textFile) == 0) {
						System.out.println("disk initialized");
					}
					else {
						System.out.println("disk restored");
					}
				}
			}
			else if(commandName.equals("sv")) {
				String textFile;
				
				if(tokenizedCommand.length < 2) {
					System.out.println("Error, to few arguments to command");
				}
				else {
					textFile = tokenizedCommand[1];
					
					if(ldisk.saveLDisk(textFile) == -1) {
						System.out.println("Error, saving ldisk");
					}
					else {
						System.out.println("disk saved");
					}
				}
			}
			else if(commandName.toLowerCase().equals("exit")) {
				bVal = false;
			}
			else if(commandName.equals("did")) {
				int index = Integer.parseInt(tokenizedCommand[1]);
				ldisk.displayData(index);
			}
			else if(commandName.equals("dib")) {
				fileSystem.oft.displayOFT();
			}
			else {
				System.out.println("Error, command not found");
			}
		}
	}
	
	
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
		boolean bVal = true;
		char mem_area = 'c';
		
		
		//Initialize Ldisk
		IOSystem ldisk = new IOSystem(64,64);
		//ldisk.testIOSystem();
		
		//Initializing FileSystem.java
		FileSystem fileSystem = new FileSystem(64, 24, 3, ldisk, l, b);
		
		//System.out.println("------");
		
		//ldisk.displayData(1);
		
		//System.out.println("------");
		
		//Test create/destroy
		//fileSystem.create("foo1");
		//fileSystem.create("fo2");
		//fileSystem.create("f3");
		
		//System.out.println("-----");
		//ldisk.displayData(7);
		//ldisk.displayData(1);
		
		//fileSystem.destroy("f3");
		//fileSystem.destroy("fo2");
		
		//ldisk.displayData(7);
		//System.out.println("----");
		//ldisk.displayData(1);
		
		//Test OFT
		//int oftIndex = fileSystem.open("foo1");
		//fileSystem.close(oftIndex);
		//System.out.println("--------_____");
		//ldisk.displayData(1);
		
		//oftIndex = fileSystem.open("foo1");
		//System.out.println("oftIndex is:  " + oftIndex);
		//fileSystem.write(oftIndex, mem_area, 66);
		//fileSystem.oft.displayOFT();
		//fileSystem.close(oftIndex);
		
		//System.out.println("WHAT>>>>>>>");
		//ldisk.displayData(10);
		//System.out.println(" asdfasdf ");
		//ldisk.displayData(11);
		//System.out.println(" asdfasdf ");
		//ldisk.displayData(12);
		
		//Test Reading Writing Seeking
		//fileSystem.write(oftIndex, mem_area, 5);
		//fileSystem.close(oftIndex);

		//ldisk.displayData(11);

		//System.out.println("---------");
		
		//ldisk.displayData(12);
		
		//System.out.println("---------");
		
		
		//Test Directory
		
		shell(fileSystem, ldisk);
	}

}
