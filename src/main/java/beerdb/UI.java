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

import java.util.LinkedHashMap;
import java.util.Map;
import org.qiweb.api.outcomes.Outcome;
import org.qiweb.api.templates.Template;
import org.qiweb.api.templates.Templates;
import org.qiweb.controllers.Classpath;

import static org.qiweb.api.context.CurrentContext.outcomes;
import static org.qiweb.api.context.CurrentContext.plugin;
import static org.qiweb.api.context.CurrentContext.reverseRoutes;
import static org.qiweb.util.Maps.fromMap;

public class UI
{
    public Outcome app()
    {
        Map<String, Object> context = fromMap( new LinkedHashMap<String, Object>() )
            .put( "css", reverseRoutes().get( UI.class, c -> c.assets( "css/main.css" ) ).httpUrl() )
            .put( "js", reverseRoutes().get( UI.class, c -> c.assets( "js/main.js" ) ).httpUrl() )
            .toMap();
        Template template = plugin( Templates.class ).named( "index.html" );
        String body = template.render( context );
        return outcomes().ok().asHtml().withBody( body ).build();
    }

    public Outcome assets( String path )
    {
        return new Classpath().resource( "assets/", path );
    }
}
