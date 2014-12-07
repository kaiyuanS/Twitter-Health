package health;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

import de.daslaboratorium.machinelearning.classifier.BayesClassifier;
import de.daslaboratorium.machinelearning.classifier.Classifier;
/**
 * 
 * @author Kaiyuan Shi
 *
 */
public class NBClassifier {
	
	private static final String MY_STOPWORDS = "stopWords.txt";
	private static final String MY_LEARNING_TWEETS = "learningData.txt";
	private static final int MAX_LEARNING_DATA = 5000;
	
	private Set<String> myStopwords = new HashSet<String>();
	
	private List<String> myRelevantTweets = new ArrayList<String>();
	private List<String> myNonRelevantTweets = new ArrayList<String>();

	private List<String> myRelevantWords = new ArrayList<String>();
	private List<String> myNonRelevantWords = new ArrayList<String>();
	
	private static Classifier<String, String> myBayes = new BayesClassifier<String, String>();
	
	private static int myTcounter = 0;
	private static int myR = 0;
	private static int myN = 0;

	public void learn() {
		myBayes.setMemoryCapacity(10000);
		readInput();
		learning();

	}
	
	public void report() {
		System.out.println("===================================\n"
				+ "Learning Report:");
		System.out.println("Total tweets learned:           "
				+ myTcounter);
		System.out.println("Relevant tweets learnded:       "
				+ myR);
		System.out.println("Non Relevant tweets learnded:   "
				+ myN);
		System.out.println("Percentage of relevant:         "
				+ ((double)(myR) / (myR + myN)));
		System.out.println("\nEnd of Learning report\n"
				+ "===================================\n");
	}
	
	public String getRelevant(String aTweet) {
		return myBayes.classify(modifyWord(aTweet)).getCategory();
		
	}
	
	public void learn(String aRelevant, List<String> aTweet) {
		if (!aRelevant.equals("R") && !aRelevant.equals("N"))
			throw new IllegalArgumentException("Wrong category: " + aRelevant);
		myBayes.learn(aRelevant, aTweet);
		
		StringBuilder temp = new StringBuilder();
		for (String eachWord: aTweet)
			temp.append(eachWord + " ");
		
		if (aRelevant.equals("R")) {
			myRelevantTweets.add(temp.toString());
		} else {
			myNonRelevantTweets.add(temp.toString());
		}
		clearMemory();
	}
	
	public void learn(String aRelevant, String aTweet) {
		if (!aRelevant.equals("R") && !aRelevant.equals("N"))
			throw new IllegalArgumentException("Wrong category: " + aRelevant);
		myBayes.learn(aRelevant, modifyWord(aTweet));
		if (aRelevant.equals("R")) {
			myRelevantTweets.add(aTweet);
		} else {
			myNonRelevantTweets.add(aTweet);
		}
		clearMemory();
	}
	
	private void readInput() {
		String line = null;
		Scanner scanner = null;
        try {
            scanner = new Scanner(new FileInputStream(MY_STOPWORDS));
            while (scanner.hasNextLine()) {
            	line = scanner.nextLine();
            	myStopwords.add(line.toLowerCase());
            }
            
            scanner = new Scanner(new FileInputStream(MY_LEARNING_TWEETS));
            while (scanner.hasNextLine()) {
            	line = scanner.nextLine();
            	
            	if (line.length() == 0)
            		continue;
            	if (line.charAt(0) != '['
            			|| (line.charAt(1) != 'R' && line.charAt(1) != 'N')
            			|| line.charAt(2) != ']') {
            		System.out.println("Wrong learning format: " + line.substring(0,3));
            		continue;
            	}
            	/*
            	 * learning Tweets format:
            	 * format: [X/N]Tweets String
            	 * index:  01/123............
            	 */
            	myTcounter++;
            	
            	String topic = Character.valueOf(line.charAt(1)).toString().toUpperCase();
            	String tweet = line.substring(3);// tweets start from char @ 3
            	
            	
        		if (topic.equals("R")) {
        			myRelevantTweets.add(tweet);
         			myRelevantWords.addAll(modifyWord(tweet));
         			myR++;
        		} else if (topic.equals("N")) {
        			myNonRelevantTweets.add(tweet);
        			myNonRelevantWords.addAll(modifyWord(tweet));
        			myN++;
        		}
            	
            }
            
            
        } catch (final NoSuchElementException ex) {
            System.out.println("Input folder not found: " + ex.getMessage());
        } catch (final FileNotFoundException ex) {
            System.out.println("Input file not found: " + ex.getMessage());
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

	}
	
	public List<String> getRelevantTweets() {
		return myRelevantTweets;
	}
	
	/*public void saveLearninData() {
		PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileOutputStream(MY_LEARNING_TWEETS));
            
    		for (String eachTweet: myRelevantTweets)
    			writer.println("[R]" + eachTweet);
    		for (String eachTweet: myNonRelevantWords)
    			writer.println("[N]" + eachTweet);

        } catch (final NoSuchElementException ex) {
            System.out.println("Output folder not found: " + ex.getMessage());
        } catch (final FileNotFoundException ex) {
            System.out.println("Output file not found: " + ex.getMessage());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
	}*/
	
	private List<String> modifyWord(String tweet) {
		StringBuilder tweetString = new StringBuilder();
		List<String> ret = new ArrayList<String>();
		tweet = tweet.toLowerCase();
		char[] temp = tweet.toCharArray();
		for (char eachChar: temp) {
			if (Character.isLetterOrDigit(eachChar)
					|| eachChar == ' ')
				tweetString.append(eachChar);
		}
		
		String[] tweetWords = tweetString.toString().split(" ");
		for (String eachWord: tweetWords) {
			//System.out.println(eachWord + ": " + myStopwords.contains(eachWord));
			boolean isNumber = true;
			for (int i = 0; i < eachWord.length(); i++) {
				if (Character.isLetter(eachWord.charAt(i))) {
					isNumber = false;
					break;
				}
			}
			if (isNumber && !myStopwords.contains(eachWord) && eachWord.length() != 0)
				ret.add(eachWord);
		}
		
		return ret;
	}
	
	
	
	private void learning() {
		myBayes.learn("R", myRelevantWords);
		myBayes.learn("N", myNonRelevantWords);
	}
	
	private void clearMemory() {
		while (myRelevantTweets.size() > MAX_LEARNING_DATA) {
			myRelevantTweets.remove(0);
		}
		while (myNonRelevantWords.size() > MAX_LEARNING_DATA) {
			myNonRelevantTweets.remove(0);
		}
	}

}
