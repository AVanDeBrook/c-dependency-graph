package depgraph;

import java.io.File;
import java.nio.file.Files;

public class Configurator {
	String nameOfFile;
	String nameOfDirectory;

	public Configurator() {
	
	}
	public void manageCmdLineArguments(String [] args){
	
		if(args.length == 0){
			System.out.println("No arguments passed.");
		}
		if(args.length !=0){
			System.out.println("Processing arguments now ... \n");

			for(int i = 0; i < args.length; i++ ){
				if( (args[i].charAt(0) == '-') && (args[i].length() == 2) ){
					switch(args[i].charAt(1)){
						case 'h' : this.help();
								   break;
						case 's' : //Logging purposes: System.out.println("Name of file passed to the program: " + args[i]);
								   this.processSingleFile(args[++i]);
								   break;
						case 'd' : //Logging purposes: System.out.println("Path of directory passed to the program : " + args[i]);
								   this.processDirectory(args[++i]);
						default:   System.out.println("Could not interpret arguments. Run gradle run --args=\" -h \"\n");
								   break;

					}
				}
			}		

		} 
	}
	private void help(){
		System.out.println("Welcome to the C Dependency Graph Tool.\n");
		System.out.println("Command line argument syntax is as follows:");
		System.out.println("gradle run --args=\" -[options] [arguments] \"\n");
		System.out.println("Possible arguments include -h for help,");
		System.out.println("-s for processing a single file,");		
		System.out.println("-d for processing a directory.\n");
		System.out.println("If having problems providing a path to a file or directory, remember that the root directroy is c-dependecy-graph");
	}

	//source directory is SeniorDesign\c-dependency-graph
	private void processSingleFile(String fileName){

		File singleFile = new File(fileName);
		
		if(singleFile.isFile()){
			this.nameOfFile = fileName;
		}else{
			System.out.println("File name provided cannot resolve to a file");
		}
		
	}
	private void processDirectory(String directoryName){
		File singleDirectory = new File(directoryName);
		if(singleDirectory.isDirectory()){
			this.nameOfDirectory = directoryName;
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
