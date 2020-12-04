package com.csc301.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitProfileDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
		}
	}
	
	@Override
	public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
		
		StatementResult existsResult = null;
		
		DbQueryStatus dbQueryStatus = null;
		
		try (Session session = driver.session()) {			
			try (Transaction trans = session.beginTransaction()) {
				
				// Check if user name already exists
				
				String existsQuery = "MATCH (p:profile {userName: \"" + userName + "\"}) RETURN p";
				
				existsResult = trans.run(existsQuery);
				
				List<Record> records = existsResult.list();
				
				if (!records.isEmpty()) {
					dbQueryStatus = new DbQueryStatus("User Profile with userName already exists", DbQueryExecResult.QUERY_ERROR_GENERIC);
					trans.failure();
					return dbQueryStatus;
				}
				
				String query = "MERGE (a:profile {userName: \"" + userName + "\", fullName: \"" + fullName + "\", password: \"" + password + "\" })\n" +
				"MERGE (b:playlist {plName: \"" + userName + "-favourites\" })\n" +
				"CREATE (a)-[:created]->(b)\n" +
						"RETURN a,b ";
				
				trans.run(query);
				dbQueryStatus = new DbQueryStatus("Created user profile", DbQueryExecResult.QUERY_OK);
				trans.success();
				
			}			
			session.close();
		}
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		
		StatementResult existsResult = null;
		DbQueryStatus dbQueryStatus;

		try (Session session = driver.session()) {
			try (Transaction trans = session.beginTransaction()) {

			    // Check if userName or friendUserName exist or not 
			    
			    String checkUserName = "MATCH (p:profile {userName: \"" + userName + "\"}) RETURN p";
			    String checkfriendUserName = "MATCH (p:profile {userName: \"" + frndUserName + "\"}) RETURN p";
			    
                existsResult = trans.run(checkUserName);  
                List<Record> record1 = existsResult.list();
                
                existsResult = trans.run(checkfriendUserName); 
                List<Record> record2 = existsResult.list();
                
                if (record1.isEmpty() || record2.isEmpty()) {
                    dbQueryStatus = new DbQueryStatus("userName or friendUserName does not exist", DbQueryExecResult.QUERY_ERROR_GENERIC);
                    trans.failure();
                    return dbQueryStatus;
                }
			    
			    
				// Check if userName is already following frndUserName

				String existsQuery = "MATCH (a:profile), (b:profile) WHERE a.userName = \"" + userName +  "\" AND b.userName = \"" + frndUserName + "\" \n" +
						"MATCH (a)-[f:follows]->(b) \n" + "RETURN f";
				
				existsResult = trans.run(existsQuery);
				
				List<Record> records = existsResult.list();
				
				if (!records.isEmpty()) {
					dbQueryStatus = new DbQueryStatus(userName + " already follows " + frndUserName, DbQueryExecResult.QUERY_ERROR_GENERIC);
					trans.failure();
					return dbQueryStatus;
					
				}
				
				String query = "MATCH (a:profile), (b:profile) WHERE a.userName = \"" + userName +  "\" AND b.userName = \"" + frndUserName + "\" \n" +
						"CREATE (a)-[:follows]->(b) \n" + "RETURN a,b";
				
				trans.run(query);
				dbQueryStatus = new DbQueryStatus("Created relation", DbQueryExecResult.QUERY_OK);				
				trans.success();

			}
			session.close();
		}
			
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		

		StatementResult existsResult = null;
		DbQueryStatus dbQueryStatus;
		
		try (Session session = driver.session()) {
			try (Transaction trans = session.beginTransaction()) {

				// Check if userName is following frndUserName

				String existsQuery = "MATCH (a:profile), (b:profile) WHERE a.userName = \"" + userName +  "\" AND b.userName = \"" + frndUserName + "\" \n" +
						"MATCH (a)-[f:follows]->(b) \n" + "RETURN f";
				
				existsResult = trans.run(existsQuery);
				
				List<Record> records = existsResult.list();
				
				if (!records.isEmpty()) {
					
					String deleteQuery = "MATCH (a:profile), (b:profile) WHERE a.userName = \"" + userName +  "\" AND b.userName = \"" + frndUserName + "\" \n" +
							"MATCH (a)-[f:follows]->(b) \n" + "DELETE f";
					
					trans.run(deleteQuery);
					
					dbQueryStatus = new DbQueryStatus("User successfully unfollowed", DbQueryExecResult.QUERY_OK);
					trans.success();
					
				} else {
					
					dbQueryStatus = new DbQueryStatus("User not following", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
					trans.failure();
				}
				
				
			}
			session.close();
		}
			
		return dbQueryStatus;
		
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
		
	    StatementResult existsResult = null;
		DbQueryStatus dbQueryStatus;
		StatementResult allUsersFollowedResult;
		
		try (Session session = driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				
			    // Check if user exist in data base 
	            String existsQuery = "MATCH (p:profile {userName: \"" + userName + "\"}) RETURN p";
	            existsResult = trans.run(existsQuery);
                
                List<Record> records = existsResult.list();
                
                if(records.isEmpty()) {
                    trans.failure();
                    session.close();
                    return new DbQueryStatus("User name does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
                }
	               
			    
				// Get all users that userName follows
				
				String allUsersFollowedQuery = "MATCH (p:profile),(nProfile:profile) WHERE p.userName = \"" + userName + "\" \n" +
				"AND (p)-[:follows]->(nProfile) \n" +
				"RETURN nProfile";
								
				allUsersFollowedResult = trans.run(allUsersFollowedQuery);
				
				Map<String, List<String>> allSongsFriendsLike = new HashMap<String, List<String>>();
				
				List<Record> allUsersFollowedRecords = allUsersFollowedResult.list();
				
				List<String> allUsersNamesFollowed = new ArrayList<String>();
				
				StatementResult songsResult;
				
				for (Record record : allUsersFollowedRecords) {
					allUsersNamesFollowed.add(record.get(0).get("userName").toString());	
				}
				
				for (String name : allUsersNamesFollowed) {
					
					String playlistName = name.substring(1, name.length()-1) + "-favourites";
					
					
//					System.out.println(playlistName);
					
					String getSongsQuery = "MATCH (p:profile {userName: " + name + " }), (pl:playlist {plName: \"" + playlistName + "\" }) \n" +
							"MATCH (pl)-[:includes]-(s:song) \n" +
							"RETURN s";
					
					songsResult = trans.run(getSongsQuery);
					
					List<Record> songsResultRecords = songsResult.list();
					
					List<String> songList = new ArrayList<String>();
					
					for (Record songsRecord : songsResultRecords) {
					    
		
						songList.add(songsRecord.get(0).get("songId").toString().replace("\"", ""));
						
					}
					
					
					allSongsFriendsLike.put(name.replace("\"", ""), songList);
					
				}
				
				System.out.println(allSongsFriendsLike);
				
				dbQueryStatus = new DbQueryStatus("Found all songs friends like", DbQueryExecResult.QUERY_OK);
				dbQueryStatus.setData(allSongsFriendsLike);
				
				
				
				
			}
			session.close();
		}
		
		
		return dbQueryStatus;
	}
}
