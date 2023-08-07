package ca.yorku.eecs;

import java.io.IOException;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;

import java.net.URI;
import java.util.*;
//new import for the new JSON handling
import org.json.*;

public class APIController implements HttpHandler {
	private static final String uri = "bolt://localhost:7687";
	private static final String user = "neo4j";
	private static final String password = "12345678";
	private static Utils uti = new Utils();
	private DBManager dbm = new DBManager(uri, user, password);

	public APIController() {}

	// HANDLE requests
	// -------------------------------------------------------------------
	@Override
	public void handle(HttpExchange request) throws IOException {
		// Check if request is a GET or PUT request
		try {
			if (request.getRequestMethod().equals("GET")) {
				handleGet(request);
			} else if (request.getRequestMethod().equals("POST")) {
				// TODO: Change to PUT
				handlePost(request);
			} else {
				uti.sendString(request, "Unimplemented method\n", 501);
			}
		} catch (Exception e) {
			e.printStackTrace();
			uti.sendString(request, "Server error\n", 500);
		}
	}

	public void handleGet(HttpExchange request) throws IOException {
		// Strip path context, save for last part of path
		URI uri = request.getRequestURI();
		String selectedMethod = uri.getPath().replace("/api/v1/", "");

		// Determine which method to call
		if (selectedMethod.equals("getActor")) {
			getActor(request);
		} else if (selectedMethod.equals("getMovie")) {
			getMovie(request);
		} else if (selectedMethod.equals("hasRelationship")) {
			hasRelationship(request);
		} else if (selectedMethod.equals("computeBaconNumber")) {
			computeBaconNumber(request);
		} else if (selectedMethod.equals("computeBaconPath")) {
			computeBaconPath(request);
		} else if (selectedMethod.equals("getMoviesByRating")) {
			getMoviesByRating(request);
		}

	}

	public void handlePost(HttpExchange request) throws IOException, JSONException {
		// TODO: I suggest changing this to handlePUT as mentioned in our project
		// instructions
		// Strip path context, save for last part of path
		URI uri = request.getRequestURI();
		String selectedMethod = uri.getPath().replace("/api/v1/", "");

		// Determine which method to call
		if (selectedMethod.equals("addActor")) {
			addActor(request);
		} else if (selectedMethod.equals("addMovie")) {
			addMovie(request);
		} else if (selectedMethod.equals("addRelationship")) {
			addRelationship(request);
		}
	}

	// GET Requests
	// All GET requests must return either a 200, 400, or 404 status code.
	// -------------------------------------------------------------------
	private void getActor(HttpExchange request) throws IOException {
		URI uri = request.getRequestURI();
		String query = uri.getQuery();

		if (query == null) {
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}

		Map<String, String> queryParam = uti.splitQuery(query);
		String actorId = queryParam.get("actorId");

		//Boolean actorExists = dbm.checkNodeExists("actor", actorId);
		if (!dbm.checkNodeExists("actor", actorId)) {
			uti.sendString(request, "NOT FOUND\n", 404);
			return;
		}
		if (actorId == null) uti.sendString(request, "BAD REQUEST\n", 400);
		else {
			String result = dbm.convertActorToJson(actorId.trim());
			uti.sendResponse(request, result, 200);
		}
	}

	private void getMovie(HttpExchange request) throws IOException {
		URI uri = request.getRequestURI();
		String query = uri.getQuery();

		if (query == null) {
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}

		Map<String, String> queryParam = uti.splitQuery(query);
		String movieId = queryParam.get("movieId");

		//Boolean movieExists = dbm.checkNodeExists("movie", movieId);
		if (!dbm.checkNodeExists("movie", movieId)) {
			uti.sendString(request, "NOT FOUND\n", 404);
			return;
		}
		if (movieId == null) uti.sendString(request, "BAD REQUEST\n", 400);
		else {
			String result = dbm.convertMovieToJson(movieId.trim());
			uti.sendResponse(request, result, 200);
		}

	}

	/*
	Method returns a list of movies with a rating equal to the rating provided
	End point requires the rating provided to be a string due to how the findJsonProperty method works
	TODO potentially change the logic for findJsonProperty to avoid String only returns
	 */
	public void getMoviesByRating(HttpExchange request) throws IOException {
		URI uri = request.getRequestURI();
		String query = uri.getQuery();
		
		String json = uti.getBody(request);
		String rating = uti.findJsonProperty(json, "rating");
		int ratingValidityCheck = uti.isNumeric(rating);

		if (ratingValidityCheck <= 0) {
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		} else {
			Integer a = Integer.parseInt(rating);
			List result = dbm.findMoviesByRating(a);

			uti.sendResponse(request, result.toString(), 200);
		}
	}

	private void hasRelationship(HttpExchange request) throws IOException {
		URI uri = request.getRequestURI();
		String query = uri.getQuery();

		if (query == null) {
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}

		Map<String, String> queryParam = uti.splitQuery(query);
		String actorId = queryParam.get("actorId");
		String movieId = queryParam.get("movieId");
		
		if (!dbm.checkNodeExists("movie", movieId) || !dbm.checkNodeExists("actor", actorId)) {
			uti.sendString(request, "NOT FOUND\n", 404);
			return;
		}
		if (actorId == null || movieId == null) uti.sendString(request, "BAD REQUEST\n", 400);
		else {
			String result = dbm.convertRelationshipToJson(actorId.trim(), movieId.trim());
			uti.sendResponse(request, result, 200);
		}
	}

	private void computeBaconNumber(HttpExchange request) throws IOException {
		URI uri = request.getRequestURI();
		String query = uri.getQuery();

		if (query == null) {
			// If the request body is improperly formatted or missing required information
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}

		Map<String, String> queryParam = uti.splitQuery(query);
		String actorId = queryParam.get("actorId");

		//check if actorId is exists
		Boolean actorExists = dbm.checkNodeExists("actor", actorId);
		if (!actorExists) {
			uti.sendString(request, "NOT FOUND\n", 404);
			return;
		}

		if (actorId == null) {
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		} else {
			// Get bacon number from DB, send 200 request for successful computation.
			String result = dbm.convertBaconNumberToJson(actorId);
			uti.sendResponse(request, result, 200);
		}

	}

	private void computeBaconPath(HttpExchange request) throws IOException {
		URI uri = request.getRequestURI();
		String query = uri.getQuery();

		if (query == null) {
			// If the request body is improperly formatted or missing required information
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}

		Map<String, String> queryParam = uti.splitQuery(query);
		String actorId = queryParam.get("actorId");

		//check if actorId is exists
		Boolean actorExists = dbm.checkNodeExists("actor", actorId);
		if (!actorExists) {
			uti.sendString(request, "NOT FOUND\n", 404);
			return;
		}

		if (actorId == null) {
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		} else {
			// Get bacon number from DB, send 200 request for successful computation.
			String result = dbm.convertBaconPathToJson(actorId);
			uti.sendResponse(request, result, 200);
		}
	}
	
	// PUT Requests
	// All PUT requests must return either a 200 or 400 status code
	// -------------------------------------------------------------------
	private void addActor(HttpExchange request) throws IOException, JSONException {

		//consider changing to JSONArray for easier validty checking of optional params like rating??
		//this will POTENTIALLY avoid a sever error when parsing for params that do not exist in the JSON object
		JSONObject sample;
		String json = uti.getBody(request);

		try{
			sample=new JSONObject(json);
		}
		catch (Exception e){
			uti.sendString(request, "BAD REQUEST 1\n", 400);
			return;
		}

		String name = sample.optString("name");
		String actorId = sample.optString("actorId");
		
		if (sample.isNull("name") || sample.isNull("actorId")) {

			// If the request body is improperly formatted or missing required information
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}

		//check if name or actorId is empty, this was not being checked before and must be done
		//after the null checks to avoid a null pointer exception
		else if(name.equals("")||actorId.equals("")) {
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}

		// Create actor node, with name and actorId
		Boolean addResult = dbm.createNodeWith2Props("actor", "name", name, "actorId", actorId);

		if (addResult) {
			// Successful add
			uti.sendString(request, "OK\n", 200);
		} else {
			// Duplicate node found
			uti.sendString(request, "BAD REQUEST\n", 400);
		}
		return;

	}

	//added in logic for rating
	//fixed the lack of empty string and null property checking, the majority of this logic can be moved over to Utils
	private void addMovie(HttpExchange request) throws IOException {
		// Extract and convert request

		Boolean providedRating=false;
		Boolean addResult;
		String json = uti.getBody(request);
		int rating=-1;
		JSONObject sample;

		//try catch for the JSONObject creation
		try{
			sample=new JSONObject(json);
		}
		catch (Exception e){
			uti.sendString(request, "BAD REQUEST 1\n", 400);
			return;
		}

		//must use the optString/optInt method to avoid a server error when parsing for params that do not exist in the JSON object
		String name = sample.optString("name");
		String movieId = sample.optString("movieId");

		if(sample.has("rating")){
			//checking for null
			if(!sample.isNull("rating")){
				rating = sample.optInt("rating");
				providedRating=true;
			}
		}

		//check if name or movie id are null, also check if rating IS provided and IS NOT valid
		if (sample.isNull("name") || sample.isNull("movieId")|| (rating<1||rating>5) && providedRating) {
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}
		//empty string handling
		else if(name.equals("")||movieId.equals("")) {
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}

		//checks if rating is not provided, calls 2prop node creation
		if(!providedRating) {
			 addResult = dbm.createNodeWith2Props("movie", "name", name, "movieId", movieId);
		}
		//used if rating is provided and valid
		//note that the rating will be stored as an int within the nod
		else {
			 addResult = dbm.createNodeWith3Props("movie", "name", name, "movieId", movieId, "rating", rating);
		}
		// Create movie node, with name and movieId

		if (addResult) {
			// Successful add
			uti.sendString(request, "OK\n", 200);
		} else {
			// Duplicate node found
			uti.sendString(request, "BAD REQUEST\n", 400);
		}
		return;

	}
	
	private void addRelationship(HttpExchange request) throws IOException {
		// Extract and convert request
		JSONObject sample;
		String json = uti.getBody(request);

		try{
			sample=new JSONObject(json);
		}
		catch (Exception e){
			uti.sendString(request, "BAD REQUEST 1\n", 400);
			return;
		}
		
		String actorId = sample.optString("actorId");
		String movieId = sample.optString("movieId");
		
		System.out.println("RELATIONSHIP\n-----\n" + json + "\n" + actorId + "\n" + movieId);
		
		
		/**
		if (movieId == null || actorId == null) {
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}**/
		
		if (sample.isNull("actorId") || sample.isNull("movieId")) {
			// If the request body is improperly formatted or missing required information
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}

		//check if name or actorId is empty, this was not being checked before and must be done
		//after the null checks to avoid a null pointer exception
		else if(actorId.equals("")||movieId.equals("")) {
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}
		
		if(!(dbm.checkNodeExists("actor", actorId) && dbm.checkNodeExists("movie", movieId))) {
			uti.sendString(request, "NOT FOUND\n", 404);
			return;
		}

		// Create relationship arrow, with actorId and movieId
		Boolean addResult = dbm.createRelationship(actorId, movieId);

		if (addResult) uti.sendString(request, "OK\n", 200);
		else uti.sendString(request, "BAD REQUEST\n", 400);
	}

}
