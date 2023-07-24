package ca.yorku.eecs;

import java.io.IOException;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;

import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

public class APIController implements HttpHandler {
	private static final String uri = "bolt://localhost:7687";
	private static final String user = "neo4j";
	private static final String password = "12345678";
	private static Utils uti = new Utils();
	private DBManager dbm = new DBManager(uri, user, password);

	public APIController() {
	}

	// Handling requests
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
		}

	}

	public void handlePost(HttpExchange request) throws IOException {
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
	// -------------------------------------------------------------------
	private void getActor(HttpExchange request) throws IOException {
		URI uri = request.getRequestURI();
		String query = uri.getQuery();

		if (query == null) {
			// If the request body is improperly formatted or missing required information
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}

		Map<String, String> queryParam = uti.splitQuery(query);
		String actorId = queryParam.get("actorId");

		if (actorId == null) {
			uti.sendString(request, "BAD REQUEST\n", 400);
			// return; (Commented out because function will return regardless)
		} else {
			// Get actor from DB, send 200 request for successful retrieval.
			String result = dbm.convertActorToJson(actorId.trim());
			uti.sendResponse(request, result, 200);
		}
	}

	private void getMovie(HttpExchange request) throws IOException {
		URI uri = request.getRequestURI();
		String query = uri.getQuery();

		if (query == null) {
			// If the request body is improperly formatted or missing required information
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}

		Map<String, String> queryParam = uti.splitQuery(query);
		String movieId = queryParam.get("movieId");

		if (movieId == null) {
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		} else {
			// Get movie from DB, send 200 request for successful retrieval.
			String result = dbm.convertMovieToJson(movieId.trim());
			uti.sendResponse(request, result, 200);
		}

	}

	private void hasRelationship(HttpExchange request) throws IOException {
		URI uri = request.getRequestURI();
		String query = uri.getQuery();

		if (query == null) {
			// If the request body is improperly formatted or missing required information
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}

		Map<String, String> queryParam = uti.splitQuery(query);
		String actorId = queryParam.get("actorId");
		String movieId = queryParam.get("movieId");

		if (actorId == null || movieId == null) {
			// TODO: Don't evaluate with movieID, actorId. Check if the either is in the
			// database instead by using previous methods implemented in other todo
			// statements
			// If there is no relationship in the database that exists with that movieId and
			// actorId
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		} else {
			// Get relationship from DB, send 200 request for successful retrieval.
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

		if (actorId == null) {
			// TODO: Don't evaluate with actorID. Use getActor todo implemented method
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		} else {
			// Get bacon number from DB, send 200 request for successful computation.
			String result = dbm.convertBaconNumberToJson(actorId);
			uti.sendResponse(request, result, 200);
		}

	}

	// PUT Requests
	// -------------------------------------------------------------------
	private void addActor(HttpExchange request) throws IOException {
		// Extract and convert request
		String json = uti.getBody(request);
		String name = uti.findJsonProperty(json, "name");
		String actorId = uti.findJsonProperty(json, "actorId");

		if (name == null || actorId == null) {
			// If the request body is improperly formatted or missing required information
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

	private void addMovie(HttpExchange request) throws IOException {
		// Extract and convert request
		String json = uti.getBody(request);
		String name = uti.findJsonProperty(json, "name");
		String actorId = uti.findJsonProperty(json, "movieId");

		if (name == null || actorId == null) {
			// If the request body is improperly formatted or missing required information
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}

		// Create movie node, with name and movieId
		Boolean addResult = dbm.createNodeWith2Props("movie", "name", name, "movieId", actorId);

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
		String json = uti.getBody(request);
		String actorId = uti.findJsonProperty(json, "actorId");
		String movieId = uti.findJsonProperty(json, "movieId");

		if (movieId == null || actorId == null) {
			// If the request body is improperly formatted or missing required information
			uti.sendString(request, "BAD REQUEST\n", 400);
			return;
		}

		// Create relationship arrow, with actorId and movieId
		Boolean addResult = dbm.createRelationship(actorId, movieId);

		if (addResult) {
			// Successful add
			uti.sendString(request, "OK\n", 200);
		} else {
			// Duplicate relationship found
			uti.sendString(request, "BAD REQUEST\n", 400);
		}
		return;

	}

}
