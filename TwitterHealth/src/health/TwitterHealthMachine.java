package health;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;


public class TwitterHealthMachine {
	
	private static final String MY_KEYWORD_FILE = "relatedterms.txt";
	private static final String MY_STOPWORD_FILE = "stopWords.txt";
	private static final String MY_TEST_FILE = "testingData.txt";
	private static final String MY_SCORE_FILE = "score.txt";
	private static final int TERM_NUMBER = 300;
	private static final int LEARN_NON_RELEVANT = 1;//percentage
	private static final Random MY_RANDOM = new Random();
	private List<String> myMostTen = new ArrayList<String>();
	
	private TweetRetriever myTweetRetriever;
	private NBClassifier myClassifier;
	
	private List<String> myRawTweets;
	private List<String> myRelevantTweets = new ArrayList<String>();
	
	private List<String> myKeywords = new ArrayList<String>();
	private List<String> myNewKeywords = new ArrayList<String>();
	private List<String> myNewWords = new ArrayList<String>();
	private Set<String> myStopwords = new HashSet<String>();
	private Map<String, Integer> myKeywordsCounter = new HashMap<String, Integer>();
	private Map<String, Integer> myNewKeywordsCounter = new HashMap<String, Integer>();
	private PriorityQueue<KeyWordNode> myKeywordsQueue = new PriorityQueue<KeyWordNode>();
	
	private int myRefreshNumCounter = 0;
	private int myRefreshTimeCounter = 0;
	private List<String> myScore = new ArrayList<String>();
	
	public TwitterHealthMachine(String consumerKey, String consumerSecret, String token, String secret){
		myTweetRetriever = new TweetRetriever(consumerKey, consumerSecret, token, secret);
		myClassifier = new NBClassifier();
		myClassifier.learn();
		refreshKeyWords();
		readStopwords();
	}
	
	/**
	 * learn the learning data
	 */
	/*public void learnTweets() {
		myClassifier.learn();
	}*/
	
	/**
	 * refresh the keyword list and classifier;
	 * @param numberOfTweets
	 */
	public void refresh(int numberOfTweets) {
		if (numberOfTweets < 10) {
			throw new IllegalArgumentException(
					"number of Tweets refreshed can not smaller than 10");
		}
		
		myRelevantTweets.clear();
		while (myRelevantTweets.size() < numberOfTweets) {
			int re = 0;
			int nre = 0;
			myRawTweets = myTweetRetriever.getTweets(numberOfTweets * 10);
			for (String eachRawTweets: myRawTweets) {
				if (myClassifier.getRelevant(eachRawTweets).equals("R")) {
					re++;
					myRelevantTweets.add(eachRawTweets);
				} else {
					nre++;
					if (MY_RANDOM.nextInt(100) + 1 <= LEARN_NON_RELEVANT / ((double)100))
						myClassifier.learn("N", eachRawTweets);
				}
				if (myRelevantTweets.size() == numberOfTweets)
					break;
			}
			
			System.out.println("re: " + re + ",  nre: " + nre);
		}
		
		refreshKeyWords();
		learnNewTweets();
		myRefreshNumCounter += numberOfTweets;
		myRefreshTimeCounter++;
		getScore();
	}
	
	
	/**
	 * get top 10 topic
	 */
	public List<String> getMostTen() {
		return myMostTen;
	}
	
	/**
	 * get top 10 tweets
	 * @return
	 */
	public List<String> getTenTweet() {
		List<String> ret = new ArrayList<String>();
		List<String> tweets = new ArrayList<String>(myRelevantTweets);
		tweets.addAll(myClassifier.getRelevantTweets());
		for (String eachTweet: tweets) {
			boolean isContain = false;
			for (String eachTerm: myMostTen) {
				if (eachTweet.contains(eachTerm)) {
					isContain = true;
					break;
				}
			}
			if (isContain)
				ret.add(eachTweet);
		}
		return ret;
	}
	
	/**
	 * this method refresh the keywords list
	 */
	private void refreshKeyWords() {
		readKeywords();
		
		//count the existing keywords
		
		List<String> existTweets = myClassifier.getRelevantTweets();
		
		for (String eachTweet: existTweets)
			myKeywords.addAll(modifyWord(eachTweet));
		
		for (String eachKeyword: myKeywords) {
			if (myKeywordsCounter.keySet().contains(eachKeyword)) {
				myKeywordsCounter.put(eachKeyword,
						myKeywordsCounter.get(eachKeyword) + 1);
			} else {
				myKeywordsCounter.put(eachKeyword, 1);
			}
		}
		
		for (String eachKey: myKeywordsCounter.keySet()) {
			myKeywordsQueue.add(
					new KeyWordNode(eachKey
							, myKeywordsCounter.get(eachKey)));
		}
		
		//counter the new keywords
		for (String eachTweet: myRelevantTweets)
			myNewWords.addAll(modifyWord(eachTweet));
		for (String eachKeyword: myNewWords) {
			if (myNewKeywordsCounter.keySet().contains(eachKeyword)) {
				myNewKeywordsCounter.put(eachKeyword,
						myNewKeywordsCounter.get(eachKeyword) + 1);
			} else {
				myNewKeywordsCounter.put(eachKeyword, 1);
			}
		}
		
		//if new keyword appear more than once, add it to PriorityQueue
		for (String eachKey: myNewKeywordsCounter.keySet()) {
			if (myNewKeywordsCounter.get(eachKey) != 1) {
				myKeywordsCounter.put(eachKey,
						myNewKeywordsCounter.get(eachKey));
			}
		}
		
		//put every new keywords into a PriorityQueue
		for (String eachKey: myNewKeywordsCounter.keySet()) {
			myKeywordsQueue.add(
					new KeyWordNode(eachKey
							, myNewKeywordsCounter.get(eachKey)));
		}
		
		//pull fist 300 keywords and save first 10
		myMostTen.clear();
		myNewKeywords.clear();
		for (int i = 0; i < TERM_NUMBER; i++) {
			if (myKeywordsQueue.size() == 0)
				break;
				
			String keyWord =  myKeywordsQueue.poll().keyword;
			
			boolean isNumber = true;
			for (int j = 0; j < keyWord.length(); j++) {
				if (Character.isLetter(keyWord.charAt(j))) {
					isNumber = false;
					break;
				}
			}
			
			if(isNumber)
				continue;
			
			if (myMostTen.size() < 10)
				myMostTen.add(keyWord);
			
			myNewKeywords.add(keyWord);
			
		}
	
		writeKeywords();
	}
	
	private void readKeywords() {
		String line = null;
		Scanner scanner = null;
        try {
            scanner = new Scanner(new FileInputStream(MY_KEYWORD_FILE));
            while (scanner.hasNextLine()) {
            	line = scanner.nextLine().toLowerCase();
            	myKeywords.add(line);
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
	
	
	
	private void writeKeywords() {
		PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileOutputStream(MY_KEYWORD_FILE));
            
            for (String eachKeyword: myNewKeywords)
            	writer.println(eachKeyword);
        } catch (final NoSuchElementException ex) {
            System.out.println("Output folder not found: " + ex.getMessage());
        } catch (final FileNotFoundException ex) {
            System.out.println("Output file not found: " + ex.getMessage());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
	}
	
	
	
	private void readStopwords() {
		String line = null;
		Scanner scanner = null;
        try {
            scanner = new Scanner(new FileInputStream(MY_STOPWORD_FILE));
            while (scanner.hasNextLine()) {
            	line = scanner.nextLine().toLowerCase();
            	myStopwords.add(line);
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
	
	/**
	 * this method learn new Tweets
	 */
	private void learnNewTweets() {
		List<String> temp;
		for (String eachTweets: myRelevantTweets) {
			temp = modifyWord(eachTweets);
			for (String eachWord: temp) {
				if (myMostTen.contains(eachWord)) {
					myClassifier.learn("R", temp);
					break;
				}
			}
		}
	}
	
	private void getScore() {
		String line = null;
		Scanner scanner = null;
		PrintWriter writer = null;
		int trueP = 0;
		int falseP = 0;
		int falseN = 0;
		int tCounter = 0;
		List<String> tweets = new ArrayList<String>();
		List<Boolean> relevant = new ArrayList<Boolean>();
        try {
            scanner = new Scanner(new FileInputStream(MY_TEST_FILE));
            writer = new PrintWriter(new FileOutputStream(MY_SCORE_FILE));
            while (scanner.hasNextLine()) {
            	line = scanner.nextLine();
            	if (line.length() != 0) {
	            	tweets.add(line.substring(3));
	            	if (line.charAt(1) == 'R')
	            		relevant.add(true);
	            	else
	            		relevant.add(false);
	            	tCounter++;
            	}
            }
            
            for (int i = 0; i < tweets.size(); i++) {
            	if (myClassifier.getRelevant(tweets.get(i)).equals("R")) {
            		if (relevant.get(i))
            			trueP++;
            		else
            			falseP++;
            	} else {
            		if (relevant.get(i))
            			falseN++;
            	}
            }
            
            myScore.add("RefrehTime: " + myRefreshTimeCounter
            		+ ", RefreshNum: " + myRefreshNumCounter
            		+ ", Precision: " + ((double)trueP / (trueP + falseP))
            		+ ", Recall: " + ((double)trueP / (trueP + falseN))
            		+ ", F: " + ((double)2 * trueP / (2 * trueP + falseP + falseN))
            		+ ", A: " + (1 - ((double)(falseP + falseN)) / tCounter));
            
            for (int i = 0; i < myScore.size(); i++)
            	writer.println(myScore.get(i));
            
            
        } catch (final NoSuchElementException ex) {
            System.out.println("Input folder not found: " + ex.getMessage());
        } catch (final FileNotFoundException ex) {
            System.out.println("Input file not found: " + ex.getMessage());
        } finally {
            if (scanner != null)
                scanner.close();
            if (writer != null)
            	writer.close();
        }
	}
	
	private class KeyWordNode implements Comparable<KeyWordNode>{
		String keyword;
		int times;
		KeyWordNode(String aKeyword, int theTimes) {
			keyword = aKeyword;
			times = theTimes;
		}
		
		@Override
		public int compareTo(KeyWordNode aNode) {
			return aNode.times - times;
		}
	}
}
