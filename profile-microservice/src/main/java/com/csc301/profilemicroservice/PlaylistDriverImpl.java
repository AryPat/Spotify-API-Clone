package com.csc301.profilemicroservice;

import java.util.List;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitPlaylistDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}

	@Override
	public DbQueryStatus likeSong(String userName, String songId) {

		StatementResult result = null;
		StatementResult existsResult = null;
		
		DbQueryStatus dbQueryStatus = null;
		
		try (Session session = driver.session()) {			
			try (Transaction trans = session.beginTransaction()) {
				
				// Check if user exists
				
				String userExistsQuery = "MATCH (pl:playlist {plName: \"" + userName + "-favourites\" }) RETURN pl";
				
				result = trans.run(userExistsQuery);
				
				List<Record> userRecords = result.list();
				
				if (userRecords.isEmpty()) {
					
					dbQueryStatus = new DbQueryStatus("User not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
					trans.failure();
					
				} else {
					
				    String message = "put song in playlist";
				
					// Check if playlist and song relation exists
					
					String existsQuery = "MATCH (pl:playlist {plName: \"" + userName + "-favourites\" }), (s:song {songId: \"" + songId + "\" }) \n" +
							"MATCH (pl)-[r:includes]-(s) \n" +
							"RETURN r";
					
					existsResult = trans.run(existsQuery);
					
					List<Record> records = existsResult.list();
					
					if(!records.isEmpty()) {
					    message = "song already liked";
					    dbQueryStatus = new DbQueryStatus(message, DbQueryExecResult.QUERY_OK);
	                    trans.success();
	                    return dbQueryStatus;
					}
					
					// Add song to playlist
					
					String query = "MATCH (pl:playlist {plName: \"" + userName + "-favourites\" }) \n" +
							"MERGE (s:song {songId: \"" + songId + "\" }) \n" + 
							"MERGE (pl)-[r:includes]->(s)\n" +
							"RETURN r";
					
					trans.run(query);
					
					dbQueryStatus = new DbQueryStatus(message, DbQueryExecResult.QUERY_OK);
					trans.success();
					
				}
			}			
			session.close();
		}
		return dbQueryStatus;
		
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		
		StatementResult existsResult = null;
		
		DbQueryStatus dbQueryStatus = null;
		
		try (Session session = driver.session()) {			
			try (Transaction trans = session.beginTransaction()) {
				
				// Check if user name already exists
				
				String existsQuery = "MATCH (p:profile {userName: \"" + userName + "\"}) RETURN p";
				
				existsResult = trans.run(existsQuery);
				
				List<Record> records = existsResult.list();
				
				if (!records.isEmpty()) {
					
				    String checkSong = "MATCH (pl:playlist {plName: \"" + userName + "-favourites\" }), (pl)-[r:includes]->(:song {songId: \"" + songId + "\" })\n" + "RETURN r";
					String query = "MATCH (pl:playlist {plName: \"" + userName + "-favourites\" }), (pl)-[r:includes]->(:song {songId: \"" + songId + "\" })\n" +
							"DELETE r";
					
					// Check if the user does not have a relation with the song 
					
					existsResult = trans.run(checkSong);
					List<Record> check = existsResult.list();
					
					if(check.isEmpty()) {
					    dbQueryStatus = new DbQueryStatus("User does not have relation with song", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
	                    trans.failure();
	                    return dbQueryStatus;
					}
					
					// User likes the song, therefore delete it from their playlist
					
					existsResult = trans.run(query);
				
					dbQueryStatus = new DbQueryStatus("Deleted song in playlist", DbQueryExecResult.QUERY_OK);
					trans.success();
					
				} else {
					dbQueryStatus = new DbQueryStatus("User does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
					trans.failure();
				}
			}
			session.close();
		}
		return dbQueryStatus;
		
	}

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		
		StatementResult existsResult = null;
		
		DbQueryStatus dbQueryStatus = null;
		
		try (Session session = driver.session()) {			
			try (Transaction trans = session.beginTransaction()) {
				
			    // This is a helper function
			    // This does not care if there EXISTED a song with a relation 
			    // Just deletes all songs and gives OK message
			    // Pretty Sure I am right...
				
				String existsQuery = "MATCH (s:song {songId: \"" + songId + "\"}) RETURN s";
				
				existsResult = trans.run(existsQuery);
				
				List<Record> records = existsResult.list();

				String query = "MATCH (s:song {songId: \"" + songId + "\"}) DETACH DELETE s";

				trans.run(query);
					
				dbQueryStatus = new DbQueryStatus("Deleted song in database", DbQueryExecResult.QUERY_OK);
				trans.success();
					
			
			}			
			session.close();
		}
		return dbQueryStatus;
	}
}
