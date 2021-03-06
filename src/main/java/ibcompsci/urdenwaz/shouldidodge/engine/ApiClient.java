package ibcompsci.urdenwaz.shouldidodge.engine;

import com.google.gson.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ApiClient {

    // See https://developer.riotgames.com/regional-endpoints.html
    String endpoint;
    // See https://developer.riotgames.com/api-keys.html
    String key;

    Gson gson;
    JsonParser parser;
    OkHttpClient http;

    // See https://developer.riotgames.com/static-data.html

    // See https://developer.riotgames.com/api-keys.html
    public static String loadKey(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path))).trim();
    }

    /**
     * Creates a new client using the specified endpoint and key
     * @param endpoint DNS name of the endpoint
     * @param key API key
     */
    public ApiClient(String endpoint, String key) {
        this(endpoint, key, new GsonBuilder().setPrettyPrinting().create(),
                new JsonParser(), new OkHttpClient());
    }

    public ApiClient(String endpoint, String key, Gson gson, JsonParser parser, OkHttpClient http) {
        this.endpoint = endpoint;
        this.key = key;
        this.gson = gson;
        this.parser = parser;
        this.http = http;
    }

    private <T> T get(String path, Function<JsonElement, T> evaluator) throws ApiException {
        try {
            Request request = new Request.Builder()
                    .url(new URL("https", endpoint, path))
                    .addHeader("X-Riot-Token", key)
                    .get()
                    .build();

            try (Response response = http.newCall(request).execute()) {
                JsonElement element = parser.parse(response.body().charStream());
                return evaluator.apply(element);
            }

        } catch (IOException e) {
            throw new ApiException("IO Error", e);
        }
    }

    private List<ApiValue> getList(String path) throws ApiException {
        return get(path, (element) -> {
            JsonArray array = element.getAsJsonArray();
            return StreamSupport.stream(array.spliterator(), false)
                    .map(e -> new ApiValue(gson, e.getAsJsonObject()))
                    .collect(Collectors.toList());
        });
    }

    private ApiValue get(String path) throws ApiException {
        return get(path, (element) -> {
            JsonObject object = element.getAsJsonObject();
            return new ApiValue(gson, object);
        });
    }
    private JsonArray getAsJsonArray(String path) throws ApiException{
    	  return get(path, (element) -> {
              JsonArray array = element.getAsJsonArray();
              return array;
          });
    }
    private String sanitize(String parameter) throws ApiException {
        try {
            return URLEncoder.encode(parameter, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ApiException("Unable to encode parameter [" + parameter + "]", e);
        }
    }

    // Return the "raw" JSON response, however it has been parsed/serialized
    // for formatting and verifying it is valid JSON
    public String getRaw(String path) throws ApiException {
        return get(path, e -> gson.toJson(e));
    }

    // https://developer.riotgames.com/api-methods/#summoner-v4/GET_getBySummonerName
    public ApiValue getSummoner(String name) throws ApiException {
        return get("/lol/summoner/v4/summoners/by-name/" + sanitize(name));
    }

    // https://developer.riotgames.com/api-methods/#champion-mastery-v4/GET_getAllChampionMasteries
    // Requires id
    public JsonArray getChampions(String id) throws ApiException {
        return getAsJsonArray("/lol/champion-mastery/v4/champion-masteries/by-summoner/" + sanitize(id));
    }

    //https://developer.riotgames.com/apis#champion-mastery-v4/GET_getAllChampionMasteries
    //Requires Id
    //Required championID
    public ApiValue getChampionMasteryByChampionID(String id, int championID) throws ApiException {
    	StringBuilder sb = new StringBuilder(); 
    	sb.append("/lol/champion-mastery/v4/champion-masteries/by-summoner/");
    	sb.append(sanitize(id));
    	sb.append("/by-champion/");
    	sb.append(championID);
    	return get(sb.toString());
    }

    // https://developer.riotgames.com/api-methods/#league-v4/GET_getAllLeaguePositionsForSummoner
    // Requires id
    public List<ApiValue> getLeagues(String id) throws ApiException {
        return getList("/lol/league/v4/entries/by-summoner/" + sanitize(id));
    }

    // https://developer.riotgames.com/apis#match-v4/GET_getMatchlist
    // Requires accountID
    public List<ApiValue> getMatchHistory(String accountID, int numMatches) throws ApiException {
        ApiValue val =  get("/lol/match/v4/matchlists/by-account/" + sanitize(accountID));
        JsonArray array = val.getJsonArray("matches");
        List<ApiValue> values = new ArrayList<>();

        for (int i = 0; i < numMatches; i++) {
            values.add(new ApiValue(new GsonBuilder().setPrettyPrinting().create(),
                    array.get(i).getAsJsonObject()));
        }
        return values;
    }

    //Requires accountID
    //Requires season 
    //search ranked only QueueID: 420 
    public JsonArray getRankedMatchHistory(String accountID, int season) throws ApiException {
    	StringBuilder sb = new StringBuilder(); 
    	sb.append("/lol/match/v4/matchlists/by-account/");
    	sb.append(sanitize(accountID));
    	sb.append("?queue=420&season=");
    	sb.append(13);
        ApiValue val =  get(sb.toString());

        JsonArray array = val.getJsonArray("matches");
        //debug test
        //System.out.println(val.getRawJsonObject().get("totalGames").getAsInt());
        return array;
    }

    // https://developer.riotgames.com/apis#match-v4/GET_getMatch
    // Requires matchID
    public ApiValue getMatch(String matchID) throws ApiException {
        return get("/lol/match/v4/matches/" + sanitize(matchID));
    }
    
}

