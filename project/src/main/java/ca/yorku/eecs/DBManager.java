package ca.yorku.eecs;

import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.Record;

import java.util.HashMap;
import java.util.HashSet;//Newly added import
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;//Newly added import
import java.util.Set;//Newly added import

import static org.neo4j.driver.v1.Config.build;

public class DBManager implements AutoCloseable {
	private final Driver driver;

	public DBManager(String uri, String user, String password) {
		// Establish connection to Neo4J DB with driver
		Config.ConfigBuilder builder = build();
		builder.withEncryption().toConfig();
		Config config = builder.withoutEncryption().toConfig();
		driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password), config);
	}

	@Override
	public void close() throws RuntimeException {
		driver.close();
	}

	// CREATION of Nodes/ Relationships:
	// ---------------------------------------------------------------------
	public Boolean createNodeWith2Props(String label, String prop1Label, String prop1Value, String prop2Label,
			String prop2Value) {
		try (Session session = driver.session()) {

			if (hasDuplicate(prop2Label, prop2Value)) {
				// TODO: This should only check for duplicate Id rather than both name and Id
				return false;
			}

			// Create and send Cypher query to DB
			String query = "CREATE (m:" + label + " {" + prop1Label + ": '" + prop1Value + "', " + prop2Label + ": '"
					+ prop2Value + "'})";
			session.writeTransaction(tx -> tx.run(query));

			return true;
		}

	}

	public Boolean createRelationship(String actorId, String movieId) {
		try (Session session = driver.session()) {
			if (hasDuplicateRelationship(actorId, movieId)) {
				// Check for duplicate relationship
				return false;
			}

			// Create and send Cypher query to DB
			String query = "MATCH (a:actor),(m:movie) " + "WHERE a.actorId = '" + actorId + "' AND m.movieId = '"
					+ movieId + "' " + "CREATE (a)-[r:ACTED_IN]->(m) RETURN type(r)";

			session.writeTransaction(tx -> tx.run(query));

			return true;
		}
	}

	// Duplicate Checkers
	// ---------------------------------------------------------------------
	public Boolean hasDuplicate(String label, String value) {
		try (Session session = driver.session()) {
			String query = "MATCH (n) WHERE n." + label + " = '" + value + "' RETURN count(n) > 0 AS hasDuplicate";

			StatementResult result = session.run(query);

			if (result.hasNext()) {
				Record record = result.next();
				return record.get("hasDuplicate").asBoolean();
			}

			return false; // No duplicate with the same name found
		}
	}

	public Boolean hasDuplicateRelationship(String actorId, String movieId) {
		try (Session session = driver.session()) {
			String query = "MATCH (a:actor)-[r:ACTED_IN]->(m:movie) " + "WHERE a.actorId = '" + actorId
					+ "' AND m.movieId = '" + movieId + "' " + "RETURN count(r) > 0 AS hasDuplicate";

			StatementResult result = session.run(query);

			if (result.hasNext()) {
				Record record = result.next();
				return record.get("hasDuplicate").asBoolean();
			}

			return false; // No duplicate with the same name found
		}
	}

	public List<String> findRelationship(String label, String name) {
		try (Session session = driver.session()) {
			String query = null;

			if (label.equals("movie")) {
				query = "MATCH (n)-[r]->(m) WHERE n.name = $name RETURN n,r,m";
			} else if (label.equals("actor")) {
				query = "MATCH (n)-[r]->(m) WHERE m.name = $name RETURN n,r,m";
			}
			String finalQuery = query;
			StatementResult result = session
					.writeTransaction(tx -> tx.run(finalQuery, Values.parameters("name", name)));
			List<String> results = new LinkedList<>();
			while (result.hasNext()) {
				Record record = result.next();
				if (label.equals("movie")) {
					results.add("\"" + record.get("m").asMap().get(label + "Id").toString() + "\"");
				} else if (label.equals("actor")) {
					results.add("\"" + record.get("n").asMap().get(label + "Id").toString() + "\"");
				}
			}
			return results;
		}

	}

	public StatementResult searchByLabelAndId(String label, String actorId) {
		try (Session session = driver.session()) {
			Map<String, Object> params = new HashMap<>();
			params.put(label + "Id", actorId);
			params.put("label", label);

			String query = "MATCH (n:" + label + ") WHERE n." + label + "Id = $" + label + "Id RETURN n";
			StatementResult result = session.writeTransaction(tx -> tx.run(query, params));
			return result;
		}
	}

	// JSON Formatting
	// ---------------------------------------------------------------------
	public String convertActorToJson(String actorId) {
		// Used in APIController getActor method
		StatementResult actor = searchByLabelAndId("actor", actorId);
		String actorIdString = null;
		String nameString = null;

		while (actor.hasNext()) {
			// Retrieve actor record, extract name and ID
			Record record = actor.next();
			nameString = record.get("n").asMap().get("name").toString();
			actorIdString = record.get("n").asMap().get("actorId").toString();
		}

		// Return list of movies in String format
		String movies = findRelationship("movie", nameString).toString();

		return "{\"actorId\":\"" + actorIdString + "\",\"name\":\"" + nameString + "\",\"movies\":" + movies + "}";
	}

	public String convertMovieToJson(String movieId) {
		// Used in APIController getMovie method
		StatementResult movie = searchByLabelAndId("movie", movieId);
		String movieIdString = null;
		String nameString = null;

		while (movie.hasNext()) {
			// Retrieve movie record, extract name and ID
			Record record = movie.next();
			nameString = record.get("n").asMap().get("name").toString();
			movieIdString = record.get("n").asMap().get("movieId").toString();
		}

		// Return list of actors in String format
		String actors = findRelationship("actor", nameString).toString();
		return "{\"movieId\":\"" + movieIdString + "\",\"name\":\"" + nameString + "\",\"actors\":" + actors + "}";
	}

	public String convertRelationshipToJSON(String actorId, String movieId) {
		// Used in APIController hasRelationship method
		StatementResult actor = searchByLabelAndId("actor", actorId);
		String nameString = null;

		while (actor.hasNext()) {
			// Retrieve actor record, extract name
			Record record = actor.next();
			nameString = record.get("n").asMap().get("name").toString();
		}

		// Get list of movies in String format
		String movies = findRelationship("movie", nameString).toString();
		movies = movies.substring(1, movies.length() - 1); // Remove the square brackets
		// System.out.println(movies);

		// Iterate through each movieId and find if actor has a relationship with movie
		Boolean hasRelationship = false;
		for (String movie : movies.split(", ")) {
			System.out.println(movie);
			if (movie.equals("\"" + movieId + "\"")) {
				hasRelationship = true;
				break;
			}
		}
		
		//Return as JSON
		return "{\"actorId\":\"" + actorId + "\",\"movieId\":\"" + movieId + "\",\"hasRelationship\":" + hasRelationship
				+ "}";
	}

	// NEW ADDED BY ME
	// ---------------------------------------------------------------------
	public String calculateBaconNumber(String actorId) {
		int baconNumber = 0;
		if (actorId != "nm0000102") {
			try (Session session = driver.session()) {
				String query = "MATCH p=shortestPath((bacon:actor {actorId: 'nm0000102'})-[:ACTED_IN*]-(actor:actor {actorId: '"
						+ actorId + "'}))\n" + "RETURN LENGTH(p)/2 AS bacon_number";

				StatementResult result = session.run(query);
				baconNumber = result.single().get("bacon_number").asInt();

			}
		}
		return "{\"baconNumber\":" + baconNumber + "}";
	}

}
