package tagrelator.collect.anc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import javax.xml.namespace.*;
import javax.xml.stream.*;
import javax.xml.stream.events.*;


/**preprocesses the open american national corpus to an Array of {@link ANCdocument} and serializes them to files.
 * the {@link ANCdocument} class itself contains a String Array of the words of 
 * the document in the corpus.   
 * */
public class ANCprocessor {
	/**base directory of the corpus all subdirectories are cycled for
	 * corpus files
	 * */
	private File baseDir;
	public final static String DefBaseDir = "F:\\OANC_GrAF\\OANC-GrAF\\data\\written_1";//
	/**the end of the filenames the should be processed have to match this String
	 * */
	private String fileNameSuff;
	public final static String DefFileNameSuff = ".txt";
	
	/**Prefix for the processed corpus files that are written
	 * Pattern will be Prefix Number .obj 
	 * */
	String outFilePrefix;
	public final static String DefOutFilePrefix = "corpus";
	/**output directory
	 * */
	String outDir;
	public final static String DefOutDir = "ANCcorpus";
	
	//////////////////
	//Constructor
	public ANCprocessor(String aBaseDir, String afileNameSuff, String aOutFilePrefix, String aOutDir){
		
		if(!(aBaseDir.equals("")||aBaseDir==null)){
			this.baseDir = new File(aBaseDir);
		}
		else{
			this.baseDir = new File(DefBaseDir);
		}
		
		if(!(afileNameSuff.equals("")||afileNameSuff==null)){
			this.fileNameSuff = new String(afileNameSuff);
		}
		else{
			this.fileNameSuff = new String(DefFileNameSuff);
		}
		
		if(!(aOutFilePrefix.equals("")||aOutFilePrefix==null)){
			this.outFilePrefix = new String(aOutFilePrefix);
		}
		else{
			this.outFilePrefix = new String(DefOutFilePrefix);
		}
		
		if(!(aOutDir.equals("")||aOutDir==null)){
			this.outDir = new String(aOutDir);
		}
		else{
			this.outDir = new String(DefOutDir);
		}
		
		
		
	}
	//default Constructor
	public ANCprocessor(){
		this.baseDir = new File(DefBaseDir);
		this.fileNameSuff = new String(DefFileNameSuff);
		this.outFilePrefix = new String(DefOutFilePrefix);
		this.outDir = new String(DefOutDir);
	}
	

	/**driver method to process the ANCcorpus .txt files
	 * potentially able to process other corpora given as plain text files
	 * without annotations. at your own risk
	 * */
	public void process(){
		
		//find all files to compute
		ArrayList<File> files = searchFile( baseDir, fileNameSuff);
		//this Array is not initialised,
		Integer docArrSize = 300;
		if(docArrSize>files.size()){
			docArrSize = files.size();
		}
		
		//arrays gonna be written out
		ANCdocument[] docs = new ANCdocument[docArrSize];
		Integer arrInd = 0;
		Integer fileInd = 1;
		
		System.out.println("found "+files.size()+" files\nbegin processing");
		//iterate over all files and read them in
		
		
		//for(int i = 0; i < 3; i++){
		for(int i = 0; i < files.size(); i++){
			//cleaned up words come here
			ArrayList<String> wordList = new ArrayList<String>();
		
			try{
				//open file
				FileReader fR = new FileReader(files.get(i));
				BufferedReader br = new BufferedReader(fR);
				//read first line
				String aLine = br.readLine();
				
				
				while(!(aLine==null)){
					//remove some unwanted stuff
					aLine = cleanLine(aLine);
					
					String[] elmts = aLine.split(" ");
					
					for(int a=0; a<elmts.length; a++){
						String elmt = elmts[a];
						elmt = elmt.replace(" ", "");
						//filter out words we dont want
						if(filter(elmt)){
							//System.out.print(elmts[a]+" ");
							wordList.add(elmt);
						}						
					}
					aLine = br.readLine();
					//System.out.println("*");
				}
				
				//create a document and put it in the array
				//as long as array is not filled up
				if(arrInd < docArrSize){
					docs[arrInd] = new ANCdocument(wordList.toArray(new String[wordList.size()]));
					arrInd++;
				}
				else{
					//write out the object
					writeDocs(docs, fileInd);
					
					if(!((docArrSize * (fileInd+1))<=files.size())){
						docArrSize = files.size() - (docArrSize * fileInd);
					}
					
					docs = new ANCdocument[docArrSize];
					//we are here because doc didnt fit in the array before
					docs[0] = new ANCdocument(wordList.toArray(new String[wordList.size()]));
					//index for the next file
					fileInd++;
					//reset array index
					arrInd = 1;
				}
				
				if(i == (files.size()-1)){
					writeDocs(docs, fileInd);
					}
				}
				catch(FileNotFoundException e){
					e.printStackTrace();
				}
				catch(IOException e){
					e.printStackTrace();
				}
			
			
		}//end for
		System.out.println("...done");
				
	}
	
	public String cleanLine(String dirty){
		dirty = dirty.toLowerCase();
		dirty = dirty.replace("\t", "");
		dirty = dirty.replace(",", "");
		dirty = dirty.replace(".", "");
		dirty = dirty.replace("?", "");
		dirty = dirty.replace("!", "");
		dirty = dirty.replace("(", "");
		dirty = dirty.replace(":", "");
		dirty = dirty.replace("--", "-");
		dirty = dirty.replace(";", "");
		dirty = dirty.replace("%", "");
		dirty = dirty.replace("~", "");
		dirty = dirty.replace("__","");
		dirty = dirty.replace("_","");
		String clean = dirty.replace(")", "");
		
		return clean;
	}
	
	/**some test on supposed words which throw them out
	 * */
	private boolean filter(String testObject){
		
		if(testObject==null){
			return false;
		}
		
		if(testObject.contains("FIELD")){
			return false;
		}
		if(testObject.equals("")){
			return false;
		}
		
		return true;
	}
	
	private void writeDocs(ANCdocument[] theDocs, Integer fileindex){
		
		//System.out.print(theDocs.length);
		String path = outDir+File.separatorChar+outFilePrefix+Integer.toString(fileindex)+".obj";
		//System.out.print("\nwriting processed Corpus to "+path);
		//System.out.print(".");
		ObjectOutputStream oos = null;
		FileOutputStream fos = null;
		
		
		try {
		  fos = new FileOutputStream(path);
		  oos = new ObjectOutputStream(fos);
		  
		  oos.writeObject(theDocs);
		  
		  oos.close();
		  fos.close();
		  
		  //System.out.println(" ...done");
		}
		catch (IOException e) {
			System.err.println("writing object failed");
			e.printStackTrace();
		}
		finally {
		  if (oos != null) try { oos.close(); } catch (IOException e) {}
		  if (fos != null) try { fos.close(); } catch (IOException e) {}
		}
	}
	
	private void processDepr(){
		//find all files to compute
		ArrayList<File> files = searchFile( baseDir, "hepple.xml");
		//this Array is not initialised
		//ANCdocument[] docs = new ANCdocument[files.size()];
		
		System.out.println("found "+files.size()+" files");
		//iterate over all files and read them in
		
		int i = 0;
		//for(int i = 0; i < files.size(); i++){
			try{
				//the file to process
				InputStream in = new FileInputStream(files.get(i));
				// new XMLInputFactory
				XMLInputFactory inputFactory = XMLInputFactory.newInstance();
				//XML Eventreader
				XMLEventReader evReader = inputFactory.createXMLEventReader(in);
				//these Elements contain the words of the document
				javax.xml.namespace.QName aQ = new QName("http://www.xces.org/ns/GrAF/1.0/", "f");
				
				//cycle through the document
				while(evReader.hasNext()){
					XMLEvent event = evReader.nextEvent();
					
					//System.out.print(event.getEventType());
					
					
					if(event.isStartElement()&& event.asStartElement().getName().equals(aQ)){
							
							Attribute nameAttr = event.asStartElement().getAttributeByName(new QName("name"));
							
							if(nameAttr.getValue().equalsIgnoreCase("base")){
							
								Attribute valAttr = event.asStartElement().getAttributeByName(new QName("value"));
							
								//System.out.println("-"+attr.getValue()+"-"+bQ.getNamespaceURI()+"-"+bQ.getLocalPart());
								System.out.println("-"+valAttr.getValue()+"-");
							}
					}
					
				}
				
			}
			catch(FileNotFoundException e){
				e.printStackTrace();
			}
			catch(XMLStreamException e){
				e.printStackTrace();
			}
			
		//}//end for
		
	}
	
	
	
	/**copied from http://www.java-forum.org/allgemeines/33129-verzeichnisse-durchsuchen-bearbeiten-auslesen.html
	 * and modiefied
	 * */
	public ArrayList<File> searchFile(File dir, String find) {

		File[] files = dir.listFiles();
		ArrayList<File> matches = new ArrayList<File> ();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				if (files[i].getName().endsWith(find) ) {
					
					matches.add(files[i]);
				}
				if (files[i].isDirectory()) {
					matches.addAll(searchFile(files[i], find));
				}
			}
		}
		return matches;
	}
	
}
