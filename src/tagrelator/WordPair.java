package tagrelator;
/**simple holder class to store a pair of words of which the PMI/SOC-PMI should be computed and a possibly empty human rating of the similarity of the two words 
 * */
public class WordPair {
	private final String wordA;
	private final String wordB;
	private final Double rating;
	
	/**PMI value for the word Pair, ! init to null
	 * */
	private Double pmi;
	/**computed similarity value, ! init to null 
	 * */
	private Double sim; 
	/**SOC-PMI value with wordA as first word, ! init to null
	 * */
	private Double socpmiA;
	/**the computed beta value when when wordA was first word ! init to null 
	 * */
	private Double betaA;
	/**the actual length of the context word list that was used, PMI>0 is the contraint 
	 * */
	private Integer actBetaA;
	/**SOC-PMI value with wordB as first word, ! init to null
	 * */
	private Double socpmiB;
	/**the computed beta value when when wordB was first word ! init to null 
	 * */
	private Double betaB;
	/**the actual length of the context word list that was used, PMI>0 is the contraint 
	 * */
	private Integer actBetaB;

	
		
	//////////////
	//Constructor
	/**@param aWordA String holding first word of the pair
	 * @param aWordB String holding second word of the pair
	 * @param aRating Double that holds a rating of similarity of the two words 
	 * */
	public WordPair(String aWordA, String aWordB, Double aRating) {
		this.wordA = new String(aWordA);
		this.wordB = new String(aWordB);
		this.rating = new Double(aRating);
		this.pmi = null;
		this.sim = null;
		this.socpmiA = null;
		this.socpmiB = null;
		
		
		
	}
	/**@param aWordA String holding first word of the pair
	 * @param aWordB String holding second word of the pair
	 * rating var is initialised to 0.0
	 * */
	public WordPair(String aWordA, String aWordB) {
		this.wordA = new String(aWordA);
		this.wordB = new String(aWordB);
		this.rating = new Double(0.0);
		this.pmi = null;
		this.sim = null;
		this.socpmiA = null;
		this.socpmiB = null;
	}
	
	////////////
	//getter
	public String getWordA() {
		return wordA;
	}
	
	public String getWordB() {
		return wordB;	
	}
	public Double getRating() {
		return rating;
	}
	public Double getSim() {
		return sim;
	}
	public Double getPmi() {
		return pmi;
	}
	public Double getSocpmiA() {
		return socpmiA;
	}
	public Double getSocpmiB() {
		return socpmiB;
	}
	public Double getBetaA() {
		return betaA;
	}
	public Integer getActBetaA() {
		return actBetaA;
	}
	public Double getBetaB() {
		return betaB;
	}
	public Integer getActBetaB() {
		return actBetaB;
	}
	///////////////
	//tests
	public boolean isSetsim(){
		if(sim==null){
			return false;
		}
		return true;
	}
	public boolean isSetpmi(){
		if(pmi==null){
			return false;
		}
		return true;
	}
	public boolean isSetsocpmiA(){
		if(socpmiA==null){
			return false;
		}
		return true;
	}
	
	public boolean isSetsocpmiB(){
		if(socpmiB==null){
			return false;
		}
		return true;
	}
	public boolean isSetbetaA(){
		if(betaA==null){
			return false;
		}
		return true;
	}
	public boolean isSetBetaB(){
		if(betaB==null){
			return false;
		}
		return true;
	}
	public boolean isSetActBetaA(){
		if(actBetaA==null){
			return false;
		}
		return true;
	}
	public boolean isSetActBetaB(){
		if(actBetaB==null){
			return false;
		}
		return true;
	}
	
	/////////////////////
	//setter
	public void setPmi(Double pmi) {
		this.pmi = pmi;
	}
	public void setSim(Double sim) {
		this.sim = sim;
	};
	public void setSocpmiA(Double socpmiA) {
		this.socpmiA = socpmiA;
	};
	public void setSocpmiB(Double socpmiB) {
		this.socpmiB = socpmiB;
	}
	public void setBetaA(Double betaA) {
		this.betaA = betaA;
	}
	public void setActBetaA(Integer actBetaA) {
		this.actBetaA = actBetaA;
	}
	public void setBetaB(Double betaB) {
		this.betaB = betaB;
	}
	public void setActBetaB(Integer actBetaB) {
		this.actBetaB = actBetaB;
	}
}
