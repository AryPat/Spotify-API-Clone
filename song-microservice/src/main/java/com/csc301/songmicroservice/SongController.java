package com.csc301.songmicroservice;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class SongController {

	@Autowired
	private final SongDal songDal;

	private OkHttpClient client = new OkHttpClient();

	
	public SongController(SongDal songDal) {
		this.songDal = songDal;
	}

	
	@RequestMapping(value = "/getSongById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {
	   
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = songDal.findSongById(songId);

		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}

	
	@RequestMapping(value = "/getSongTitleById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongTitleById(@PathVariable("songId") String songId,
			HttpServletRequest request) {
	    
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = songDal.getSongTitleById(songId);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		

		return response;
	}

	
	@RequestMapping(value = "/deleteSongById/{songId}", method = RequestMethod.DELETE)
	public @ResponseBody Map<String, Object> deleteSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("DELETE %s", Utils.getUrl(request)));
		
		
		DbQueryStatus dbQueryStatus = songDal.deleteSongById(songId);
		
		if(dbQueryStatus.getMessage().equals("Removed song from DB")) {
	        Request sendReq = new Request.Builder().url("http://localhost:3002/deleteAllSongsFromDb/" + songId).put(new FormBody.Builder().build())
                      .build();
            
            // Most likely this part of the code wont return.
            try (Response sentReq = this.client.newCall(sendReq).execute()){
                String reqBody = sentReq.body().string();
                JSONObject reqJson = new JSONObject(reqBody);
                
                if(!reqJson.get("status").equals("OK")) {
                    response.put("message", "Internal: Error when deleting songs in profile microservice");
                    Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_NOT_FOUND, null);
                    return response;
                }   
                
                
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
		}
		
		
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}

	
	@RequestMapping(value = "/addSong", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addSong(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

	    Map<String, Object> response = new HashMap<String, Object>();
	    
	    String songName = null, songArtistFullName = null, songAlbum = null; 
	    songName = params.get("songName");
	    songArtistFullName = params.get("songArtistFullName");
	    songAlbum = params.get("songAlbum");
	    
	    //Check for BAD_REQUEST 
	    if(songName == null || songArtistFullName == null | songAlbum == null) {
	        response.put("status", HttpStatus.BAD_REQUEST);
	        return response;
	    }
	    
	    Song to_Add = new Song(songName, songArtistFullName, songAlbum);
	    DbQueryStatus dbQueryStatus = songDal.addSong(to_Add);
	    
	    
		response.put("path", String.format("POST %s", Utils.getUrl(request)));
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}

	
	@RequestMapping(value = "/updateSongFavouritesCount/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> updateFavouritesCount(@PathVariable("songId") String songId,
			@RequestParam("shouldDecrement") String shouldDecrement, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();

		if(!shouldDecrement.equals("true") && !shouldDecrement.equals("false")) {
		    response.put("status", HttpStatus.BAD_REQUEST);
		    return response;
		}
		
		response.put("data", String.format("PUT %s", Utils.getUrl(request)));
		DbQueryStatus dbQueryStatus = songDal.updateSongFavouritesCount(songId, Boolean.parseBoolean(shouldDecrement));
		
		
		response.put("messmage", dbQueryStatus.getMessage());
		
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		

		return response;
	}
}