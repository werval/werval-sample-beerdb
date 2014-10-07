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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.text.Normalizer;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;
import org.hibernate.validator.constraints.SafeHtml;
import org.hibernate.validator.constraints.SafeHtml.WhiteListType;

@Entity
@Table( name = "beers" )
@JsonIgnoreProperties( ignoreUnknown = true )
public class Beer
{
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private Long id;

    @Column( length = 255, nullable = false )
    @NotBlank
    @Length( min = 3, max = 255 )
    @SafeHtml( whitelistType = WhiteListType.NONE, message = "Unauthorized html elements in 'name' property" )
    private String name;

    @Column( nullable = false )
    @NotNull
    @Range( min = 0, max = 100 )
    private Float abv;

    @Column( length = 16384, nullable = true )
    @Length( max = 16384 )
    @SafeHtml( whitelistType = WhiteListType.BASIC, message = "Unauthorized html elements in 'description' property" )
    private String description;

    @ManyToOne( optional = false )
    @JoinColumn( name = "brewery_id", nullable = false )
    /* package */ Brewery brewery;

    @JsonView( { BeerListView.class, BeerDetailView.class, BreweryDetailView.class } )
    public Long getId()
    {
        return id;
    }

    @JsonView( { BeerListView.class, BeerDetailView.class, BreweryDetailView.class } )
    public String getName()
    {
        return name;
    }

    @JsonView( BeerDetailView.class )
    public Float getAbv()
    {
        return abv;
    }

    @JsonView( BeerDetailView.class )
    public String getDescription()
    {
        return description;
    }

    @JsonView( { BeerListView.class, BeerDetailView.class } )
    public Brewery getBrewery()
    {
        return brewery;
    }

    @JsonDeserialize
    public void setName( String name )
    {
        this.name = name == null
                    ? null
                    : Normalizer.normalize( name, Normalizer.Form.NFKC ).trim();
    }

    public void setAbv( Float abv )
    {
        this.abv = abv;
    }

    @JsonDeserialize
    public void setDescription( String description )
    {
        this.description = description == null
                           ? null
                           : Normalizer.normalize( description, Normalizer.Form.NFKC ).trim();
    }
}
