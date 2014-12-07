package health;
import java.util.List;
public class TestMain {

	public static void main(String[] args) {
		TweetRetriever tr = new TweetRetriever("StslpRKhCwJ4V6GRCSDzDsRCe",
				"445pzWwNmIu5PJfXXoTKUUSIkyXdl9FuVV5IOT4AoS73IKeh4n",
				"122779575-q5PgoMy2yextJKpa7Ei0ux3kxV06A5yXIWsMPRLp",
				"x092uSGnIN8i7T7KAu0M7WcbLrWlGXVffekviqQ2eYdIC");
		
		List<String> tweets = tr.getTweets(100);
		
		System.out.println("getSize: " + tweets.size());
			
	}

}
 