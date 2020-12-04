package com.csc301.songmicroservice;

import org.bson.types.ObjectId;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

	@Override
	public DbQueryStatus addSong(Song songToAdd) {
		db.insert(songToAdd);
		DbQueryStatus dbQueryStatus = new DbQueryStatus("Added the song", DbQueryExecResult.QUERY_OK);
		dbQueryStatus.setData(songToAdd);

		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
	
		Song song = db.findById(songId, Song.class);
		
		// No such song exist 
		if(song == null) {
		    DbQueryStatus dbQueryStatus = new DbQueryStatus("No songs found in DB", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		    return dbQueryStatus;
		}
		
		DbQueryStatus dbQueryStatus = new DbQueryStatus("Song found in DB", DbQueryExecResult.QUERY_OK);
		dbQueryStatus.setData(song);
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		
	    Song song = db.findById(songId, Song.class);
	    
	    //No Such songs found
	    if(song == null) {
	        return new DbQueryStatus("No song found in DB", DbQueryExecResult.QUERY_ERROR_NOT_FOUND); 
	    }
	    
	    DbQueryStatus dbQueryStatus = new DbQueryStatus("song found in DB", DbQueryExecResult.QUERY_OK);
	    dbQueryStatus.setData(song.getSongName());
	    
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		
	    Song song = db.findById(songId, Song.class);
	    
	    if(song == null) {
            return new DbQueryStatus("No song found in DB", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
	    }
	    
	    this.db.remove(song);
	    return new DbQueryStatus("Removed song from DB", DbQueryExecResult.QUERY_OK);
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		Song exist = db.findById(songId, Song.class);
		int num;
		if(exist == null) {
		    return new DbQueryStatus("No song found in DB", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		}
	    
		// you have 0 favourite and you write false 
		if(exist.getSongAmountFavourites() == 0 && shouldDecrement ) {
		    return new DbQueryStatus("Cannot have lower than 0 rating", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		
		DbQueryStatus dbQueryStatus = new DbQueryStatus("",DbQueryExecResult.QUERY_OK);
		if(shouldDecrement) {
		    dbQueryStatus.setMessage("Removed Favourite by 1");
            num = -1; 
		    
		    
		}
		else{
		    num = 1; 
            dbQueryStatus.setMessage("Added to favourite by 1");
		    
		}
		
		exist.setSongAmountFavourites(exist.getSongAmountFavourites() + num );
		this.db.findAndReplace(new Query(where("_id").is(songId)), exist);
		
		return dbQueryStatus;
		
	}
}