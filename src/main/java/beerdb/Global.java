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

import io.werval.api.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Beer Database Global Object.
 */
public class Global
    extends io.werval.api.Global
{
    private static final Logger LOG = LoggerFactory.getLogger( Global.class );

    @Override
    public void onActivate( Application application )
    {
        LOG.info( "Beer Database Activated" );
    }

    @Override
    public void onPassivate( Application application )
    {
        LOG.info( "Beer Database Passivated" );
    }
}
