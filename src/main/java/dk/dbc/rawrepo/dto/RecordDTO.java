package dk.dbc.rawrepo.dto;

public class RecordDTO {

    private RecordIdDTO recordId;
    private boolean isDeleted;
    private String mimetype;
    private String created;
    private String modified;
    private String trackingId;
    private byte[] content;

    public RecordIdDTO getRecordId() {
        return recordId;
    }

    public void setRecordId(RecordIdDTO recordId) {
        this.recordId = recordId;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

}
