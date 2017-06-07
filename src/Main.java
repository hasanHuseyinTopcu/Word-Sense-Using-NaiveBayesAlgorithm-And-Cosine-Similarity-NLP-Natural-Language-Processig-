import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

	public static void main(String[] args) {
		
		String trainFileName 			= "train.txt";			/* file variable definitions for task1 */
		String vocabularyFileName 		= "vocabulary.txt";
		String testFileName 			= "test.txt";
		String outputFileNameForTask1 	= "task1Output.txt";
		
		String inputFileNameForTask2 	= "inputForTask2.txt";	/* file variable definitions for task2 */
		String outputFileNameForTask2 	= "task2Output.txt";

		ArrayList<String> listOfLineInTrainTextFile 		= porterStemmer(trainFileName);			/* apply Porter Stemmer to all task1's text file */
		ArrayList<String> listOfLineInVocabularyTextFile 	= porterStemmer(vocabularyFileName);	
		ArrayList<String> listOfLineInTestTextFile			= porterStemmer(testFileName);
		
		ArrayList<String> listOfLineInTask2InputTextFile 	= porterStemmer(inputFileNameForTask2);	/* apply Porter Stemmer to all task2's text file */
		
		task1(listOfLineInTrainTextFile, listOfLineInVocabularyTextFile, listOfLineInTestTextFile, outputFileNameForTask1);
		
		task2(listOfLineInTask2InputTextFile, outputFileNameForTask2);
	}
	
	public static void task1(ArrayList<String> listOfLineInTrainTextFile, ArrayList<String> listOfLineInVocabularyTextFile, ArrayList<String> listOfLineInTestTextFile, String outputFileNameForTask1){
		
		ArrayList<String> listOfWordInVocabulary = createVocabularyList(listOfLineInVocabularyTextFile);
		
		ArrayList<Document> listOfDocumentOfTrainData = createDocumentList(listOfLineInTrainTextFile, listOfWordInVocabulary);
		ArrayList<Document> listOfDocumentOfTestData = createDocumentList(listOfLineInTestTextFile, listOfWordInVocabulary);
		
		ArrayList<String> listOfResult = wordSenseDisambiguation(listOfDocumentOfTrainData, listOfWordInVocabulary, listOfDocumentOfTestData);
		
		writeListToFile(outputFileNameForTask1, listOfResult);
	}
	
	public static ArrayList<String> createVocabularyList(ArrayList<String> listOfLineInVocabularyTextFile){
		
		ArrayList<String> listOfVocabulary = new ArrayList<String>();
		
		for(String line : listOfLineInVocabularyTextFile){
			
			String[] words = line.split(",");
			
			for (String word : words)
				listOfVocabulary.add(word.trim());
		}
		
		return listOfVocabulary;
	}
	
	public static ArrayList<Document> createDocumentList(ArrayList<String> listOfLineInTextFile, ArrayList<String> listOfWordInVocabulary){
		
		ArrayList<Document> listOfDocument = new ArrayList<Document>();
		
		Document newDocument = null;
		
		for(String line : listOfLineInTextFile){
			
			if(line.matches("^[0-9]+$")){	//if line is sense reference number
				
				if(newDocument != null)
					listOfDocument.add(newDocument);
				
				newDocument = new Document();
				newDocument.setDocumentNumber(line);
			}
			else{	//if line is sentence
				
				String sentence = line;
				
				Pattern tagValuePattern = Pattern.compile("<tag(.)+>");
				
				Matcher m = tagValuePattern.matcher(sentence);
				
				if(m.find()){	//if sentence contain "tag"

					String tagPart = m.group(0);
					
					String wordSenseReferenceNumber = null;
					
					if(tagPart.indexOf('"') != -1)	//if tag has sense reference number
						wordSenseReferenceNumber = tagPart.substring(tagPart.indexOf('"')+1, tagPart.lastIndexOf('"'));
					
					newDocument.setClassReferenceNumber(wordSenseReferenceNumber);
				}
				
				sentence = sentence.replaceAll("[^A-Za-z]", " ").replaceAll(" ( )+", " ").trim();
				
				for(String wordInSentence : sentence.split(" ")){
				
					if(listOfWordInVocabulary.contains(wordInSentence))	//if word is in vocabulary
						newDocument.getListOfWord().add(wordInSentence);
				}
			}
		}
		
		return listOfDocument;
	}
	
	public static ArrayList<String> wordSenseDisambiguation(ArrayList<Document> listOfDocumentOfTrainData, ArrayList<String> listOfWordInVocabulary, ArrayList<Document> listOfDocumentOfTestData){
		
		ArrayList<String> listOfResult = new ArrayList<String>();
		
		for(Document document : listOfDocumentOfTestData){
			
			String result = findWordSenseUsingNaiveBayesAlgorithm(document, listOfDocumentOfTrainData, listOfWordInVocabulary);
			
			listOfResult.add(result);
		}
		
		return listOfResult;
	}
	
	public static String findWordSenseUsingNaiveBayesAlgorithm(Document documentOfTestData, ArrayList<Document> listOfDocumentOfTrainData, ArrayList<String> listOfWordInVocabulary){
		
		String result = documentOfTestData.getDocumentNumber()+" ";
		
		double maxProbability = 0.0;
		String classReferenceNumberOfMaxProbability = null;
		
		ArrayList<String> listOfUniqueClassReferenceNumberOfTrainData = new ArrayList<String>();
		
		for(Document document : listOfDocumentOfTrainData){
			
			if(!listOfUniqueClassReferenceNumberOfTrainData.contains(document.getClassReferenceNumber()))
				listOfUniqueClassReferenceNumberOfTrainData.add(document.getClassReferenceNumber());
		}
		
		for(String classReferenceNumber : listOfUniqueClassReferenceNumberOfTrainData){ //each class reference number
			
			double probability = calculateClassProbability(listOfDocumentOfTrainData, classReferenceNumber);

			for(String word : documentOfTestData.getListOfWord()){//each word in document
				
				probability *=  calculateProbabilityOfGeneratingWordFromClass(listOfDocumentOfTrainData, listOfWordInVocabulary, word, classReferenceNumber);
			}
			
			if(probability > maxProbability){
				maxProbability = probability;
				classReferenceNumberOfMaxProbability = classReferenceNumber;
			}
		}
		
		result += classReferenceNumberOfMaxProbability;
		
		return result;
	}
	
	public static double calculateClassProbability(ArrayList<Document> listOfDocumentOfTrainData, String classReferenceNumber){
		
		int countOfClassReferenceNumber = 0;
		
		for(Document document : listOfDocumentOfTrainData){
			
			if(document.getClassReferenceNumber().equals(classReferenceNumber))
				countOfClassReferenceNumber++;
		}
		
		return (double)countOfClassReferenceNumber / (double)listOfDocumentOfTrainData.size();
	}
	
	public static double calculateProbabilityOfGeneratingWordFromClass(ArrayList<Document> listOfDocumentOfTrainData, ArrayList<String> listOfWordInVocabulary, String word, String classReferenceNumber){
		
		int countOfWordInClass = 0, countOfTotalWordInClass = 0, countOfVocabulary = listOfWordInVocabulary.size();
		
		for(Document document : listOfDocumentOfTrainData){
			
			if(document.getClassReferenceNumber().equals(classReferenceNumber)){
				
				for(String string : document.getListOfWord()){

					if(word.equals(string))
						countOfWordInClass++;
					
					countOfTotalWordInClass++;							
				}
			}
		}
		
		return (double)( countOfWordInClass + 1 ) / (double)( countOfTotalWordInClass + countOfVocabulary );
	}
	
	public static void task2(ArrayList<String> listOfSentenceOfTask2, String outputFileNameForTask2){
		
		ArrayList<String> listOfResult = new ArrayList<String>();
		
		int windowSize = 3;
		
		String firstWord = null, secondWord = null;
		
		ArrayList<String> listOfSentenceOfFirstWord = new ArrayList<String>();
		ArrayList<String> listOfSentenceOfSecondWord = new ArrayList<String>();
		
		for(String line : listOfSentenceOfTask2){
			
			if( line.indexOf(" ") == -1 && line.lastIndexOf(':') == line.length()-1 ){ //if line is word
				
				if(firstWord == null)
					firstWord = line.replace(":", "");
				else
					secondWord = line.replace(":", "");								
			}
			else{	//if line is sentence
				
				if(secondWord == null && firstWord != null)
					listOfSentenceOfFirstWord.add(line.replaceAll("'s", " ").replaceAll("[^A-Za-z]", " ").replaceAll(" ( )+", " ").trim());
				else
					listOfSentenceOfSecondWord.add(line.replaceAll("'s", " ").replaceAll("[^A-Za-z]", " ").replaceAll(" ( )+", " ").trim());
			}
		}
		
		ArrayList<String> listOfWordOfFeatureVector = new ArrayList<String>();
				
		fillFeatureVectorList(listOfWordOfFeatureVector, listOfSentenceOfFirstWord, firstWord, windowSize);
		fillFeatureVectorList(listOfWordOfFeatureVector, listOfSentenceOfSecondWord, secondWord, windowSize);

		
		ArrayList<Integer> listOfValueOfFrequencyOnFeatureVectorForFirstWord = new ArrayList<Integer>();
		ArrayList<Integer> listOfValueOfFrequencyOnFeatureVectorForSecondWord = new ArrayList<Integer>();
		
		for(String word : listOfWordOfFeatureVector){
			
			int frequencyValueOnFeatureVectorForFirstWord = findFrequencyValueOnFeatureVector(word, listOfSentenceOfFirstWord, firstWord, windowSize);
			int frequencyValueOnFeatureVectorForSecondWord = findFrequencyValueOnFeatureVector(word, listOfSentenceOfSecondWord, secondWord, windowSize);
			
			listOfValueOfFrequencyOnFeatureVectorForFirstWord.add( frequencyValueOnFeatureVectorForFirstWord );
			listOfValueOfFrequencyOnFeatureVectorForSecondWord.add( frequencyValueOnFeatureVectorForSecondWord );	
		}
		
		double similarity = calculateCosineSimilarity(listOfWordOfFeatureVector, listOfValueOfFrequencyOnFeatureVectorForFirstWord, listOfValueOfFrequencyOnFeatureVectorForSecondWord);
		
		String result = "Cosine similarity value of between " + firstWord + " and " + secondWord + " : " + similarity;
		
		listOfResult.add(result);
		
		writeListToFile(outputFileNameForTask2, listOfResult);
	}
	
	public static ArrayList<String> fillFeatureVectorList(ArrayList<String> listOfWordOfFeatureVector, ArrayList<String> listOfSentence, String word, int windowSize){
		
		for(String sentence : listOfSentence){
			
			String[] words = sentence.split(" ");
			
			int indexOfWord = -1;
			
			for(int i=0; i<words.length; i++){
				
				if(words[i].equals(word))
					indexOfWord = i;
			}
			
			for(int i=1; i<=windowSize; i++){
				
				int leftIndex = indexOfWord - i;
				int rightIndex = indexOfWord + i;
				
				if(leftIndex >= 0 && listOfWordOfFeatureVector.contains(words[leftIndex]) == false)
					listOfWordOfFeatureVector.add(words[leftIndex]);
				
				if(rightIndex <= words.length-1 && listOfWordOfFeatureVector.contains(words[rightIndex]) == false)
					listOfWordOfFeatureVector.add(words[rightIndex]);
			}
		}

		return listOfWordOfFeatureVector;
	}
	
	public static int findFrequencyValueOnFeatureVector(String wordOfFeatureVector, ArrayList<String> listOfSentence, String word, int windowSize){
		
		int frequencyValueOnFeatureVectorForWord = 0;
		
		for(String sentence : listOfSentence){
			
			String[] words = sentence.split(" ");
			
			int indexOfWord = -1;
			
			for(int i=0; i<words.length; i++){
				if(words[i].equals(word))
					indexOfWord = i;
			}
			
			for(int i=1; i<=windowSize; i++){
				
				int leftIndex 	= indexOfWord - i;
				int rightIndex 	= indexOfWord + i;
				
				if(leftIndex >= 0 && words[leftIndex].equals(wordOfFeatureVector))
					frequencyValueOnFeatureVectorForWord++;
				
				if(rightIndex <= words.length-1 && words[rightIndex].equals(wordOfFeatureVector))
					frequencyValueOnFeatureVectorForWord++;
			}
			
			
		}
		
		return frequencyValueOnFeatureVectorForWord;
	}
	
	public static double calculateCosineSimilarity(ArrayList<String> listOfWordOfFeatureVector, ArrayList<Integer> listOfValueOfFrequencyOnFeatureVectorForFirstWord, ArrayList<Integer> listOfValueOfFrequencyOnFeatureVectorForSecondWord){
		
		double similarity;
		
		int sumOfProductTwoVector = 0, sumOfProdcutFirstVector = 0, sumOfProductSecondVector = 0;
		
		for(int i=0; i<listOfWordOfFeatureVector.size(); i++){
			
			sumOfProductTwoVector += listOfValueOfFrequencyOnFeatureVectorForFirstWord.get(i) * listOfValueOfFrequencyOnFeatureVectorForSecondWord.get(i);
			
			sumOfProdcutFirstVector += listOfValueOfFrequencyOnFeatureVectorForFirstWord.get(i) * listOfValueOfFrequencyOnFeatureVectorForFirstWord.get(i);
			
			sumOfProductSecondVector += listOfValueOfFrequencyOnFeatureVectorForSecondWord.get(i) * listOfValueOfFrequencyOnFeatureVectorForSecondWord.get(i);
		}
		
		similarity = (double)sumOfProductTwoVector / (Math.sqrt((double)sumOfProdcutFirstVector) * Math.sqrt((double)sumOfProductSecondVector));
		
		return similarity;
	}
	
	public static ArrayList<String> porterStemmer(String fileName){
		
		ArrayList<String> listOfLine = new ArrayList<String>();
		String line = "";
		
		char[] w = new char[501];
	    Stemmer s = new Stemmer();
	    
	    FileInputStream in = null;
	    
	    for (int i = 0; i < 1; i++){	    	
	    	try{	    		
		    	in = new FileInputStream(fileName);
		        try{	
		        	while(true){
		        		int ch = in.read();
		              	if (Character.isLetter((char) ch)){
		              		int j = 0;
		              		while(true){
		              			ch = Character.toLowerCase((char) ch);
		              			w[j] = (char) ch;
		              			if (j < 500) j++;
		              			ch = in.read();
		              			
		              			if (!Character.isLetter((char) ch)){
		              				
		              				/* to test add(char ch) */
		              				for (int c = 0; c < j; c++) s.add(w[c]);
		
				                    /* or, to test add(char[] w, int j) */
				                    /* s.add(w, j); */
		
				                    s.stem();
				                    {  
				                    	String u;
				                        /* and now, to test toString() : */
				                        u = s.toString();
				                        /* to test getResultBuffer(), getResultLength() : */
				                        /* u = new String(s.getResultBuffer(), 0, s.getResultLength()); */
				                        
				                        line += u.toString();
				                     }
				                    break;
		              			}
		              		}
		              	}
		         
		              	if(ch == '\n'){
		              		listOfLine.add(line);
		              		line = "";
		              	}
		              	else{
		              		line += String.valueOf((char)ch);
		              	}
		              	
		              	if (ch < 0) break;
		        	}
		        }
		        catch (IOException e){
		        	System.out.println("error reading " + fileName);
		            break;
		        }
	    	}
	    	catch (FileNotFoundException e){
	    		System.out.println("file " + fileName + " not found");
		        break;
	    	}
	    	finally{
	    		try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	}
	    }
	    
	    for(int i=0; i<listOfLine.size(); i++){			//remove blank line and trim line
	    	listOfLine.set(i, listOfLine.get(i).trim());
	    	if(listOfLine.get(i).length() == 0){
	    		listOfLine.remove(i);
	    		i--;
	    	}
	    }
	    
	    return listOfLine;
	}
	
	public static void writeListToFile(String outputFileName, ArrayList<String> listOfLine){
		
		BufferedWriter bw = null;
		FileWriter fw = null;

		try {
			
			fw = new FileWriter(outputFileName);
			bw = new BufferedWriter(fw);
			
			for (String line : listOfLine) {
				bw.write(line);
				bw.newLine();
			}

		}
		catch(IOException e){
			e.printStackTrace();
		}
		finally{
			
			try {						//close reader and bufferedReader
				bw.close();
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	

}
