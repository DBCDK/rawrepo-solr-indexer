package dk.dbc.rawrepo.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


@ApplicationPath("/")
public class RestApplication extends Application {
    private static final Set<Class<?>> classes = new HashSet<>(Arrays.asList(StatusBean.class));

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}

