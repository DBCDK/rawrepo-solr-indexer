/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.rawrepo.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
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

