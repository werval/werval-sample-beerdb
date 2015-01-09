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
import com.jayway.restassured.response.Response;
import io.werval.test.WervalHttpRule;
import org.junit.ClassRule;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
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
        expect().
            statusCode( 200 ).
            contentType( APPLICATION_JSON ).
            when().
            get( "/api" );
    }

    @Test
    public void testInvalidHashids()
    {
        expect().
            statusCode( 404 ).
            when().
            get( "/api/breweries/whatever" );

        expect().
            statusCode( 404 ).
            when().
            get( "/api/beers/whatever" );
    }

    @Test
    public void testNotFound()
    {
        String hashid = WERVAL.application().crypto().hashids().encodeToString( 123L );
        expect().
            statusCode( 404 ).
            when().
            get( "/api/breweries/" + hashid );

        expect().
            statusCode( 404 ).
            when().
            get( "/api/beers/" + hashid );
    }

    @Test
    public void testCreateBreweriesBadRequest()
    {
        given().
            contentType( TEXT_PLAIN ).
            body( "{ \"name\":\"Wow it's a name\", \"url\":\"http://zeng-beers.com/\" }" ).
            expect().
            statusCode( 400 ).
            body( containsString( "content-type" ) ).
            when().
            post( "/api/breweries" );

        given().
            contentType( APPLICATION_JSON ).
            body( "BAD PAYLOAD" ).
            expect().
            statusCode( 400 ).
            body( containsString( "Unrecognized token" ) ).
            when().
            post( "/api/breweries" );

        given().
            contentType( APPLICATION_JSON ).
            body( "{}" ).
            expect().
            statusCode( 400 ).
            body( containsString( "name" ) ).
            when().
            post( "/api/breweries" );

        given().
            contentType( APPLICATION_JSON ).
            body( "{ \"url\":\"http://zeng-beers.com/\" }" ).
            expect().
            statusCode( 400 ).
            body( containsString( "name" ) ).
            when().
            post( "/api/breweries" );

        given().
            contentType( APPLICATION_JSON ).
            body( "{ \"name\":\"Wow it's a name\" }" ).
            expect().
            statusCode( 400 ).
            body( containsString( "url" ) ).
            when().
            post( "/api/breweries" );

        given().
            contentType( APPLICATION_JSON ).
            body( "{ \"name\":\"\", \"url\":\"http://zeng-beers.com/\", \"description\":\"\" }" ).
            expect().
            statusCode( 400 ).
            body( containsString( "name" ) ).
            when().
            post( "/api/breweries" );

        given().
            contentType( APPLICATION_JSON ).
            body( "{ \"name\":\"Wow it's a name\", \"url\":\"But this is not an URL\", \"description\":\"\" }" ).
            expect().
            statusCode( 400 ).
            body( containsString( "url" ) ).
            when().
            post( "/api/breweries" );

        given().
            contentType( APPLICATION_JSON ).
            body( "{ \"name\":\"a  \", \"url\":\"But this is not an URL\", \"description\":\"\" }" ).
            expect().
            statusCode( 400 ).
            body( containsString( "name" ) ).
            body( containsString( "URL" ) ).
            when().
            post( "/api/breweries" );

        // Malicious HTML input
        given().
            contentType( APPLICATION_JSON ).
            body( "{ \"name\":\"Zeng<a>Brewery\", \"url\":\"http://zeng-beers.com/\", \"description\":\"\\uFE64script\\uFE65alert('powned');\\uFE64/script\\uFE65\" }" ).
            expect().
            statusCode( 400 ).
            body( containsString( "name" ) ).
            body( containsString( "description" ) ).
            when().
            post( "/api/breweries" );
    }

    @Test
    public void testCreateBeerBadRequest()
    {
        Response response = given().
            contentType( APPLICATION_JSON ).
            body( "{ \"name\":\"ZengBrewery\", \"url\":\"http://zeng-beers.com/\", \"description\":\"\" }" ).
            expect().
            statusCode( 201 ).
            when().
            post( "/api/breweries" );

        String breweryUrl = response.header( LOCATION );

        response = expect().
            statusCode( 200 ).
            contentType( APPLICATION_JSON ).
            when().
            get( breweryUrl );

        String breweryId = response.body().jsonPath().getString( "id" );

        // Missing Brewery
        given().
            contentType( APPLICATION_JSON ).
            body( "{ \"name\":\"ZengBeer\", \"abv\": 4.5 }" ).
            expect().
            statusCode( 400 ).
            body( containsString( "brewery" ) ).
            when().
            post( "/api/beers" );

        // Missing Name
        given().
            contentType( APPLICATION_JSON ).
            body( "{ \"brewery_id\": \"" + breweryId + "\",\"abv\": 4.5 }" ).
            expect().
            statusCode( 400 ).
            body( containsString( "name" ) ).
            when().
            post( "/api/beers" );

        // Missing ABV
        given().
            contentType( APPLICATION_JSON ).
            body( "{ \"brewery_id\": \"" + breweryId + "\", \"name\":\"ZengBeer\" }" ).
            expect().
            statusCode( 400 ).
            body( containsString( "abv" ) ).
            when().
            post( "/api/beers" );

        // Invalid ABV
        given().
            contentType( APPLICATION_JSON ).
            body( "{ \"brewery_id\": \"" + breweryId + "\", \"name\":\"ZengBeer\", \"abv\": -1, \"description\":\"\" }" ).
            expect().
            statusCode( 400 ).
            body( containsString( "abv" ) ).
            when().
            post( "/api/beers" );

        // Invalid ABV
        given().
            contentType( APPLICATION_JSON ).
            body( "{ \"brewery_id\": \"" + breweryId + "\", \"name\":\"ZengBeer\", \"abv\": 101, \"description\":\"\" }" ).
            expect().
            statusCode( 400 ).
            body( containsString( "abv" ) ).
            when().
            post( "/api/beers" );

        // Malicious HTML input
        given().
            contentType( APPLICATION_JSON ).
            body( "{ \"brewery_id\": \"" + breweryId + "\", \"name\":\"Zeng<a>Beer\", \"abv\": 101, \"description\":\"\\uFE64script\\uFE65alert('powned');\\uFE64/script\\uFE65\" }" ).
            expect().
            statusCode( 400 ).
            body( containsString( "name" ) ).
            body( containsString( "description" ) ).
            when().
            post( "/api/beers" );
    }

    @Test
    public void testCreateListDeleteBreweries()
    {
        int breweriesAtStart = (int) expect()
            .statusCode( 200 )
            .contentType( APPLICATION_JSON )
            .when()
            .get( "/api/breweries" )
            .as( Page.class )
            .getTotal();

        Response response = given().
            contentType( APPLICATION_JSON ).
            body( "{ \"name\":\"ZengBrewery\", \"url\":\"http://zeng-beers.com/\", \"description\":\"\" }" ).
            expect().
            statusCode( 201 ).
            when().
            post( "/api/breweries" );

        String breweryUrl = response.header( LOCATION );

        expect().
            statusCode( 200 ).
            contentType( APPLICATION_JSON ).
            body( "name", equalTo( "ZengBrewery" ) ).
            when().
            get( breweryUrl );

        expect().
            statusCode( 200 ).
            contentType( APPLICATION_JSON ).
            body( "total", is( breweriesAtStart + 1 ) ).
            body( "list", hasSize( breweriesAtStart + 1 ) ).
            body( "list.name", hasItems( "ZengBrewery" ) ).
            when().
            get( "/api/breweries" );

        expect().
            statusCode( 200 ).
            when().
            delete( breweryUrl );

        expect().
            statusCode( 200 ).
            contentType( APPLICATION_JSON ).
            body( "total", is( breweriesAtStart ) ).
            body( "list", hasSize( breweriesAtStart ) ).
            when().
            get( "/api/breweries" );
    }

    @Test
    public void testCreateBreweryAndBeerThenDeleteThem()
    {
        Response response = given().
            contentType( APPLICATION_JSON ).
            body( "{ \"name\":\"ZengBrewery\", \"url\":\"http://zeng-beers.com/\", \"description\":\"\" }" ).
            expect().
            statusCode( 201 ).
            when().
            post( "/api/breweries" );

        String breweryUrl = response.header( LOCATION );

        response = expect().
            statusCode( 200 ).
            contentType( APPLICATION_JSON ).
            when().
            get( breweryUrl );

        String breweryId = response.body().jsonPath().getString( "id" );

        response = given().
            contentType( APPLICATION_JSON ).
            body( "{ \"brewery_id\": \"" + breweryId + "\", \"name\":\"ZengBeer\", \"abv\": 4.5, \"description\":\"\" }" ).
            expect().
            statusCode( 201 ).
            when().
            post( "/api/beers" );

        String beerUrl = response.header( LOCATION );

        expect().
            statusCode( 200 ).
            contentType( APPLICATION_JSON ).
            body( "name", equalTo( "ZengBeer" ) ).
            when().
            get( beerUrl );

        expect().
            statusCode( 409 ).
            when().
            delete( breweryUrl );

        expect().
            statusCode( 200 ).
            when().
            delete( beerUrl );

        expect().
            statusCode( 200 ).
            when().
            delete( breweryUrl );
    }

    @Test
    public void testCreateUpdateBreweryAndBeer()
    {
        Response response = given().
            contentType( APPLICATION_JSON ).
            body( "{ \"name\":\"ZengBrewery\", \"url\":\"http://zeng-beers.com/\", \"description\":\"\" }" ).
            expect().
            statusCode( 201 ).
            when().
            post( "/api/breweries" );

        String breweryUrl = response.header( LOCATION );

        response = expect().
            statusCode( 200 ).
            contentType( APPLICATION_JSON ).
            when().
            get( breweryUrl );

        String breweryId = response.body().jsonPath().getString( "id" );

        response = given().
            contentType( APPLICATION_JSON ).
            body( "{ \"brewery_id\": \"" + breweryId + "\", \"name\":\"ZengBeer\", \"abv\": 4.5, \"description\":\"\" }" ).
            expect().
            statusCode( 201 ).
            when().
            post( "/api/beers" );

        String beerUrl = response.header( LOCATION );

        // Edit Brewery
        given().
            contentType( APPLICATION_JSON ).
            body( "{ \"name\":\"ZengBrewery EDITED\", \"url\":\"http://zeng-beers.com/EDITED\", \"description\":\"\" }" ).
            expect().
            statusCode( 200 ).
            when().
            put( breweryUrl );

        expect().
            statusCode( 200 ).
            contentType( APPLICATION_JSON ).
            body( "name", endsWith( "EDITED" ) ).
            body( "url", endsWith( "EDITED" ) ).
            when().
            get( breweryUrl );

        // Edit Beer
        given().
            contentType( APPLICATION_JSON ).
            body( "{  \"name\":\"ZengBeer EDITED\", \"abv\": 45, \"description\":\"\" }" ).
            expect().
            statusCode( 200 ).
            when().
            put( beerUrl );

        expect().
            statusCode( 200 ).
            contentType( APPLICATION_JSON ).
            body( "name", endsWith( "EDITED" ) ).
            body( "abv", equalTo( 45F ) ).
            when().
            get( beerUrl );
    }
}
