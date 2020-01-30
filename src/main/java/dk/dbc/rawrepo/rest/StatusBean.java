/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.rawrepo.rest;

import dk.dbc.serviceutils.ServiceStatus;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

@Stateless
@Path("/api")
public class StatusBean implements ServiceStatus {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(StatusBean.class);
    private static final String IM_ALIVE_QUERY="select 1 alive";

    @Resource(lookup = "jdbc/rawrepo")
    public DataSource rawrepoDataSource;

    public boolean isDbAlive() {
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

    @Override
    public Response getStatus() {
        return isDbAlive()?Response.ok().build():Response.serverError().build();
    }
}
