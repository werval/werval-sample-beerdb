/*
 * Copyright (c) 2013-2014 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package beerdb;

import beerdb.values.Page;
import io.werval.test.WervalHttpRule;
import org.junit.ClassRule;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static io.werval.api.http.Headers.Names.LOCATION;
import static io.werval.api.mime.MimeTypesNames.APPLICATION_JSON;
import static io.werval.api.mime.MimeTypesNames.TEXT_PLAIN;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

/**
 * Assert HTTP API Behaviour.
 */
public class HttpApiTest
{
    @ClassRule
    public static WervalHttpRule WERVAL = new WervalHttpRule();

    @Test
    public void testIndex()
    {
        when().get( "/api" )
            .then().statusCode( 200 )
            .and().contentType( APPLICATION_JSON );
    }

    @Test
    public void testInvalidHashids()
    {
        when().get( "/api/breweries/whatever" ).then().statusCode( 404 );
        when().get( "/api/beers/whatever" ).then().statusCode( 404 );
    }

    @Test
    public void testNotFound()
    {
        String hashid = WERVAL.application().crypto().hashids().encodeToString( 123L );
        when().get( "/api/breweries/" + hashid ).then().statusCode( 404 );
        when().get( "/api/beers/" + hashid ).then().statusCode( 404 );
    }

    @Test
    public void testCreateBreweriesBadRequest()
    {
        given().contentType( TEXT_PLAIN )
            .body( "{ \"name\":\"Wow it's a name\", \"url\":\"http://zeng-beers.com/\" }" )
            .when().post( "/api/breweries" )
            .then().statusCode( 400 )
            .and().body( containsString( "content-type" ) );

        given().contentType( APPLICATION_JSON )
            .body( "BAD PAYLOAD" )
            .when().post( "/api/breweries" )
            .then().statusCode( 400 )
            .and().body( containsString( "Unrecognized token" ) );

        given().contentType( APPLICATION_JSON )
            .body( "{}" )
            .when().post( "/api/breweries" )
            .then().statusCode( 400 )
            .and().body( containsString( "name" ) );

        given().contentType( APPLICATION_JSON )
            .body( "{ \"url\":\"http://zeng-beers.com/\" }" )
            .when().post( "/api/breweries" )
            .then().statusCode( 400 )
            .and().body( containsString( "name" ) );

        given().contentType( APPLICATION_JSON )
            .body( "{ \"name\":\"Wow it's a name\" }" )
            .when().post( "/api/breweries" )
            .then().statusCode( 400 )
            .and().body( containsString( "url" ) );

        given().contentType( APPLICATION_JSON )
            .body( "{ \"name\":\"\", \"url\":\"http://zeng-beers.com/\", \"description\":\"\" }" )
            .when().post( "/api/breweries" )
            .then().statusCode( 400 )
            .and().body( containsString( "name" ) );

        given().contentType( APPLICATION_JSON )
            .body( "{ \"name\":\"Wow it's a name\", \"url\":\"But this is not an URL\", \"description\":\"\" }" )
            .when().post( "/api/breweries" )
            .then().statusCode( 400 )
            .and().body( containsString( "url" ) );

        given().contentType( APPLICATION_JSON )
            .body( "{ \"name\":\"a  \", \"url\":\"But this is not an URL\", \"description\":\"\" }" )
            .when().post( "/api/breweries" )
            .then().statusCode( 400 )
            .and().body( containsString( "name" ) )
            .and().body( containsString( "URL" ) );

        // Malicious HTML input
        given().contentType( APPLICATION_JSON )
            .body( "{ \"name\":\"Zeng<a>Brewery\", \"url\":\"http://zeng-beers.com/\", \"description\":\"\\uFE64script\\uFE65alert('powned');\\uFE64/script\\uFE65\" }" )
            .when().post( "/api/breweries" )
            .then().statusCode( 400 )
            .and().body( containsString( "name" ) )
            .and().body( containsString( "description" ) );
    }

    @Test
    public void testCreateBeerBadRequest()
    {
        String breweryUrl = given().contentType( APPLICATION_JSON )
            .body( "{ \"name\":\"ZengBrewery\", \"url\":\"http://zeng-beers.com/\", \"description\":\"\" }" )
            .when().post( "/api/breweries" )
            .then().statusCode( 201 )
            .extract().header( LOCATION );

        String breweryId = when().get( breweryUrl )
            .then().statusCode( 200 )
            .and().contentType( APPLICATION_JSON )
            .extract().body().jsonPath().getString( "id" );

        // Missing Brewery
        given().contentType( APPLICATION_JSON )
            .body( "{ \"name\":\"ZengBeer\", \"abv\": 4.5 }" )
            .when().post( "/api/beers" )
            .then().statusCode( 400 )
            .and().body( containsString( "brewery" ) );

        // Missing Name
        given().contentType( APPLICATION_JSON )
            .body( "{ \"brewery_id\": \"" + breweryId + "\",\"abv\": 4.5 }" )
            .when().post( "/api/beers" )
            .then().statusCode( 400 )
            .and().body( containsString( "name" ) );

        // Missing ABV
        given().contentType( APPLICATION_JSON )
            .body( "{ \"brewery_id\": \"" + breweryId + "\", \"name\":\"ZengBeer\" }" )
            .when().post( "/api/beers" )
            .then().statusCode( 400 )
            .and().body( containsString( "abv" ) );

        // Invalid ABV
        given().contentType( APPLICATION_JSON )
            .body( "{ \"brewery_id\": \"" + breweryId + "\", \"name\":\"ZengBeer\", \"abv\": -1, \"description\":\"\" }" )
            .when().post( "/api/beers" )
            .then().statusCode( 400 )
            .and().body( containsString( "abv" ) );

        // Invalid ABV
        given().contentType( APPLICATION_JSON )
            .body( "{ \"brewery_id\": \"" + breweryId + "\", \"name\":\"ZengBeer\", \"abv\": 101, \"description\":\"\" }" )
            .when().post( "/api/beers" )
            .then().statusCode( 400 )
            .and().body( containsString( "abv" ) );

        // Malicious HTML input
        given().contentType( APPLICATION_JSON )
            .body( "{ \"brewery_id\": \"" + breweryId + "\", \"name\":\"Zeng<a>Beer\", \"abv\": 101, \"description\":\"\\uFE64script\\uFE65alert('powned');\\uFE64/script\\uFE65\" }" )
            .when().post( "/api/beers" )
            .then().statusCode( 400 )
            .and().body( containsString( "name" ) )
            .and().body( containsString( "description" ) );
    }

    @Test
    public void testCreateListDeleteBreweries()
    {
        int breweriesAtStart = (int) when().get( "/api/breweries" )
            .then().statusCode( 200 )
            .and().contentType( APPLICATION_JSON )
            .extract().as( Page.class ).getTotal();

        String breweryUrl = given().contentType( APPLICATION_JSON )
            .body( "{ \"name\":\"ZengBrewery\", \"url\":\"http://zeng-beers.com/\", \"description\":\"\" }" )
            .when().post( "/api/breweries" )
            .then().statusCode( 201 )
            .extract().header( LOCATION );

        when().get( breweryUrl )
            .then().statusCode( 200 )
            .and().contentType( APPLICATION_JSON )
            .and().body( "name", equalTo( "ZengBrewery" ) );

        when().get( "/api/breweries" )
            .then().statusCode( 200 )
            .and().contentType( APPLICATION_JSON )
            .and().body( "total", is( breweriesAtStart + 1 ) )
            .and().body( "list", hasSize( breweriesAtStart + 1 ) )
            .and().body( "list.name", hasItems( "ZengBrewery" ) );

        when().delete( breweryUrl ).then().statusCode( 200 );

        when().get( "/api/breweries" )
            .then().statusCode( 200 )
            .and().contentType( APPLICATION_JSON )
            .and().body( "total", is( breweriesAtStart ) )
            .and().body( "list", hasSize( breweriesAtStart ) );
    }

    @Test
    public void testCreateBreweryAndBeerThenDeleteThem()
    {
        String breweryUrl = given().contentType( APPLICATION_JSON )
            .body( "{ \"name\":\"ZengBrewery\", \"url\":\"http://zeng-beers.com/\", \"description\":\"\" }" )
            .when().post( "/api/breweries" )
            .then().statusCode( 201 )
            .extract().header( LOCATION );

        String breweryId = when().get( breweryUrl )
            .then().statusCode( 200 )
            .and().contentType( APPLICATION_JSON )
            .extract().body().jsonPath().getString( "id" );

        String beerUrl = given().contentType( APPLICATION_JSON )
            .body( "{ \"brewery_id\": \"" + breweryId + "\", \"name\":\"ZengBeer\", \"abv\": 4.5, \"description\":\"\" }" )
            .when().post( "/api/beers" )
            .then().statusCode( 201 )
            .extract().header( LOCATION );

        when().get( beerUrl )
            .then().statusCode( 200 )
            .and().contentType( APPLICATION_JSON )
            .and().body( "name", equalTo( "ZengBeer" ) );

        when().delete( breweryUrl ).then().statusCode( 409 );
        when().delete( beerUrl ).then().statusCode( 200 );
        when().delete( breweryUrl ).then().statusCode( 200 );
    }

    @Test
    public void testCreateUpdateBreweryAndBeer()
    {
        String breweryUrl = given().contentType( APPLICATION_JSON )
            .body( "{ \"name\":\"ZengBrewery\", \"url\":\"http://zeng-beers.com/\", \"description\":\"\" }" )
            .when().post( "/api/breweries" )
            .then().statusCode( 201 )
            .extract().header( LOCATION );

        String breweryId = when().get( breweryUrl )
            .then().statusCode( 200 )
            .and().contentType( APPLICATION_JSON )
            .extract().body().jsonPath().getString( "id" );

        String beerUrl = given().contentType( APPLICATION_JSON )
            .body( "{ \"brewery_id\": \"" + breweryId + "\", \"name\":\"ZengBeer\", \"abv\": 4.5, \"description\":\"\" }" )
            .when().post( "/api/beers" )
            .then().statusCode( 201 )
            .extract().header( LOCATION );

        // Edit Brewery
        given().contentType( APPLICATION_JSON )
            .body( "{ \"name\":\"ZengBrewery EDITED\", \"url\":\"http://zeng-beers.com/EDITED\", \"description\":\"\" }" )
            .when().put( breweryUrl )
            .then().statusCode( 200 );

        when().get( breweryUrl )
            .then().statusCode( 200 )
            .and().contentType( APPLICATION_JSON )
            .and().body( "name", endsWith( "EDITED" ) )
            .and().body( "url", endsWith( "EDITED" ) );

        // Edit Beer
        given().contentType( APPLICATION_JSON )
            .body( "{  \"name\":\"ZengBeer EDITED\", \"abv\": 45, \"description\":\"\" }" )
            .when().put( beerUrl )
            .then().statusCode( 200 );

        when().get( beerUrl )
            .then().statusCode( 200 )
            .and().contentType( APPLICATION_JSON )
            .and().body( "name", endsWith( "EDITED" ) )
            .and().body( "abv", equalTo( 45F ) );
    }
}
