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
package beerdb.entities;

import beerdb.Json.BeerDetailView;
import beerdb.Json.BeerListView;
import beerdb.Json.BreweryDetailView;
import beerdb.Json.BreweryListView;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.werval.util.Hashid;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;
import org.hibernate.validator.constraints.SafeHtml;
import org.hibernate.validator.constraints.SafeHtml.WhiteListType;
import org.hibernate.validator.constraints.URL;

import static io.werval.api.context.CurrentContext.crypto;

@Entity
@Table( name = "breweries" )
@JsonIgnoreProperties( ignoreUnknown = true )
public class Brewery
{
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private Long id;

    @Column( length = 255, nullable = false )
    @NotBlank
    @Length( min = 3, max = 255 )
    @SafeHtml( whitelistType = WhiteListType.NONE, message = "Unauthorized html elements in 'name' property" )
    private String name;

    @Column
    @NotNull
    @Range( min = 0 )
    private Integer since = 1664;

    @Column( length = 1024, nullable = true )
    @URL
    @NotBlank
    @Length( max = 1024 )
    private String url;

    @Column( length = 16384, nullable = true )
    @Length( max = 16384 )
    @SafeHtml( whitelistType = WhiteListType.BASIC, message = "Unauthorized html elements in 'description' property" )
    private String description;

    @Column( name = "beers_count", nullable = false )
    /* package */ Integer beersCount = 0;

    @OneToMany( mappedBy = "brewery" )
    private List<Beer> beers = new ArrayList<>();

    public Hashid getHashid()
    {
        return crypto().hashids().encode( id );
    }

    @JsonProperty( "id" )
    @JsonView( { BreweryListView.class, BreweryDetailView.class, BeerListView.class, BeerDetailView.class } )
    public String getHashidString()
    {
        return getHashid().toString();
    }

    @JsonProperty( "id" )
    @JsonDeserialize
    public void setHashIdString( String hashid )
    {
        id = crypto().hashids().decode( hashid ).singleLong();
    }

    @JsonView( { BreweryListView.class, BreweryDetailView.class, BeerListView.class, BeerDetailView.class } )
    public String getName()
    {
        return name;
    }

    @JsonView( BreweryDetailView.class )
    public String getUrl()
    {
        return url;
    }

    @JsonView( { BreweryListView.class, BreweryDetailView.class } )
    public Integer getSince()
    {
        return since;
    }

    @JsonView( BreweryDetailView.class )
    public String getDescription()
    {
        return description;
    }

    @JsonView( BreweryDetailView.class )
    public List<Beer> getBeers()
    {
        return beers;
    }

    @JsonView( BreweryListView.class )
    public Integer getBeersCount()
    {
        return beersCount;
    }

    @JsonDeserialize
    public void setName( String name )
    {
        this.name = name == null
                    ? null
                    : Normalizer.normalize( name, Normalizer.Form.NFKC ).trim();
    }

    public void setSince( Integer since )
    {
        this.since = since;
    }

    @JsonDeserialize
    public void setUrl( String url )
    {
        this.url = url == null ? null : url.trim();
    }

    @JsonDeserialize
    public void setDescription( String description )
    {
        this.description = description == null
                           ? null
                           : Normalizer.normalize( description, Normalizer.Form.NFKC ).trim();
    }

    public void addBeer( Beer beer )
    {
        beers.add( beer );
        beer.brewery = this;
        beersCount += 1;
    }

    public void removeBeer( Beer beer )
    {
        beers.remove( beer );
        beer.brewery = null;
        beersCount -= 1;
    }
}
