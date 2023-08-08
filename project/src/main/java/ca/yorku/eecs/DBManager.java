package ca.yorku.eecs;

import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.Record;

import java.util.*;

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

	// CREATION of Nodes/Relationships:
	// ---------------------------------------------------------------------
	public Boolean createNodeWith2Props(String label, String prop1Label, String prop1Value, String prop2Label, String prop2Value) {
		try (Session session = driver.session()) {
			if (hasDuplicate(prop2Label, prop2Value)) return false;

			String query = "CREATE (m:" + label + " {" + prop1Label + ": '" + prop1Value + "', " + prop2Label + ": '" + prop2Value + "'})";
			session.writeTransaction(tx -> tx.run(query));
			return true;
		}
	}

	/*
	Create a node with 3 properties, intended for use with the addMovie endpoint when
	providing a rating alongside a movie name and movie ID. The endpoint will only accept String values for a rating
	however the database stores the rating as an integer.
	 */
	public Boolean createNodeWith3Props(String label, String prop1Label, String prop1Value, String prop2Label, String prop2Value, String prop3Label, Integer prop3Value) {
		try (Session session = driver.session()) {
			if (hasDuplicate(prop2Label, prop2Value)) return false;

			String query = "CREATE (m: "+label +"{"+prop1Label+": '"+prop1Value+"', "+prop2Label+": '"+prop2Value+"', "+prop3Label+": "+prop3Value+"})";
			session.writeTransaction(tx -> tx.run(query));
			return true;
		}

	}

	public Boolean createRelationship(String actorId, String movieId) {
		try (Session session = driver.session()) {
			if (hasDuplicateRelationship(actorId, movieId)) return false;

			String query = "MATCH (a:actor),(m:movie) " + "WHERE a.actorId = '" + actorId + "' AND m.movieId = '" + movieId + "' " + "CREATE (a)-[r:ACTED_IN]->(m) RETURN type(r)";
			session.writeTransaction(tx -> tx.run(query));
			return true;
		}
	}

	// FIND Nodes/Relationships
	// ---------------------------------------------------------------------

	/*
	Generate a list of movie names based on the rating.
	Note that the rating input is an integer as the node property for ratings is stored as an integer, the endpoint
	that calls this method however will only accept values given as a string for the rating.
	 */
	public String findMoviesByRating(Integer rating) {
		//String query = "MATCH (n:" + label + ") WHERE n." + label + "Id = $" + label + "Id RETURN n";
		String query ="MATCH (m:movie) WHERE m.rating = " + rating + "  RETURN m";
		
		try (Session session = driver.session()) {
			Map<String, Object> params = new HashMap<>();
			params.put("rating", rating);
			
			StatementResult result = session.writeTransaction(tx -> tx.run(query, params));
			
			List<String> results = new LinkedList<>();
			while (result.hasNext()) {
				Record record = result.next();
				results.add("\"" + record.get("m").asMap().get("name").toString() + "\"");
			}
			return "{\"movieList\": " + results.toString() + "}";
		}
	}

	public List<String> /* convertActorToJson, convertMovieToJson, and convertRelationshipToJSON */ findRelationship(String label, String name) {
		String query = null;
		
		try (Session session = driver.session()) {
			if (label.equals("movie")) {
				query = "MATCH (n)-[r]->(m) WHERE n.name = $name RETURN n,r,m";
			} else if (label.equals("actor")) {
				query = "MATCH (n)-[r]->(m) WHERE m.name = $name RETURN n,r,m";
			}

			String finalQuery = query;
			StatementResult result = session.writeTransaction(tx -> tx.run(finalQuery, Values.parameters("name", name)));

			// Add associated nodes to results list
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

	public StatementResult /* convertActorToJson, convertMovieToJson, and convertRelationshipToJSON */ findByLabelAndId(String label, String nodeId) {
		String query = "MATCH (n:" + label + ") WHERE n." + label + "Id = $" + label + "Id RETURN n";
		
		try (Session session = driver.session()) {
			Map<String, Object> params = new HashMap<>();
			params.put(label + "Id", nodeId);
			params.put("label", label);

			StatementResult result = session.writeTransaction(tx -> tx.run(query, params));
			return result;
		}
	}
	
	// Duplicate/Existence Checkers:
	// ---------------------------------------------------------------------
	public Boolean /* createNodeWith2Props */ hasDuplicate(String label, String value) {
		String query = "MATCH (n) WHERE n." + label + " = '" + value + "' RETURN count(n) > 0 AS hasDuplicate";
		
		try (Session session = driver.session()) {
			StatementResult result = session.run(query);

			if (result.hasNext()) {
				Record record = result.next();
				return record.get("hasDuplicate").asBoolean();
			}

			return false;
		}
	}
	
	public Boolean /* createRelationship */ hasDuplicateRelationship(String actorId, String movieId) {
		String query = "MATCH (a:actor)-[r:ACTED_IN]->(m:movie) " + "WHERE a.actorId = '" + actorId + "' AND m.movieId = '" + movieId + "' " + "RETURN count(r) > 0 AS hasDuplicate";
		
		try (Session session = driver.session()) {
			StatementResult result = session.run(query);

			if (result.hasNext()) {
				Record record = result.next();
				return record.get("hasDuplicate").asBoolean();
			}

			return false; 
		}
	}

	public Boolean checkNodeExists(String label, String id){
		String query = "MATCH (n:" + label + ") WHERE n." + label + "Id = \"" + id + "\" RETURN count(n) AS count";
		
		try (Session session = driver.session()) {
			StatementResult result = session.run(query, Values.parameters(label, label+"Id"));

			if (result.hasNext()) {
				Record record = result.next();
				int count = record.get("count").asInt();
				return (count > 0);
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	// JSON Formatting
	// ---------------------------------------------------------------------
	public String /* APIController getActor */ convertActorToJson(String actorId) {
		StatementResult actor = findByLabelAndId("actor", actorId);
		String actorIdString = null;
		String nameString = null;

		while (actor.hasNext()) {
			Record record = actor.next();
			nameString = record.get("n").asMap().get("name").toString();
			actorIdString = record.get("n").asMap().get("actorId").toString();
		}
		String movies = findRelationship("movie", nameString).toString();

		return "{\"actorId\":\"" + actorIdString + "\",\"name\":\"" + nameString + "\",\"movies\":" + movies + "}";
	}

	public String convertMovieToJson(String movieId) {
		// Used in APIController getMovie method
		StatementResult movie = findByLabelAndId("movie", movieId);
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

	public String convertRelationshipToJson(String actorId, String movieId) {
		// Used in APIController hasRelationship method
		StatementResult actor = findByLabelAndId("actor", actorId);
		String nameString = null;

		while (actor.hasNext()) {
			// Retrieve actor record, extract name
			Record record = actor.next();
			nameString = record.get("n").asMap().get("name").toString();
		}

		// Get list of movies in String format
		String movies = findRelationship("movie", nameString).toString();
		movies = movies.substring(1, movies.length() - 1); // Remove the square brackets

		// Iterate through each movieId and find if actor has a relationship with movie
		Boolean hasRelationship = false;
		for (String movie : movies.split(", ")) {
			if (movie.equals("\"" + movieId + "\"")) {
				hasRelationship = true;
				break;
			}
		}

		// Return as JSON
		return "{\"actorId\":\"" + actorId + "\",\"movieId\":\"" + movieId + "\",\"hasRelationship\":" + hasRelationship
				+ "}";
	}

	public String convertBaconNumberToJson(String actorId) {
		String actorIdString = null;
		String baconId = "nm0000102";
		String baconNumber = "0";
		StatementResult actor = findByLabelAndId("actor", actorId);
		
		while (actor.hasNext()) {
			Record record = actor.next();
			actorIdString = record.get("n").asMap().get("actorId").toString();
		}
		

		if (!actorIdString.equals(baconId)) {
			try (Session session = driver.session()) {
				String query = "MATCH p=shortestPath((bacon:actor {actorId: 'nm0000102'})-[:ACTED_IN*]-(actor:actor {actorId: '" + actorIdString + "'}))\n" + "RETURN LENGTH(p)/2 AS bacon_number";
				StatementResult result = session.run(query);
				
				if(result.hasNext()) {
					Record recordResult = result.next();
					baconNumber = recordResult.values().get(0).toString();
					
					
					//TODO: This should be a 404 request, not a str. 
					//What I would do is make an if statement checking if the returned string is 404, and send a 404 request. we got not time for other shit
					if(baconNumber.equals("NULL")) {
						baconNumber = "404";
					}
				}
			}
		}
		return "{\"baconNumber\":" + baconNumber + "}";
	}

	public String convertBaconPathToJson(String actorId) {
		String baconId = "nm0000102";
		String baconPath = "";
		
		StatementResult actor = findByLabelAndId("actor", actorId);
		String actorIdString = null;
		while (actor.hasNext()) {
			Record record = actor.next();
			actorIdString = record.get("n").asMap().get("actorId").toString();
		}
		
		if (!actorIdString.equals(baconId)) {
			try (Session session = driver.session()) {
				String query = "MATCH (a:actor {actorId: '" + actorIdString + "'}), (b:actor {actorId: 'nm0000102'}), p = shortestPath((a)-[:ACTED_IN*]-(b))\n"
						+ "WITH p as p, nodes(p) as n\n"
						+ "RETURN [value in n | CASE WHEN 'actorId' IN keys(value) THEN value.actorId ELSE value.movieId END] as baconPath";
				StatementResult result = session.run(query);
				
				if (result.hasNext()) {
                    Record record = result.next();
                    baconPath = record.values().get(0).toString();
                    
                  //TODO: This should be a 404 request, not a str, like the baconNumber function
                    if (baconPath.equals("NULL")) {
                    	baconPath = "404";
                    }
                }
                return "{\"baconPath\": " + baconPath + "}";
			}
			
		}else {
			return "{\"baconPath\":[\"nm0000102\"]}";
		}
	}

}
