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

import beerdb.entities.Beer;
import beerdb.entities.Brewery;
import beerdb.values.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.werval.api.outcomes.Outcome;
import io.werval.filters.AcceptContentTypes;
import io.werval.filters.XContentTypeOptions;
import io.werval.modules.jpa.JPA;
import io.werval.modules.json.JsonPluginException;
import io.werval.util.Hashid;
import java.io.IOException;
import java.util.List;
import javax.persistence.EntityManager;
import javax.validation.ConstraintViolationException;

import static io.werval.api.context.CurrentContext.crypto;
import static io.werval.api.context.CurrentContext.outcomes;
import static io.werval.api.context.CurrentContext.plugin;
import static io.werval.api.context.CurrentContext.request;
import static io.werval.api.context.CurrentContext.reverseRoutes;
import static io.werval.api.http.Headers.Names.LOCATION;
import static io.werval.api.mime.MimeTypesNames.APPLICATION_JSON;
import static io.werval.modules.json.JSON.json;

@XContentTypeOptions
public class API
{
    private final int PAGE_SIZE = 8;

    public Outcome index()
        throws JsonProcessingException
    {
        ObjectNode root = json().newObject();
        root.put( "breweries", reverseRoutes().get( API.class, c -> c.breweries( 1, "name", "asc" ) ).httpUrl() );
        root.put( "beers", reverseRoutes().get( API.class, c -> c.beers( 1, "name", "asc" ) ).httpUrl() );
        return outcomes().ok( json().toJSON( root ) ).asJson().build();
    }

    //
    //  _____                       _
    // | __  |___ ___ _ _ _ ___ ___|_|___ ___
    // | __ -|  _| -_| | | | -_|  _| | -_|_ -|
    // |_____|_| |___|_____|___|_| |_|___|___|
    // _________________________________________________________________________________________________________________
    //
    public Outcome breweries( Integer page, String sortBy, String order )
        throws JsonProcessingException
    {
        if( page < 1 )
        {
            page = 1;
        }
        EntityManager em = plugin( JPA.class ).em();
        Long total = em.createQuery( "select count(b) from Brewery b", Long.class ).getSingleResult();
        List<Brewery> breweries = em.createQuery(
            "select b from Brewery b order by b." + sortBy + " " + order, Brewery.class )
            .setFirstResult( ( page - 1 ) * PAGE_SIZE )
            .setMaxResults( PAGE_SIZE )
            .getResultList();
        byte[] json = json().toJSON( new Page<>( total, page, PAGE_SIZE, breweries ), Json.BreweryListView.class );
        return outcomes().ok( json ).asJson().build();
    }

    @AcceptContentTypes( APPLICATION_JSON )
    public Outcome createBrewery()
        throws IOException
    {
        byte[] body = request().body().asBytes();
        Brewery brewery;
        try
        {
            brewery = json().fromJSON( Brewery.class, body );
        }
        catch( JsonPluginException ex )
        {
            return badRequest( ex.getMessage() );
        }
        try
        {
            EntityManager em = plugin( JPA.class ).em();
            em.getTransaction().begin();
            em.persist( brewery );
            em.getTransaction().commit();

            String breweryRoute = reverseRoutes().get( API.class, c -> c.brewery( brewery.getHashid() ) ).httpUrl();
            byte[] json = json().toJSON( brewery, Json.BreweryListView.class );
            return outcomes().created()
                .withHeader( LOCATION, breweryRoute )
                .asJson()
                .withBody( json )
                .build();
        }
        catch( ConstraintViolationException ex )
        {
            return badRequest( ex.getConstraintViolations().toString() );
        }
    }

    public Outcome brewery( Hashid id )
        throws JsonProcessingException
    {
        EntityManager em = plugin( JPA.class ).em();
        Brewery brewery = em.find( Brewery.class, id.singleLong() );
        if( brewery == null )
        {
            return notFound( "Brewery" );
        }
        byte[] json = json().toJSON( brewery, Json.BreweryDetailView.class );
        return outcomes().ok( json ).asJson().build();
    }

    @AcceptContentTypes( APPLICATION_JSON )
    public Outcome updateBrewery( Hashid id )
        throws IOException
    {
        byte[] body = request().body().asBytes();
        try
        {
            EntityManager em = plugin( JPA.class ).em();
            em.getTransaction().begin();
            Brewery brewery = em.find( Brewery.class, id.singleLong() );
            if( brewery == null )
            {
                return notFound( "Brewery" );
            }
            json().updateFromJSON( brewery, body );
            em.persist( brewery );
            em.getTransaction().commit();
            return outcomes().ok().build();
        }
        catch( ConstraintViolationException ex )
        {
            return badRequest( ex.getConstraintViolations().toString() );
        }
    }

    public Outcome deleteBrewery( Hashid id )
        throws JsonProcessingException
    {
        EntityManager em = plugin( JPA.class ).em();
        em.getTransaction().begin();
        Brewery brewery = em.find( Brewery.class, id.singleLong() );
        if( brewery == null )
        {
            return notFound( "Brewery" );
        }
        if( brewery.getBeersCount() > 0 )
        {
            return outcomes().conflict()
                .withBody( "Does not have zero beers." )
                .asJson()
                .build();
        }
        em.remove( brewery );
        em.getTransaction().commit();
        return outcomes().ok().build();
    }

    //
    //  _____
    // | __  |___ ___ ___ ___
    // | __ -| -_| -_|  _|_ -|
    // |_____|___|___|_| |___|
    // _________________________________________________________________________________________________________________
    //
    public Outcome beers( Integer page, String sortBy, String order )
        throws JsonProcessingException
    {
        if( page < 1 )
        {
            page = 1;
        }
        EntityManager em = plugin( JPA.class ).em();
        Long total = em.createQuery( "select count(b) from Beer b", Long.class ).getSingleResult();
        List<Beer> beers = em.createQuery(
            "select b from Beer b order by b." + sortBy + " " + order, Beer.class )
            .setFirstResult( ( page - 1 ) * PAGE_SIZE )
            .setMaxResults( PAGE_SIZE )
            .getResultList();
        byte[] json = json().toJSON( new Page<>( total, page, PAGE_SIZE, beers ), Json.BeerListView.class );
        return outcomes().ok( json ).asJson().build();
    }

    @AcceptContentTypes( APPLICATION_JSON )
    public Outcome createBeer()
        throws IOException
    {
        byte[] body = request().body().asBytes();
        JsonNode bodyNode = json().fromJSON( body );
        if( !bodyNode.hasNonNull( "brewery_id" ) )
        {
            return badRequest( "Missing brewery_id" );
        }
        Hashid breweryId = crypto().hashids().decode( bodyNode.get( "brewery_id" ).asText() );
        Beer beer;
        try
        {
            beer = json().fromNode( Beer.class, bodyNode );
        }
        catch( JsonPluginException ex )
        {
            return badRequest( ex.getMessage() );
        }
        try
        {
            EntityManager em = plugin( JPA.class ).em();
            em.getTransaction().begin();
            Brewery brewery = em.find( Brewery.class, breweryId.singleLong() );
            if( brewery == null )
            {
                return badRequest( "No brewery found with id " + breweryId );
            }
            brewery.addBeer( beer );
            em.persist( beer );
            em.persist( brewery );
            em.getTransaction().commit();
            String beerRoute = reverseRoutes().get( API.class, c -> c.beer( beer.getHashid() ) ).httpUrl();
            byte[] json = json().toJSON( beer, Json.BeerListView.class );
            return outcomes().created()
                .withHeader( LOCATION, beerRoute )
                .asJson()
                .withBody( json )
                .build();
        }
        catch( ConstraintViolationException ex )
        {
            return badRequest( ex.getConstraintViolations().toString() );
        }
    }

    public Outcome beer( Hashid id )
        throws JsonProcessingException
    {
        EntityManager em = plugin( JPA.class ).em();
        Beer beer = em.find( Beer.class, id.singleLong() );
        if( beer == null )
        {
            return notFound( "Beer" );
        }
        byte[] json = json().toJSON( beer, Json.BeerDetailView.class );
        return outcomes().ok( json ).asJson().build();
    }

    @AcceptContentTypes( APPLICATION_JSON )
    public Outcome updateBeer( Hashid id )
        throws IOException
    {
        byte[] body = request().body().asBytes();
        try
        {
            EntityManager em = plugin( JPA.class ).em();
            em.getTransaction().begin();
            Beer beer = em.find( Beer.class, id.singleLong() );
            if( beer == null )
            {
                return notFound( "Beer" );
            }
            json().updateFromJSON( beer, body );
            em.persist( beer );
            em.getTransaction().commit();
            return outcomes().ok().build();
        }
        catch( ConstraintViolationException ex )
        {
            return badRequest( ex.getConstraintViolations().toString() );
        }
    }

    public Outcome deleteBeer( Hashid id )
        throws JsonProcessingException
    {
        EntityManager em = plugin( JPA.class ).em();
        em.getTransaction().begin();
        Beer beer = em.find( Beer.class, id.singleLong() );
        if( beer == null )
        {
            return notFound( "Beer" );
        }
        Brewery brewery = beer.getBrewery();
        brewery.removeBeer( beer );
        em.remove( beer );
        em.persist( brewery );
        em.getTransaction().commit();
        return outcomes().ok().build();
    }

    private Outcome notFound( String what )
        throws JsonProcessingException
    {
        return outcomes().notFound()
            .withBody( errorBody( what + " not found" ) )
            .asJson()
            .build();
    }

    private Outcome badRequest( String... messages )
        throws JsonProcessingException
    {
        return outcomes().badRequest()
            .withBody( errorBody( messages ) )
            .asJson()
            .build();
    }

    private byte[] errorBody( String... messages )
        throws JsonProcessingException
    {
        ObjectNode root = json().newObject();
        ArrayNode errors = root.putArray( "errors" );
        for( String message : messages )
        {
            errors.add( message );
        }
        return json().toJSON( root );
    }
}
