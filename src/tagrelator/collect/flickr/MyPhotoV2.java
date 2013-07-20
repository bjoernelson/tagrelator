/**
 * 
 */
package tagrelator.collect.flickr;

import java.io.Serializable;

/**
 *reimplementation of the MyPhoto class, to minimize memory need
 *
 */
public class MyPhotoV2 implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 220920121;
	private final char[] url;
	private final char[] id;
	private final char[] usrId;
	private final char[][] tags;
	
	
	//////////////
	//Constructor
	public MyPhotoV2(String aUrl, String aId, String aUsrId ,String[] atags){
		
		if(aUrl != null){
		
			this.url = new char[aUrl.length()]; 
			System.arraycopy(aUrl.toCharArray(), 0, this.url, 0, aUrl.length());		aUrl.toCharArray();
			
		}
		else{
			System.err.print("no Url for MyPhoto Object. risk of NullPointerExceptions!" );
			this.url = new char[0];
		}
		
		if(aId != null){
			
			this.id = new char[aId.length()]; 
			System.arraycopy(aUsrId.toCharArray(), 0, this.id, 0, aId.length());
		}
		else{
			System.err.print("no id for MyPhoto Object. risk of NullPointerExceptions!" );
			this.id = new char[0];
		}
		//process String down to char arrays, more compact in representation
		if(atags != null){
			
			char[][] chartags = new char[atags.length][];
			
			for(int i = 0; i<atags.length; i++){
				char[] tag = new char[atags[i].length()];
				System.arraycopy(atags[i].toCharArray(), 0, tag, 0, atags[i].length());
				chartags[i] = tag;
			}
			
			this.tags = chartags;
		}
		else{
			System.err.print("tagarray empty for myPhoto Object. risk of NullPointerExceptions!" );
			this.tags = new char[0][];
		}
		
		if(aUsrId != null){	
			this.usrId = new char[aUsrId.length()];
			System.arraycopy(aUsrId.toCharArray(), 0, this.usrId, 0, aUsrId.length());
		}
		else{
			System.err.print("usrId empty for myPhoto Object. risk of NullPointerExceptions!" );
			this.usrId = new char[0];
		}
	}
	
	//////////////////////
	//rewrite Constructor
	public MyPhotoV2(MyPhoto oldPhoto){
		this.id = new char[oldPhoto.getId().toCharArray().length]; 
		System.arraycopy(oldPhoto.getId().toCharArray(), 0, this.id, 0, id.length);
		this.url = new char[oldPhoto.getUrl().toCharArray().length];
		System.arraycopy(oldPhoto.getUrl().toCharArray(), 0, this.url, 0, this.url.length);
		this.usrId = new char[0];
		
		String[] atags = oldPhoto.getTags();
		
		char[][] chartags = new char[atags.length][];
		
		for(int i = 0; i<atags.length; i++){
			char[] tag = new char[atags[i].toCharArray().length];
			System.arraycopy(atags[i].toCharArray(), 0, tag, 0, tag.length);
			chartags[i] = tag;
		}
		
		this.tags = chartags;
	}
	
	////////////
	//getter
	public String getUrl(){
		return new String(this.url);
	} 
	
	public String getId(){
		return new String(id);
	}
	
	//reprocess to String
	public String[] getTags(){
		String[] retTags = new String[tags.length];
		
		for(int i=0; i<tags.length; i++){
			retTags[i] = new String(tags[i]);
		}
		return retTags;
	}
	
	public Integer lastTagIndex(){
		return tags.length-1;
	}
	
	public String getUsrId() {
		return new String(usrId);
	}
}
