package org.fusesource.mop;

/**
 * @author chirino
*/
class ArtifactId {

    private String groupId;
    private String artifactId;
    private String classifier;
    private String version;

    public ArtifactId() {
    }

    public boolean parse(String value, String defaultVersion) {
        String parts[] = value.split(":");
        switch(parts.length) {
            case 2:
                groupId = parts[0];
                artifactId = parts[1];
                classifier = "jar";
                version = defaultVersion;
                return true;
            case 3:
                groupId = parts[0];
                artifactId = parts[1];
                version = parts[2];
                classifier = "jar";
                return true;
            case 4:
                groupId = parts[0];
                artifactId = parts[1];
                classifier = parts[2];
                version = parts[3];
                return true;
            default:
                return false;
        }
    }

    public boolean strictParse(String value) {
        String parts[] = value.split(":");
        switch(parts.length) {
            case 4:
                groupId = parts[0];
                artifactId = parts[1];
                classifier = parts[2];
                version = parts[3];
                return true;
            default:
                return false;
        }
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String toString() {
        return ""+groupId + ":" + artifactId + ":" + classifier + ":" + version;
    }
}