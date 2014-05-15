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

import java.sql.Connection;
import java.sql.SQLException;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.qiweb.api.Application;
import org.qiweb.api.exceptions.QiWebException;
import org.qiweb.modules.jdbc.JDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.qiweb.api.Mode.TEST;
import static org.qiweb.api.util.Strings.EMPTY;

/**
 * Beer Database Global Object.
 */
public class Global
    extends org.qiweb.api.Global
{
    private static final Logger LOG = LoggerFactory.getLogger( Global.class );

    @Override
    public void onActivate( Application application )
    {
        // Database schema migration
        liquibaseUpdate( application );

        LOG.info( "Beer Database Activated" );
    }

    @Override
    public void onPassivate( Application application )
    {
        // Persistence
        if( application.mode() == TEST )
        {
            // Drop ALL data on TEST mode
            liquibaseDropAll( application );
        }

        LOG.info( "Beer Database Passivated" );
    }

    private void liquibaseUpdate( Application application )
    {
        Liquibase liquibase = null;
        try
        {
            liquibase = newLiquibase( application );
            liquibase.update( EMPTY );
        }
        catch( ClassNotFoundException | LiquibaseException | SQLException ex )
        {
            throw new QiWebException( "Unable to apply database changelog: " + ex.getMessage(), ex );
        }
        finally
        {
            closeLiquibaseSilently( liquibase );
        }
    }

    private void liquibaseDropAll( Application application )
    {
        Liquibase liquibase = null;
        try
        {
            liquibase = newLiquibase( application );
            liquibase.dropAll();
        }
        catch( ClassNotFoundException | LiquibaseException | SQLException ex )
        {
            throw new QiWebException( "Unable to drop database data: " + ex.getMessage(), ex );
        }
        finally
        {
            closeLiquibaseSilently( liquibase );
        }
    }

    private Liquibase newLiquibase( Application application )
        throws ClassNotFoundException, SQLException, LiquibaseException
    {
        Connection connection = application.plugin( JDBC.class ).connection();
        Liquibase liquibase = new Liquibase(
            "db-changelog.xml",
            new ClassLoaderResourceAccessor( application.classLoader() ),
            DatabaseFactory.getInstance().findCorrectDatabaseImplementation( new JdbcConnection( connection ) )
        );
        return liquibase;
    }

    private void closeLiquibaseSilently( Liquibase liquibase )
    {
        if( liquibase != null )
        {
            try
            {
                liquibase.getDatabase().getConnection().close();
            }
            catch( DatabaseException ignored )
            {
            }
        }
    }
}
