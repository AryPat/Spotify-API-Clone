package com.csc301.profilemicroservice;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.csc301.profilemicroservice.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class ProfileController {
	public static final String KEY_USER_NAME = "userName";
	public static final String KEY_USER_FULLNAME = "fullName";
	public static final String KEY_USER_PASSWORD = "password";

	@Autowired
	private final ProfileDriverImpl profileDriver;

	@Autowired
	private final PlaylistDriverImpl playlistDriver;

	OkHttpClient client = new OkHttpClient();

	public ProfileController(ProfileDriverImpl profileDriver, PlaylistDriverImpl playlistDriver) {
		this.profileDriver = profileDriver;
		this.playlistDriver = playlistDriver;
	}

	// Create a profile 
	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addProfile(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

	    // Initiatialize variable
		Map<String, Object> response = new HashMap<String, Object>();
		
		String userName = null;
		String fullName = null;
		String password = null;
		
		userName = params.get("userName");
		fullName = params.get("fullName");
		password = params.get("password");
		
		// Check for BAD_REQUEST
		if(userName == null || fullName == null || password == null) {
			response.put("status", HttpStatus.BAD_REQUEST);
			return response;
		}
		
		// Method that calls the db
		DbQueryStatus dbQueryStatus = profileDriver.createUserProfile(userName, fullName, password);
		
		// Response 
		response.put("path", String.format("POST %s", Utils.getUrl(request)));
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		
		return response;
	}

	// Follow a friend 
	@RequestMapping(value = "/followFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> followFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

	    // Initialize variable
		Map<String, Object> response = new HashMap<String, Object>();
		
		// Check for BAD_REQUEST
		if (userName == null || friendUserName == null) {
			response.put("status", HttpStatus.BAD_REQUEST);
			return response;
		}
		
		if (userName.equals(friendUserName)) {
            response.put("status", HttpStatus.BAD_REQUEST);
            return response;
        }
		
		// method that calls db 
		DbQueryStatus dbQueryStatus = profileDriver.followFriend(userName, friendUserName);
		
		// Response
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		
		return response;
	}

	// Get all friend's favourite song titles given user name
	@RequestMapping(value = "/getAllFriendFavouriteSongTitles/{userName}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getAllFriendFavouriteSongTitles(@PathVariable("userName") String userName,
			HttpServletRequest request) {

	    // Method that calls db
		DbQueryStatus dbQueryStatus = profileDriver.getAllSongFriendsLike(userName);
		Map<String, Object> response = new HashMap<String, Object>();
	
		// Check if username exist 
		if(dbQueryStatus.getdbQueryExecResult().equals(DbQueryExecResult.QUERY_ERROR_NOT_FOUND)) {
		    response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_NOT_FOUND, null);
		    return response;
		}
		
		// Change all songId's as values to their respective song title 
		Map<String, List<String>> allSongsFriendsLike = (Map<String, List<String>>) dbQueryStatus.getData();
		
		// Go through each name 
		for(String name : allSongsFriendsLike.keySet()) {
		    List<String> songName = new ArrayList<String>();
		    
		    // Go through each songId
		    for(String songId : allSongsFriendsLike.get(name)) {
		        
		        // get song title given the songId
		        Request req = new Request.Builder().url("http://localhost:3001/getSongTitleById/"+songId).build();
		        JSONObject reqJson;
		        
		        try(Response getReq = this.client.newCall(req).execute()){
		            String reqBody = getReq.body().string();
		            reqJson = new JSONObject(reqBody);
		            
		            // Return statement here cannot be tested 
		            // Do not believe code will ever enter this if loop
		            // Just in case!   
		            if(!reqJson.get("status").equals("OK")) {
		                response.put("message", "Internal Error in Song Microservice!");
		                Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_NOT_FOUND, null);
		                return response;
		            }       
		            songName.add((String) reqJson.get("data"));
		            
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
		        
		        
		    }
		    // Create the data to be returned
		    allSongsFriendsLike.put(name, songName);
		}
		
		// Response
	    response.put("message", dbQueryStatus.getMessage());
	    response.put("path", String.format("PUT %s", Utils.getUrl(request)));
	    response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), allSongsFriendsLike);
		return response;
	}

	// Unfollow a friend 
	@RequestMapping(value = "/unfollowFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unfollowFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

	    // Initialize variable 
		Map<String, Object> response = new HashMap<String, Object>();
		
		// Check for BAD_REQUEST 
		if (userName == null || friendUserName == null) {
			response.put("status", HttpStatus.BAD_REQUEST);
			return response;
		}
		
		// method that calls the db
		DbQueryStatus dbQueryStatus = profileDriver.unfollowFriend(userName, friendUserName);
		
		// Response
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());		
		
		return response;
	}

	// Like a song
	@RequestMapping(value = "/likeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> likeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

	    // Initialize variable
		Map<String, Object> response = new HashMap<String, Object>();
		
		// Check if the song exist in the Song's database 
		Request req = new Request.Builder().url("http://localhost:3001/getSongById/"+songId).build();
		
		try(Response getReq = this.client.newCall(req).execute()){
		    String reqBody = getReq.body().string();
		    JSONObject reqJson = new JSONObject(reqBody);
		    
		    if(!reqJson.get("status").equals("OK")) {
		        response.put("message", "Song does not exist!");
		        Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_NOT_FOUND, null);
		        return response;
		    }		    
		    
		} catch (IOException e) {
            e.printStackTrace();
        }
		
		// Method that calls db
		DbQueryStatus dbQueryStatus = playlistDriver.likeSong(userName, songId);
				
		// You check if the song is not in your playlist therefore you need to increment by 1 in Song's Database
		// Only gets called if user does not LIKE the song originally
		if(dbQueryStatus.getMessage().equals("put song in playlist")) {
		    
		    // Call Song-Microservice
		    Request sendReq = new Request.Builder().url("http://localhost:3001/updateSongFavouritesCount/"+songId+"?shouldDecrement=false").put(new FormBody.Builder().build())
		              .build();
		    
		    // Most likely this part of the code wont return.
		    try (Response sentReq = this.client.newCall(sendReq).execute()){
		        
		        String reqBody = sentReq.body().string();
		        JSONObject reqJson = new JSONObject(reqBody);
		        if(!reqJson.get("status").equals("OK")) {
	                response.put("message", "Internal: Error when updating song's count");
	                Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_NOT_FOUND, null);
	                return response;
	            }   
		       
		    } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
		    }
		}
		
		// Response
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}

	// Unlike a song
	@RequestMapping(value = "/unlikeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unlikeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

	    // Initialize varialbe
		Map<String, Object> response = new HashMap<String, Object>();
		
		// Check if the song exist in the Song's database 
		// Call Song-Microservice
        Request req = new Request.Builder().url("http://localhost:3001/getSongById/"+songId).build();
        
        try(Response getReq = this.client.newCall(req).execute()){
            
            String reqBody = getReq.body().string();
            JSONObject reqJson = new JSONObject(reqBody);
            if(!reqJson.get("status").equals("OK")) {
                response.put("message", "Song does not exist!");
                Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_NOT_FOUND, null);
                return response;
            }           
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        	
        // method that calls the db
		DbQueryStatus dbQueryStatus = playlistDriver.unlikeSong(userName, songId);
		
		// Check if you are liked with the song, if so, then decrease favourite by 1 after unliking in songs DB
		if(dbQueryStatus.getMessage().equals("Deleted song in playlist")) {
	        Request sendReq = new Request.Builder().url("http://localhost:3001/updateSongFavouritesCount/"+songId+"?shouldDecrement=true").put(new FormBody.Builder().build())
                      .build();
            
            // Most likely this part of the code wont return.
            try (Response sentReq = this.client.newCall(sendReq).execute()){
                String reqBody = sentReq.body().string();
                JSONObject reqJson = new JSONObject(reqBody);
                
                if(!reqJson.get("status").equals("OK")) {
                    response.put("message", "Internal: Error when updating song's favourite count");
                    Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_NOT_FOUND, null);
                    return response;
                }   
                
                
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
		}
		
		// Response 
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}

	// Delete song from database
	@RequestMapping(value = "/deleteAllSongsFromDb/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> deleteAllSongsFromDb(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		// Initialize variable 
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		// Response
		response = Utils.setResponseStatus(response, playlistDriver.deleteSongFromDb(songId).getdbQueryExecResult(), null);
		return response;
	}
}