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
package beerdb.ui;

import org.fluentlenium.core.FluentPage;
import org.fluentlenium.core.domain.FluentWebElement;
import org.openqa.selenium.WebDriver;

import static io.werval.util.IllegalArguments.ensureNotEmpty;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.fluentlenium.FluentLeniumAssertions.assertThat;

/**
 * Brewery Page Object.
 */
public class BreweryPage
    extends FluentPage
{
    private final String url;

    public BreweryPage( WebDriver driver, String url )
    {
        super( driver );
        ensureNotEmpty( "url", url );
        this.url = url;
    }

    @Override
    public String getUrl()
    {
        return url;
    }

    @Override
    public void isAt()
    {
        // Navigation highlight
        assertThat( findFirst( "ul.navbar-nav li" ).getAttribute( "class" ) ).isEqualTo( "active" );
        // Brewery loaded
        assertThat( findFirst( "#brewery" ) ).isNotNull();
        // Associated beers loaded
        String beersCount = findFirst( "h3 span.faded" ).getText();
        assertThat( beersCount ).isNotEmpty();
        assertThat( find( ".list-group a" ).size() ).isEqualTo( Integer.valueOf( beersCount ) );
    }

    public BeerPage clickBeer( int index )
    {
        FluentWebElement link = find( ".list-group a" ).get( index );
        String href = link.getAttribute( "href" );
        link.click();
        return new BeerPage( getDriver(), href );
    }

    public String breweryName()
    {
        return findFirst( "span.brewery-name" ).getText();
    }

    public void delete()
    {
        findFirst( ".delete-button" ).click();
        getDriver().switchTo().alert().accept();
    }

    public EditBreweryPage edit()
    {
        FluentWebElement link = findFirst( ".edit-button" );
        String href = link.getAttribute( "href" );
        link.click();
        return new EditBreweryPage( getDriver(), href );
    }
}
