/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.rawrepo.indexer;

public class SupplementaryData {

    private String mimetype;
    private int correctedAgencyId;

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public int getCorrectedAgencyId() {
        return correctedAgencyId;
    }

    public void setCorrectedAgencyId(int correctedAgencyId) {
        this.correctedAgencyId = correctedAgencyId;
    }
}
