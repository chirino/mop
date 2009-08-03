/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.support;

import org.fusesource.mop.MOP;

/**
 * @author chirino
 */
public class ArtifactId {

    private String groupId;
    private String artifactId;
    private String type;
    private String classifier;
    private String version;

    public ArtifactId() {
    }

    public boolean parse(String value, String defaultVersion, String defaultType) {
        type = defaultType;
        version = defaultVersion;
        String parts[] = value.split(":");
        switch (parts.length) {
            case 1:
                artifactId = parts[0];
                return true;
            case 2:
                groupId = parts[0];
                artifactId = parts[1];
                return true;
            case 3:
                groupId = parts[0];
                artifactId = parts[1];
                version = parts[2];
                return true;
            case 4:
                groupId = parts[0];
                artifactId = parts[1];
                type = parts[2];
                version = parts[3];
                return true;
            case 5:
                groupId = parts[0];
                artifactId = parts[1];
                type = parts[2];
                classifier = parts[3];
                version = parts[4];
                return true;
            default:
                return false;
        }
    }

    public boolean strictParse(String value) {
        String parts[] = value.split(":");
        switch (parts.length) {
            case 4:
            case 5:
                return parse(value, MOP.DEFAULT_VERSION, MOP.DEFAULT_TYPE);
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String toString() {
        return "" + groupId + ":" + artifactId + ":" + type + (classifier != null ? ":" + classifier : "") + ":" + version;
    }
}