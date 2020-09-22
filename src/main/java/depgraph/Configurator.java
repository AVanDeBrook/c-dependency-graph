package depgraph;

import java.io.File;

public class Configurator {
	String nameOfFile;
	String nameOfDirectory;

	public Configurator() {
		
	}
	public void manageCmdLineArguments(String [] args){
		if (args.length != 0) {
			for (String s : args) {
				System.out.println(s);
			}
		}
		if(args.length == 0){
			System.out.println("No arguments passed.");
		}
		if(args.length !=0){
			System.out.println("Processing arguments now ... \n");

			for(int i = 0; i < args.length; i++ ){
				if(args[i].contains("-h")){
					help();
				}
				else if(args[i].contains("-s")){
					i++;
					processSingleFile(args[i]);
					System.out.println("Name of file passed to the program: " + args[i]);
				}else if(args[i].contains("-d")){
					i++;
					processDirectory(args[i]);
					System.out.println("Path of directory passed to the program : " + args[i]);
				}else{
					System.out.println("Could not interpret arguments");
				}
			}

		} 
	}
	private void help(){
		System.out.println("Welcome to the C Dependency Graph Tool.\n");
		System.out.println("Command line argument syntax is as follows:");
		System.out.println("gradle run --args=\" [arguments] \"\n");
		System.out.println("Possible arguments include -h for help,");
		System.out.println("-s for processing a single file,");		
		System.out.println("-d for processing a directory.");
	}

	private void processSingleFile(String fileName){

		File singleFile = new File(fileName);
		if(singleFile.isFile()){
			nameOfFile = fileName;
		}else{
			System.out.println("File name provided cannot resolve to a file");
		}

	}
	private void processDirectory(String directoryName){
		File singleDirectory = new File(directoryName);
		if(singleDirectory.isDirectory()){
			nameOfDirectory = directoryName;
		}else{
			System.out.println("Directory name provided cannot resolve to a directory");
		}

	}
	// getters 
	public String getFileName(){
		return nameOfFile;
	}

	public String getDirectoryName(){
		return nameOfDirectory;
	}



}
