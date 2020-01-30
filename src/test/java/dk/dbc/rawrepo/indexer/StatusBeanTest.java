package dk.dbc.rawrepo.indexer;

import dk.dbc.rawrepo.rest.StatusBean;
import java.sql.SQLDataException;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.ws.rs.core.Response;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatusBeanTest {
    @Test
    public void testStatusFail() throws SQLException {
        Response.Status status = Response.Status.OK;
        StatusBean statusBean = new StatusBean();
        statusBean.rawrepoDataSource = mock(DataSource.class);
        when(statusBean.rawrepoDataSource.getConnection()).thenThrow(new SQLDataException());
        assertThat(statusBean.isDbAlive(), is(false));
    }
}
