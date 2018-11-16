 package lydamian.pkg;

import lydamian.pkg.*;

import java.io.*;
import java.util.Scanner;

public class Driver {
	public static boolean isNumeric(String str)
	{
	  return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
	}
	
	public static void shell() {
		System.out.println("shell started");
		//local variables
		FileSystem fileSystem = null;
		IOSystem ldisk = null;
		
		try {
			File inputFile = new File("C:\\Users\\lydam\\Desktop\\School\\cs143b\\Project1\\tempcases\\input6.txt").getAbsoluteFile();
			File outputFile = new File("C:\\Users\\lydam\\Desktop\\School\\cs143b\\Project1\\tempcases\\ouput6.txt").getAbsoluteFile();
			
			System.out.println(inputFile.toString());
			
			if( !inputFile.isFile())
			{
				System.out.println("the input file does not exist");
			}
			if(!outputFile.isFile()) {
				System.out.println("the output file does not exists");
			}
			Scanner fileReader = new Scanner(inputFile);
			FileWriter fileWriter = new FileWriter(outputFile);
			BufferedWriter out = new BufferedWriter(fileWriter);
			
			while(fileReader.hasNext()) {
				String command = fileReader.nextLine();
			
				//tokenize input
				String[] tokenizedCommand = command.split(" ");
				String commandName = tokenizedCommand[0];
				//decide what to do with input
				if(commandName.equals("cr")) {
					String name;
					//get Next Token, Error if not found		
					if(tokenizedCommand.length < 2) {
						fileWriter.write("error\r\n");
						out.flush();
					}
					else {
						name = tokenizedCommand[1];
						if(fileSystem.create(name) == 1) {
							fileWriter.write(name + " created\r\n");
							out.flush();
							
						}
						else {
							fileWriter.write("error\r\n");
							out.flush();
						}
					}
				}
				else if(commandName.equals("de")) {
					String name;
					//get Next Token, Error if not found		
					if(tokenizedCommand.length < 2) {
						fileWriter.write("Error, not enough arguments supplied\r\n");
					}
					else {
						name = tokenizedCommand[1];
						if(fileSystem.destroy(name) == 1) {
							fileWriter.write(name + " destroyed\r\n");
							out.flush();
						}
						else {
							fileWriter.write("error\r\n");
							out.flush();
						}
					}
				}
				else if(commandName.equals("op")) {
					String name;
					int index;
					//get Next Token, Error if not found		
					if(tokenizedCommand.length < 2) {
						fileWriter.write("Error, not enough arguments supplied\r\n");
					}
					else {
						name = tokenizedCommand[1];
						if((index = fileSystem.open(name)) != -1) {
							fileWriter.write(name + " opened " + index + "\r\n" );
							out.flush();
						}
						else {
							fileWriter.write("error\r\n");
							out.flush();
						}
					}
				}
				else if(commandName.equals("cl")) {
					int index;
					//get Next Token, Error if not found		
					if(tokenizedCommand.length < 2) {
						fileWriter.write("Error, not enough arguments supplied\r\n");
					}
					else {
						if(isNumeric(tokenizedCommand[1])) {
							index = Integer.parseInt(tokenizedCommand[1]);
							if(fileSystem.close(index) == -1) {
								fileWriter.write("error\r\n");
								out.flush();
							}
							else {
								fileWriter.write(index + " closed\r\n");
								out.flush();
							}
						}
						else {
							fileWriter.write("Invalid argument passed, please supply a file index\r\n");
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
						fileWriter.write("Error, not enough arguments supplied\r\n");
					}
					else {
						if(!isNumeric(tokenizedCommand[1]) || !isNumeric(tokenizedCommand[2])) {
							fileWriter.write("Error, invalid argument passed.\r\n");
						}
						else {
							index = Integer.parseInt(tokenizedCommand[1]);
							count = Integer.parseInt(tokenizedCommand[2]);
							
							if(fileSystem.read(index, mem_area, count) == -1) {
								fileWriter.write("error\r\n");
								out.flush();
							}
							else {
								for(int i = 0; mem_area[i] != -1; i++) {
									fileWriter.write((char)mem_area[i]);
								}
								fileWriter.write("\r\n");
								out.flush();
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
						fileWriter.write("Error, not enough arguments supplied\r\n");
					}
					else {
						index = Integer.parseInt(tokenizedCommand[1]);
						c = tokenizedCommand[2].charAt(0);
						count = Integer.parseInt(tokenizedCommand[3]);
						
						//error checking
						if(!isNumeric(tokenizedCommand[1]) || !(isNumeric(tokenizedCommand[3]))){
							fileWriter.write("Error in arguments supplied.\r\n");
						}
						else {
							if((numWritten = fileSystem.write(index, c, count)) == -1) {
								fileWriter.write("error\r\n");
								out.flush();
							}
							else {
								fileWriter.write(numWritten + " bytes written\r\n");
								out.flush();
							}
						}
					}
				}
				else if(commandName.equals("sk")) {
					int index;
					int pos;
					
					if(tokenizedCommand.length < 3) {
						fileWriter.write("Error\r\n");
					}
					else{
						index = Integer.parseInt(tokenizedCommand[1]);
						pos = Integer.parseInt(tokenizedCommand[2]);
						
						try {
							if(fileSystem.lseek(index,  pos) == -1) {
								fileWriter.write("error\r\n" );
								out.flush();
							}
							else { 
								fileWriter.write("position is " + pos+"\r\n");
								out.flush();
							}
						}
						catch(Exception e) {
							fileWriter.write("error");
							out.flush();
						}
						catch (Throwable t) {
							
						}

					}
				}
				else if(commandName.equals("dr")) {
					fileWriter.write(fileSystem.directory() + "\r\n");
					out.flush();
				}
				else if(commandName.equals("in")) {
					String textFile;
					
					if(tokenizedCommand.length == 1) {
						ldisk = new IOSystem(64,64);
						fileSystem = new FileSystem(64, 24, 3, ldisk, 64, 64);
						fileWriter.write("disk initialized\r\n");
						out.flush();
					}
					else {
						textFile = tokenizedCommand[1];
						ldisk = new IOSystem(64,64);
						fileSystem = new FileSystem(64, 24, 3, ldisk, 64, 64);
						
						try {
							if(ldisk.restoreLDisk(textFile) == 1) {
								fileWriter.write("disk restored\r\n");
								out.flush();
							}
							else {
								fileWriter.write("error\r\n");
								out.flush();
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				else if(commandName.equals("sv")) {
					String textFile;
					
					if(tokenizedCommand.length < 2) {
						fileWriter.write("Error, to few arguments to command\r\n");
					}
					else {
						textFile = tokenizedCommand[1];
						
						fileSystem.closeOFT();
						
						if(ldisk.saveLDisk(textFile) == -1) {
							fileWriter.write("error\r\n");
							out.flush();
						}
						else {
							fileWriter.write("disk saved\r\n");
							out.flush();
						}
					}
				}
				else if(commandName.toLowerCase().equals("exit")) {
					
				}
				else if(commandName.equals("did")) {
					int index = Integer.parseInt(tokenizedCommand[1]);
					ldisk.displayData(index);
				}
				else if(commandName.equals("dib")) {
					fileSystem.oft.displayOFT();
				}
				else {
					fileWriter.write("\r\n");
				}
			}
			
			fileReader.close();
			fileWriter.flush();
			fileWriter.close();


			
		}
		catch(IOException e){
			
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

		
		shell();
	}

}
