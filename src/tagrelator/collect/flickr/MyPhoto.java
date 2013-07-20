package tagrelator.collect.flickr;

import java.io.Serializable;


public class MyPhoto implements Serializable {
	
	private String url;
	private String id;
	private String usrId;
	private String[] tags;
	
	
	//////////////
	//Constructor
	public MyPhoto(String aUrl, String aId, String aUsrId ,String[] atags){
		try{
			this.url = aUrl;
		}
		catch(NullPointerException e){
			System.err.print("no url for myPhoto Object. set to empty string" );
			this.url = "";
		}
		try{
			this.id = aId;
		}
		catch(NullPointerException e){
			System.err.print("no id for myPhoto Object. set to empty string" );
			this.id = "";
		}
		try{
		
			this.tags = atags;
		}
		catch(NullPointerException e){
			System.err.print("tagarray empty for myPhoto Object. set to empty string" );
			this.tags = new String[1];
		}
		try{
			
			this.usrId = aUsrId;
		}
		catch(NullPointerException e){
			System.err.print("usrId empty for myPhoto Object. set to empty string" );
			this.usrId = new String("");
		}
	}
	
	////////////
	//getter
	
	public String getUrl(){
		return new String(this.url);
	} 
	
	public String getId(){
		return new String(id);
	}
	
	public String[] getTags(){
		return tags;
	}
	
	public Integer lastTagIndex(){
		return tags.length-1;
	}
}
