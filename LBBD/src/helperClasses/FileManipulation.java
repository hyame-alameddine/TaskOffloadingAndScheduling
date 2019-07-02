package helperClasses;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileManipulation {
	
	public File file;
	public String fileName;

	public FileManipulation (String fileName) throws IOException
	{
		this.fileName = fileName;
		this.file = new File(fileName);
		 
		// if file doesnt exists, then create it
		if (!this.file.exists()) 
		{
			this.file.createNewFile();
		}

	}
	
	/**
	 * This function write the specified content into a file
	 * 
	 * @param content
	 * @param fileName path for the file that will be created if it doesn't exists
	 */
	
	public void writeInFile(String content)
	{
		try 
		{			
			FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();
			fw.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
	}
}
