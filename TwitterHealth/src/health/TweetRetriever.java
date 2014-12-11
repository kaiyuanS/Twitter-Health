package health;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.endpoint.StatusesSampleEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.BasicClient;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

public class TweetRetriever {
	private List<String> Tweets;
	Authentication auth;
	BlockingQueue<String> queue;
	public TweetRetriever(String consumerKey, String consumerSecret, String token, String secret) {

		this.queue = new LinkedBlockingQueue<String>(10000);
		this.auth = new OAuth1(consumerKey, consumerSecret, token, secret);
	}
	/**
	 * Get health related tweets based on the related terms files. This is done in english
	 * @param number-number of health related tweets to receive
	 * @return List<String> of health related tweets with links and twitter handles removed. Also removes non-letters
	 */
	public List<String> getTweets(int number){
		Tweets=new ArrayList<String>();
		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
		endpoint.stallWarnings(false);
		try {
			Scanner scan=new Scanner(new File("relatedterms.txt"));
			TreeSet<String> TS=new TreeSet<String>();
			while(scan.hasNextLine()){
				TS.add(scan.nextLine());
			}
			ArrayList<String> terms=new ArrayList<String>();
			for(String s:TS){
				terms.add(s);
			}
			scan.close();
			endpoint.trackTerms(terms);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BasicClient client = new ClientBuilder()
		.name("HealthClient")
		.hosts(Constants.STREAM_HOST)
		.endpoint(endpoint)
		.authentication(auth)
		.processor(new StringDelimitedProcessor(queue))
		.build();
		
		client.connect();

		HashSet<String> unique=new HashSet<String>();
		while(unique.size()<number) {
			try{
			
				if (client.isDone()) {
					System.out.println("Client connection closed unexpectedly: " + client.getExitEvent().getMessage());
					break;
				}

				String msg = queue.poll(1, TimeUnit.SECONDS);
				if (msg != null) {
					JSONObject tweet=new JSONObject(msg);
					//					ignore deletions
					if(!tweet.has("delete")){
						String language=tweet.get("lang").toString();
						if(language.equals("en")){
							String text=tweet.get("text").toString();
							Scanner remove=new Scanner(text);
							String minimize="";
							while(remove.hasNext()){
								String word=remove.next();
								boolean toNotAdd=false;
								if(word.indexOf("http://")>=0||word.indexOf("https://")>=0){
									toNotAdd=true;
								}
								if(word.indexOf('@')<0 && !toNotAdd){
									word=word.replaceAll("\\W", "");
									minimize+=" "+word;

								}
							}
							remove.close();
							unique.add(minimize.trim());
							
						}

					}
				} else {

					System.out.println("Did not receive a message in 1 seconds");
				}
				
				System.out.println("get " + unique.size() + " Tweets");
			} catch (InterruptedException e) {
				System.err.println(e.getMessage());
			} catch (JSONException e) {
				System.err.println(e.getMessage());
				continue;
			}
		}
		
		Tweets.addAll(unique);
		client.stop();
		
		return Tweets;
	}
/**
 * Gets a number of random tweets
 * @param number - number of random tweets to receive from users with english as their setting
 * @return List<String> of tweets with links and twitter handles removed. Also removes non-letters
 */
	/*public List<String> getRandomTweets(int number){
		Tweets=new ArrayList<String>();
		StatusesSampleEndpoint endpoint = new StatusesSampleEndpoint();
		endpoint.stallWarnings(false);
		
		BasicClient client = new ClientBuilder()
		.name("HealthClient")
		.hosts(Constants.STREAM_HOST)
		.endpoint(endpoint)
		.authentication(auth)
		.processor(new StringDelimitedProcessor(queue))
		.build();
		
		client.connect();

		HashSet<String> unique=new HashSet<String>();
		while(unique.size()<number) {
			try{
			
				if (client.isDone()) {
					System.out.println("Client connection closed unexpectedly: " + client.getExitEvent().getMessage());
					break;
				}

				String msg = queue.poll(1, TimeUnit.SECONDS);
				if (msg != null) {
					JSONObject tweet=new JSONObject(msg);
					//					ignore deletions
					if(!tweet.has("delete")){
						String language=tweet.get("lang").toString();
						if(language.equals("en")){
							String text=tweet.get("text").toString();
							Scanner remove=new Scanner(text);
							String minimize="";
							while(remove.hasNext()){
								String word=remove.next();
								boolean toNotAdd=false;
								if(word.indexOf("http://")>=0||word.indexOf("https://")>=0){
									toNotAdd=true;
								}
								if(word.indexOf('@')<0 && !toNotAdd){
									word=word.replaceAll("\\W", "");
									minimize+=" "+word;

								}
							}
							remove.close();
							unique.add(minimize.trim());
							
						}

					}
				} else {

					System.out.println("Did not receive a message in 1 seconds");
				}
				
				System.out.println("get " + unique.size() + " Tweets");
			} catch (InterruptedException e) {
				System.err.println(e.getMessage());
			} catch (JSONException e) {
				System.err.println(e.getMessage());
				continue;
			}
		}
		
		Tweets.addAll(unique);
		client.stop();
		
		return Tweets;
	}*/
	
}

