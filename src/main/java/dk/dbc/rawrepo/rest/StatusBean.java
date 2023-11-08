/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.rawrepo.rest;

import dk.dbc.serviceutils.ServiceStatus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Stateless
@Path("/api")
public class StatusBean implements ServiceStatus {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(StatusBean.class);
    private static final String IM_ALIVE_QUERY="select 1 alive";

    @Resource(lookup = "jdbc/rawrepo")
    public DataSource rawrepoDataSource;

    @Inject
    @ConfigProperty(name = "SOLR_URL", defaultValue = "SOLR_URL not set")
    protected String SOLR_URL;

    public Http2SolrClient solrClient;

    @PostConstruct
    public void create() {
        solrClient = new Http2SolrClient.Builder(SOLR_URL).build();
    }

    @PreDestroy
    public void destroy() {
        solrClient.close();
    }

    boolean isDbAlive() {
        LOGGER.entry();
        String res = "";
        boolean alive = true;
        try (Connection connection = rawrepoDataSource.getConnection();
             Statement stmt = connection.createStatement()) {
                try (ResultSet resultSet = stmt.executeQuery(IM_ALIVE_QUERY)) {
                    resultSet.next();
                }
        } catch (SQLException e) {
            LOGGER.error("Status check db alive failed..", e);
            alive = false;
        } finally {
            LOGGER.exit(res);
        }
        return alive;
    }

    boolean isSolrAlive() {
        LOGGER.entry();
        boolean alive = true;

        // Test connection and return proper error if there is a problem
        try {
            solrClient.ping();
        } catch (SolrServerException | IOException ex) {
            alive = false;
            LOGGER.error("Status check solr alive failed..", ex);
        }
        return alive;
    }

    @Override
    public Response getStatus() {
        if (isDbAlive() && isSolrAlive()) {
            return Response.ok().entity(OK_ENTITY).build();
        }
        else {
            return Response.serverError().build();
        }
    }
}
