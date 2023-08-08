*** Settings ***
Library           Collections
Library           RequestsLibrary
Test Timeout      30 seconds

#this is the suite setup. This will run before any test cases in this file 
Suite Setup    Create Session    localhost    http://localhost:8080
*** Test Cases ***

#NOTE!!!!!!!!: before running test cases and after every run please DELETE ALL DATABASE ENTRIES
#this can be done by running 
# MATCH (n) DETACH DELETE n
# in neo4j browser
#deleting old entries will avoid unintentional error 400s due to duplicates
#the test below will run in order, please keep that in mind and run GET requests after POST requests

addActorPass
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Robert Downey Jr    actorId=nm123
    ${resp}=    POST On Session    localhost    /api/v1/addActor    json=${params}    headers=${headers}
    Should Be Equal As Strings    ${resp.status_code}    200

addActorFail
    #Null ID check
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Ryan Gosling  actorId=${null} 
    ${resp}=    Post Request    localhost    /api/v1/addActor    json=${params}    headers=${headers}
    Should Be Equal As Strings    ${resp.status_code}    400
    #Null Name check
    ${params}=    Create Dictionary    name=${null}  actorId=someId
    ${resp}=    Post Request    localhost    /api/v1/addActor    json=${params}    headers=${headers}
    Should Be Equal As Strings    ${resp.status_code}    400
    #Duplicate Check
    ${params}=    Create Dictionary    name=Another Actor  actorId=nm123 
    ${resp}=    Post Request    localhost    /api/v1/addActor    json=${params}    headers=${headers}
    Should Be Equal As Strings    ${resp.status_code}    400

addMoviePass
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Interstellar    movieId=m1
    ${resp}=    POST On Session    localhost    /api/v1/addMovie    json=${params}    headers=${headers}
    Should Be Equal As Strings    ${resp.status_code}    200

addMovieFail
    #Null ID check
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=An Invalid Movie  movieId=${null} 
    ${resp}=    Post Request    localhost    /api/v1/addMovie    json=${params}    headers=${headers}
    Should Be Equal As Strings    ${resp.status_code}    400
    #Null Name check
    ${params}=    Create Dictionary    name=${null}  movieId=someId
    ${resp}=    Post Request    localhost    /api/v1/addMovie    json=${params}    headers=${headers}
    Should Be Equal As Strings    ${resp.status_code}    400
    #Duplicate Check
    ${params}=    Create Dictionary    name=Another Actor  movieId=m1
    ${resp}=    Post Request    localhost    /api/v1/addMovie    json=${params}    headers=${headers}
    Should Be Equal As Strings    ${resp.status_code}    400

addRelationshipPass
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Ryan Gosling    actorId=nm001
    ${resp}=    POST On Session    localhost    /api/v1/addActor    json=${params}    headers=${headers}
    ${params}=    Create Dictionary    name=Barbie    movieId=m2
    ${resp}=    POST On Session    localhost    /api/v1/addMovie    json=${params}    headers=${headers}
    
    ${params}=    Create Dictionary    actorId=nm001    movieId=m2
    ${resp}=    POST On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}
    Should Be Equal As Strings    ${resp.status_code}    200

addRelationshipFail
    #Null movieID check
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=An Invalid ID  movieId=${null} 
    ${resp}=    Post Request    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}
    Should Be Equal As Strings    ${resp.status_code}    400
    #Null actorID check
    ${params}=    Create Dictionary    actorId=${null}  movieId=someId
    ${resp}=    Post Request    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}
    Should Be Equal As Strings    ${resp.status_code}    400
    #Duplicate Check
    ${params}=    Create Dictionary    actorId=nm001    movieId=m2
    ${resp}=    Post Request    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}
    Should Be Equal As Strings    ${resp.status_code}    400

    #Not Found Check
    ${params}=    Create Dictionary    actorId=nm2    movieId=m1 
    ${resp}=    Post Request    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}
    Should Be Equal As Strings    ${resp.status_code}    404

addMovieWithRatingPass
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Avengers Endgame  movieId=m3  rating=4
    ${resp}=    POST On Session    localhost    /api/v1/addMovie    json=${params}    headers=${headers}
    Should Be Equal As Strings    ${resp.status_code}    200

addMovieWithRatingFail
    #Incorrect rating check
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=The Lego Movie  movieId=m4  rating=-1
    ${resp}=    Post Request    localhost    /api/v1/addMovie    json=${params}    headers=${headers}
    Should Be Equal As Strings    ${resp.status_code}    400

getActorPass
    ${resp}=    GET Request    localhost    /api/v1/getActor?actorId=nm123
    
    # Check actorId
    Should Be Equal As Strings    ${resp.json()['actorId']}    nm123
    
    # Check name
    Should Be Equal As Strings    ${resp.json()['name']}    Robert Downey Jr
    
    # Check movies (an empty list)
    Should Be Empty    ${resp.json()['movies']}


getMoviePass
    ${resp}=    GET Request    localhost    /api/v1/getMovie?movieId=m1
    
    # Check movieId
    Should Be Equal As Strings    ${resp.json()['movieId']}    m1
    
    # Check name
    Should Be Equal As Strings    ${resp.json()['name']}    Interstellar
    
    # Check actors (an empty list)
    Should Be Empty    ${resp.json()['actors']}

hasRelationshipPass
    ${resp}=    GET Request    localhost    /api/v1/hasRelationship?actorId=nm001&movieId=m2
    
    # Check actorId
    Dictionary Should Contain Value    ${resp.json()}    nm001    actorId
    
    # Check movieId
    Dictionary Should Contain Value    ${resp.json()}    m2    movieId
    
    # Check hasRelationship
    Dictionary Should Contain Value    ${resp.json()}    ${true}    hasRelationship

computeBaconNumberPass
    # Setting up Bacon Graph
    # Creating Kevin Bacon
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Kevin Bacon    actorId=nm0000102
    ${resp}=    POST On Session    localhost    /api/v1/addActor    json=${params}    headers=${headers}
    
    # Creating Al Pacino, Keanu Reeves, and Hugo Weaving
    ${params}=    Create Dictionary    name=Al Pacino    actorId=nm00001
    ${resp}=    POST On Session    localhost    /api/v1/addActor    json=${params}    headers=${headers}
    ${params}=    Create Dictionary    name=Keanu Reeeves    actorId=nm00002
    ${resp}=    POST On Session    localhost    /api/v1/addActor    json=${params}    headers=${headers}
    ${params}=    Create Dictionary    name=Hugo Weaving    actorId=nm00003
    ${resp}=    POST On Session    localhost    /api/v1/addActor    json=${params}    headers=${headers}

    #Creating A Few Good Men, The Devil's Advocate, and The Matrix
    ${params}=    Create Dictionary    name=A Few Good Men   movieId=m0001
    ${resp}=    POST On Session    localhost    /api/v1/addMovie    json=${params}    headers=${headers}
    ${params}=    Create Dictionary    name=The Devils Advocate   movieId=m0002
    ${resp}=    POST On Session    localhost    /api/v1/addMovie    json=${params}    headers=${headers}
    ${params}=    Create Dictionary    name=The Matrix Trilogy   movieId=m0003
    ${resp}=    POST On Session    localhost    /api/v1/addMovie    json=${params}    headers=${headers}

    #Adding relationships
    ${params}=    Create Dictionary    actorId=nm0000102    movieId=m0001
    ${resp}=    POST On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}
    ${params}=    Create Dictionary    actorId=nm00001    movieId=m0001
    ${resp}=    POST On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}
    ${params}=    Create Dictionary    actorId=nm00001    movieId=m0002
    ${resp}=    POST On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}
    ${params}=    Create Dictionary    actorId=nm00002    movieId=m0002
    ${resp}=    POST On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}
    ${params}=    Create Dictionary    actorId=nm00002    movieId=m0003
    ${resp}=    POST On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}
    ${params}=    Create Dictionary    actorId=nm00003    movieId=m0003
    ${resp}=    POST On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}

    #Test each actor's Bacon Number
    ${resp}=    GET Request    localhost    /api/v1/computeBaconNumber?actorId=nm0000102
    Should Be Equal As Integers    ${resp.json()['baconNumber']}    0
    ${resp}=    GET Request    localhost    /api/v1/computeBaconNumber?actorId=nm00001
    Should Be Equal As Integers    ${resp.json()['baconNumber']}    1
    ${resp}=    GET Request    localhost    /api/v1/computeBaconNumber?actorId=nm00002
    Should Be Equal As Integers    ${resp.json()['baconNumber']}    2
    ${resp}=    GET Request    localhost    /api/v1/computeBaconNumber?actorId=nm00003
    Should Be Equal As Integers    ${resp.json()['baconNumber']}    3

computeBaconPathPass
    #Kevin Bacon + Some actor
    ${resp}=    GET Request    localhost    /api/v1/computeBaconPath?actorId=nm0000102
    List Should Contain Value    ${resp.json()['baconPath']}    nm0000102
    ${resp}=    GET Request    localhost    /api/v1/computeBaconPath?actorId=nm00003
    ${computed_path}=    Set Variable    ${resp.json()["baconPath"]}
    ${expected_path}=    Create List    nm00003    m0003    nm00002    m0002    nm00001    m0001    nm0000102
    List Should Contain Sub List    ${computed_path}    ${expected_path}

getMoviesByRatingPass
    ${resp}=    GET Request    localhost    /api/v1/getMoviesByRating?rating=4
    ${computed_path}=    Set Variable    ${resp.json()["movieList"]}
    ${expected_path}=    Create List    Avengers Endgame
    List Should Contain Sub List    ${computed_path}    ${expected_path}
    
