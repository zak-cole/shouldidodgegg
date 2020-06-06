package ibcompsci.urdenwaz.shouldidodge.engine;

import java.io.IOException;
import java.util.*;

import com.google.gson.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


public class Champion {
    
	private boolean dodge; 
	private float winrate; 
	private int loseStreak; 
	private int games; 
	private String ID; 
	private String accountID;
	private String puuID;
	private String name; 
    private ApiClient client;

	public Champion(String ID, String accountID, String puuID, String name, ApiClient client) throws IOException, ApiException {
		this.accountID = accountID;
		this.ID = ID; 
		this.puuID = puuID;
		this.name = name;
		this.client = client;
		this.loseStreak = 0; 
		calculateWinRate();
		calculateLoseStreak();
	}

	public boolean shouldIdodge() {
		return loseStreak > 3 || (winrate <= 45.0 && games > 40);
	}
	public float getWinrate() {
		return winrate;
	}
	public int getLoseStreak() {
		return loseStreak;
	}
	public void calculateWinRate() throws ApiException {
		List<ApiValue> ranked =  client.getLeagues(ID);
		if(ranked == null || ranked.size() == 0) {
			games = 0;
			winrate = 1;
			return;
		}
		float win = Float.parseFloat(ranked.get(0).get("wins"));
		float loss = Float.parseFloat(ranked.get(0).get("losses"));
		games = (int) (win+loss); 
		winrate = win/(win+loss);
	}

	public void calculateLoseStreak() throws ApiException {
		  if(games == 0) {
			  return;
		  }
	      JsonArray matchHistory = client.getRankedMatchHistory(accountID, 13);
	      boolean end = false;
	      //Iterates through all games 
	      for(JsonElement i : matchHistory) {
	    	  if(end == true) {
	    		  break;
	    	  }
	    	  int ChampionID = i.getAsJsonObject().get("champion").getAsInt();
	    	  ApiValue match = client.getMatch(i.getAsJsonObject().get("gameId").getAsString());
	      	  JsonArray participants = match.getJsonArray("participants");
	      	  JsonObject player = null;
	      	  for(JsonElement j: participants) {
	      		  if(j.getAsJsonObject().get("championId").getAsInt() == ChampionID) {
	      			  player = j.getAsJsonObject();
	      			  break;
	      		  }
	      	  }
	      	  if(player == null) {
	      		  continue;
	      	  }
	      	  if(end == false) {
	      		  int teamID = player.get("teamId").getAsInt();
	      		  JsonArray teams = match.getJsonArray("teams");
	      		  String result = teamID == 100? teams.get(0).getAsJsonObject().get("win").getAsString() : teams.get(1).getAsJsonObject().get("win").getAsString();
	      		  if(result.equals("Win")) {
	      			  end = true;
	      		  }else {
	      			  loseStreak++;
	      		  }
	      	  }
	      	  
	    	  
	      }
	      
	}
	
/*
 *     	ApiClient client = new ApiClient(ENDPOINT, ApiClient.loadKey("key.txt"));
    	ApiValue match = client.getMatch("3428811840"); 
    	JsonArray participants = match.getJsonArray("participants");
    	JsonObject participant = participants.get(0).getAsJsonObject();
    	int id = participant.get("participantId").getAsInt();
    	JsonObject stats = participant.getAsJsonObject("stats");
 */
}
