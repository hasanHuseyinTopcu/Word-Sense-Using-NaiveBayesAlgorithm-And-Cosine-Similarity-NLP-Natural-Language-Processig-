import java.util.ArrayList;


public class Document {
	
	private String documentNumber;
	private ArrayList<String> listOfWord = new ArrayList<String>();
	private String classReferenceNumber;
	
	public String getDocumentNumber() {
		return documentNumber;
	}
	public void setDocumentNumber(String documentNumber) {
		this.documentNumber = documentNumber;
	}
	public ArrayList<String> getListOfWord() {
		return listOfWord;
	}
	public void setListOfWord(ArrayList<String> listOfWord) {
		this.listOfWord = listOfWord;
	}
	public String getClassReferenceNumber() {
		return classReferenceNumber;
	}
	public void setClassReferenceNumber(String classReferenceNumber) {
		this.classReferenceNumber = classReferenceNumber;
	}
	
}
