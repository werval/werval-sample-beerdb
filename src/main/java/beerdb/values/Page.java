/*
 * Copyright (c) 2014 the original author or authors
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
package beerdb.values;

import beerdb.Json;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;

/**
 * Page.
 *
 * @param <T> Parameterized item type
 */
public class Page<T>
{
    private final long total;
    private final int page;
    private final int pageSize;
    private final List<T> list;

    public Page( long total, int page, int pageSize, List<T> list )
    {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.list = list;
    }

    @JsonView( { Json.BreweryListView.class, Json.BeerListView.class } )
    public long getTotal()
    {
        return total;
    }

    @JsonView( { Json.BreweryListView.class, Json.BeerListView.class } )
    public int getPage()
    {
        return page;
    }

    @JsonView( { Json.BreweryListView.class, Json.BeerListView.class } )
    public int getPageSize()
    {
        return pageSize;
    }

    @JsonView( { Json.BreweryListView.class, Json.BeerListView.class } )
    public List<T> getList()
    {
        return list;
    }
}
