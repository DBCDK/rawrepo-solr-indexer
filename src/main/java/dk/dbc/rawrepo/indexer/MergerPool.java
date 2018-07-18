/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.rawrepo.indexer;

import dk.dbc.marcxmerge.MarcXMerger;
import dk.dbc.marcxmerge.MarcXMergerException;

import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author DBC {@literal <dk.dbc.dk>}
 */
@Singleton
public class MergerPool {
    Set<MarcXMerger> mergers = new HashSet<>();


    public MarcXMerger getMerger() throws MarcXMergerException {
        synchronized (this) {
            if (mergers.isEmpty())
                return new MarcXMerger();
            Iterator<MarcXMerger> iterator = mergers.iterator();
            MarcXMerger merger = iterator.next();
            iterator.remove();
            return merger;
        }
    }

    public void putMerger(MarcXMerger merger) {
        synchronized (this) {
            mergers.add(merger);
        }
    }

}
