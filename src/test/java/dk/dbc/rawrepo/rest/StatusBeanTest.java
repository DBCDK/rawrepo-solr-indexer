package dk.dbc.rawrepo.rest;

import jakarta.ws.rs.core.Response;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLDataException;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatusBeanTest {

    @Test
    public void testStatusDBFail() throws SQLException {
        final Response.Status status = Response.Status.OK;
        final StatusBean statusBean = new StatusBean();
        statusBean.rawrepoDataSource = mock(DataSource.class);
        when(statusBean.rawrepoDataSource.getConnection()).thenThrow(new SQLDataException());
        assertThat(statusBean.isDbAlive(), is(false));
    }

    @Test
    public void testStatusSOLRFail() throws IOException, SolrServerException {
        final Response.Status status = Response.Status.OK;
        final StatusBean statusBean = new StatusBean();
        statusBean.solrClient = mock(HttpSolrClient.class);
        when(statusBean.solrClient.ping()).thenThrow(new SolrServerException("Solr Exception"));
        assertThat(statusBean.isSolrAlive(), is(false));
    }

    @Test
    public void testStatusSOLROk() throws IOException, SolrServerException {
        final Response.Status status = Response.Status.OK;
        final StatusBean statusBean = new StatusBean();
        statusBean.solrClient = mock(HttpSolrClient.class);
        when(statusBean.solrClient.ping()).thenReturn(new SolrPingResponse());
        assertThat(statusBean.isSolrAlive(), is(true));
    }
}
