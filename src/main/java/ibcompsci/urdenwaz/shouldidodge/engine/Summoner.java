package ibcompsci.urdenwaz.shouldidodge.engine;

import java.awt.Image;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import javax.imageio.ImageIO;

import com.google.gson.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Summoner {

	private boolean dodge;
	private float winrate;
	private int loseStreak;
	private int games;
	private String ID;
	private String accountID;
	private String puuID;
	private String name;
	private ApiClient client;
	private HashMap<Integer, int[]> championWinRate;
	HashSet<Integer> AllEgirlChamps;
	private HashMap<String, Integer> roles;
	private String mainRole;	
	private int profileIcon;
	private DdragonLookup lookup;
	private boolean egirl;
	public Summoner(String ID, String accountID, String puuID, String name, int profileIcon, ApiClient client)
			throws IOException, ApiException {
		this.accountID = accountID;
		this.ID = ID;
		this.puuID = puuID;
		this.name = name;
		this.client = client;
		this.profileIcon = profileIcon;
		this.loseStreak = 0;
		this.championWinRate = new HashMap<Integer, int[]>();
		this.roles = new HashMap<String, Integer>();
		this.mainRole = "";
		lookup = new DdragonLookup("10.10.1");
	
		calculateWinRate();
		calculateLoseStreakAndRole();
		this.AllEgirlChamps = new HashSet<>();
		egirl = isEGirl();
	}

	public Image getProfileIcon() throws IOException {
		return lookup.getProfileIcon(profileIcon);
	}

	public boolean shouldIdodge() {
		return loseStreak > 3 || (winrate <= 0.45 && games > 40);
	}
	public boolean shouldIdodge(String input) {
		if(input.equals("Support")) {
			input = "DUO_SUPPORT";
		}
		else if(input.equals("ADC")) {
			input = "DUO_CARRY";
		}
		input = input.toUpperCase();
		return shouldIdodge() || (!mainRole.equals(input));
	} 
	public boolean shouldIdodge(int champion) throws ApiException {
		return shouldIdodge() || firstTime(champion) ||(egirl && !AllEgirlChamps.contains(champion));
	}
	 
	public int getChampionID(String c) {
		return lookup.getChampionID(c);
	}
	
	public boolean shouldIdodge(String input, int champion) throws ApiException {
		
		boolean champ = false;
		boolean role = false;
		if(champion != -1)
		champ = shouldIdodge(champion);
		if(!input.equals(""))
		role = shouldIdodge(input);
		return role || champ || shouldIdodge();
		
	}
	
	public float getWinrate() {
		return winrate;
	}

	public int getLoseStreak() {
		return loseStreak;
	}

	public String getMainRole() {
		return mainRole;
	}
	public String getName() {
		return name;
	}

	
	public void calculateWinRate() throws ApiException {
		List<ApiValue> ranked = client.getLeagues(ID);
		if (ranked == null || ranked.size() == 0) {
			games = 0;
			winrate = 1;
			return;
		}
		ApiValue rankedSolo = null;
		for (ApiValue i : ranked) {
			if (i.get("queueType").equals("RANKED_SOLO_5x5")) {
				rankedSolo = i;
				break;
			}
		}
		if (rankedSolo == null) {
			games = 0;
			winrate = 1;
			return;
		}
		float win = Float.parseFloat(rankedSolo.get("wins"));
		float loss = Float.parseFloat(rankedSolo.get("losses"));
		games = (int) (win + loss);
		winrate = win / (win + loss);
	}

	public void calculateLoseStreakAndRole() throws ApiException {
		if (games == 0) {
			return;
		}
		JsonArray matchHistory = client.getRankedMatchHistory(accountID, 13);
		boolean end = false;
		// Iterates through all games

		for (JsonElement i : matchHistory) {
//			if (end == true) {
//				break;
//			}
			String role = i.getAsJsonObject().get("lane").getAsString();
			if (role.equals("BOTTOM")) {
				String bottom = i.getAsJsonObject().get("role").getAsString();
				if (roles.containsKey(bottom))
					roles.put(bottom, roles.get(bottom) + 1);
				else
					roles.put(bottom, 1);
			} else {
				if (roles.containsKey(role))
					roles.put(role, roles.get(role) + 1);
				else
					roles.put(role, 1);
			}

			if (end == false) {
				int ChampionID = i.getAsJsonObject().get("champion").getAsInt();
				ApiValue match = client.getMatch(i.getAsJsonObject().get("gameId").getAsString());
				JsonArray participants = match.getJsonArray("participants");
				if (participants == null) {
					return;
				}
				JsonObject player = null;
				for (JsonElement j : participants) {
					if (j.getAsJsonObject().get("championId").getAsInt() == ChampionID) {
						player = j.getAsJsonObject();
						break;
					}
				}
				if (player == null) {
					continue;
				}
				int teamID = player.get("teamId").getAsInt();
				JsonArray teams = match.getJsonArray("teams");
				String result = teamID == 100 ? teams.get(0).getAsJsonObject().get("win").getAsString()
						: teams.get(1).getAsJsonObject().get("win").getAsString();
				if (result.equals("Win")) {
					end = true;

				} else {
					loseStreak++;
				}
			}

		}
		setMainRole();

	}

	public void calculateChampionWinRate(int index, int champion) {
		if (championWinRate.containsKey(champion)) {
			championWinRate.get(champion)[index] += 1;
			return;
		}
		int[] a = new int[2];
		a[index] += 1;
		championWinRate.put(champion, a);
	}

	public boolean firstTime(int champion) throws ApiException {
		ApiValue mastery = client.getChampionMasteryByChampionID(ID, champion);
		if (mastery.getRawJsonObject().has("championLevel") == false) {
			return true;
		}
		int championLevel = mastery.getRawJsonObject().get("championLevel").getAsInt();

		return championLevel < 3;
	}

	public void setMainRole() {
		int max = 0;
		mainRole = "None";
		for (String i : roles.keySet()) {
			if (roles.get(i) > max) {
				max = roles.get(i);
				mainRole = i;
			}
		}
	}

	/*
	 * yuumi 350 Soraka 16 nami 267 morgana 25 janna 40 lux 99 lulu 117 sona 37 ahri 103 zoe 142 miss fortune 21
	 * 
	 */

	public boolean isEGirl() throws ApiException, FileNotFoundException {
		JsonArray masteries = client.getChampions(ID);
		Scanner sc = new Scanner(new File("tooEasyChamps.txt"));

		while(sc.hasNext()) {
			int holder = sc.nextInt();

			AllEgirlChamps.add(holder);
		}
		int counter = 0; 
		int EgirlChamps = 0;
		for(JsonElement i: masteries) {
			counter++;
			int currentChampionID = i.getAsJsonObject().get("championId").getAsInt();
			//System.out.println(currentChampionID );
			if(AllEgirlChamps.contains(currentChampionID)) {
				EgirlChamps++;
			}
			if(counter > 4) {
				break;
			}
		}
		//System.out.println(EgirlChamps+" "+counter);
		return (counter-EgirlChamps <= 2);
	}

	@Override
	public String toString() {
		return name;
	}
	/*
	 * ApiClient client = new ApiClient(ENDPOINT, ApiClient.loadKey("key.txt"));
	 * ApiValue match = client.getMatch("3428811840"); JsonArray participants =
	 * match.getJsonArray("participants"); JsonObject participant =
	 * participants.get(0).getAsJsonObject(); int id =
	 * participant.get("participantId").getAsInt(); JsonObject stats =
	 * participant.getAsJsonObject("stats");
	 */
}
