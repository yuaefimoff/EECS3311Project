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
    public APIController(){
    }

    @Override
    public void handle(HttpExchange request) throws IOException {
        try {
            if (request.getRequestMethod().equals("GET")) {
                handleGet(request);
            } else if(request.getRequestMethod().equals("POST")){ //Change to PUT
                handlePost(request);//handlePut
            }
            else {
                uti.sendString(request, "Unimplemented method\n", 501);
            }
        } catch (Exception e) {
            e.printStackTrace();
            uti.sendString(request, "Server error\n", 500);
        }
    }

    public void handleGet(HttpExchange request) throws IOException{
        URI uri = request.getRequestURI();
        String selectedMethod = uri.getPath().replace("/api/v1/", "");

        if (selectedMethod.equals("getActor")){
            getActor(request);
        }else if(selectedMethod.equals("getMovie")) {
            getMovie(request);
        }else if(selectedMethod.equals("hasRelationship")) {
            hasRelationship(request);
        }else if(selectedMethod.equals("computeBaconNumber")) {
        	computeBaconNumber(request);
        }

    }
    //Rearranged all get related requests to go under handleGet
    private void getActor(HttpExchange request) throws IOException {
        URI uri = request.getRequestURI();
        String query = uri.getQuery();
        if(query == null){
            uti.sendString(request, "BAD REQUEST\n", 400);
            return;
        }
        Map<String, String> queryParam = uti.splitQuery(query);
        String actorId = queryParam.get("actorId");
        if (actorId == null){
            uti.sendString(request, "BAD REQUEST\n", 400);
            return;
        }else {
            String result = dbm.convertActorToJson(actorId.trim());
            uti.sendResponse(request, result, 200);
        }

        //uti.sendString(request, "OK\n", 200);
    }

    private void getMovie(HttpExchange request) throws IOException{

        URI uri = request.getRequestURI();
        String query = uri.getQuery();
        if(query == null){
            uti.sendString(request, "BAD REQUEST\n", 400);
            return;
        }
        Map<String, String> queryParam = uti.splitQuery(query);
        String movieId = queryParam.get("movieId");
        if (movieId == null) {
            uti.sendString(request, "BAD REQUEST\n", 400);
            return;
        }else {
            String result = dbm.convertMovieToJson(movieId.trim());
            uti.sendResponse(request, result, 200);
        }

    }

    private void hasRelationship(HttpExchange request) throws IOException {
        URI uri = request.getRequestURI();
        String query = uri.getQuery();
        if(query == null){
            uti.sendString(request, "BAD REQUEST\n", 400);
            return;
        }
        Map<String, String> queryParam = uti.splitQuery(query);
        String actorId = queryParam.get("actorId");
        String movieId = queryParam.get("movieId");
        if (actorId == null || movieId == null) {
            uti.sendString(request, "BAD REQUEST\n", 400);
            return;
        }else {
            String result = dbm.hasRelationship(actorId.trim(), movieId.trim());
            uti.sendResponse(request, result, 200);
        }
    }
    
    private void computeBaconNumber(HttpExchange request) throws IOException {
        URI uri = request.getRequestURI();
        String query = uri.getQuery();
        if(query == null){
            uti.sendString(request, "BAD REQUEST\n", 400);
            return;
        }
        //Extract actor ID from query
        Map<String, String> queryParam = uti.splitQuery(query);
        String actorId = queryParam.get("actorId");
        
        /**
        if (actorId == null){
            uti.sendString(request, "BAD REQUEST\n", 400);
            return;
        }else {
            String result = dbm.convertActorToJson(actorId.trim());
            uti.sendResponse(request, result, 200);
        }**/
        
        
        if (actorId == null){
            uti.sendString(request, "NOT FOUND\n", 404);
            return;
        }else {
        	String result = dbm.calculateBaconNumber(actorId);
            uti.sendResponse(request, result, 200);
        }

    }
    
    //I suggest changing this to handlePUT as mentioned in our project instructions
    public void handlePost(HttpExchange request) throws IOException{
        URI uri = request.getRequestURI();
        String selectedMethod = uri.getPath().replace("/api/v1/", "");

        if (selectedMethod.equals("addActor")){
            addActor(request);
        }else if(selectedMethod.equals("addMovie")) {
            addMovie(request);
        }else if(selectedMethod.equals("addRelationship")) {
            addRelationship(request);
        }
    }
    
    private void addActor(HttpExchange request) throws IOException {
        String json = uti.getBody(request);
        String name = uti.findJsonProperty(json, "name");
        String actorId = uti.findJsonProperty(json, "actorId");
        if (name == null || actorId == null){
            uti.sendString(request, "BAD REQUEST\n", 400);
            return;
        }

        Boolean addResult = dbm.createNodeWith2Props("actor", "name",name,"actorId", actorId);

        if (addResult){
            uti.sendString(request, "OK\n", 200);
        }else{
            uti.sendString(request, "BAD REQUEST\n", 400);
        }
        return;

    }

    private void addMovie(HttpExchange request) throws IOException {
        String json = uti.getBody(request);
        String name = uti.findJsonProperty(json, "name");
        String actorId = uti.findJsonProperty(json, "movieId");
        if (name == null || actorId == null){
            uti.sendString(request, "BAD REQUEST\n", 400);
            return;
        }

        Boolean addResult = dbm.createNodeWith2Props("movie", "name",name,"movieId", actorId);

        if (addResult){
            uti.sendString(request, "OK\n", 200);
        }else{
            uti.sendString(request, "BAD REQUEST\n", 400);
        }
        return;

    }

    private void addRelationship(HttpExchange request) throws IOException {
        String json = uti.getBody(request);
        String actorId = uti.findJsonProperty(json, "actorId");
        String movieId = uti.findJsonProperty(json, "movieId");
        if (movieId == null || actorId == null){
            uti.sendString(request, "BAD REQUEST\n", 400);
            return;
        }

        Boolean addResult = dbm.createRelationship(actorId, movieId);

        if (addResult){
            uti.sendString(request, "OK\n", 200);
        }else{
            uti.sendString(request, "BAD REQUEST\n", 400);
        }
        return;

    }




}
