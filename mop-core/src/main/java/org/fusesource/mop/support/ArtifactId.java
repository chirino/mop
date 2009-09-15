/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.support;

import java.util.ArrayList;

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

    static public ArtifactId parse(String value) {
        return parse(value, MOP.DEFAULT_VERSION, MOP.DEFAULT_TYPE);
    }
    
    static public ArtifactId parse(String value, String defaultVersion, String defaultType) {
        ArtifactId rc = new ArtifactId();
        rc.type = defaultType;
        rc.version = defaultVersion;
        String parts[] = value.split(":");
        switch (parts.length) {
            case 1:
                rc.artifactId = parts[0];
                return rc;
            case 2:
                rc.groupId = parts[0];
                rc.artifactId = parts[1];
                return rc;
            case 3:
                rc.groupId = parts[0];
                rc.artifactId = parts[1];
                rc.version = parts[2];
                return rc;
            case 4:
                rc.groupId = parts[0];
                rc.artifactId = parts[1];
                rc.type = parts[2];
                rc.version = parts[3];
                return rc;
            case 5:
                rc.groupId = parts[0];
                rc.artifactId = parts[1];
                rc.type = parts[2];
                rc.classifier = parts[3];
                rc.version = parts[4];
                return rc;
            default:
                return null;
        }
    }

    static public ArtifactId strictParse(String value) {
        String parts[] = value.split(":");
        switch (parts.length) {
            case 4:
            case 5:
                return parse(value, MOP.DEFAULT_VERSION, MOP.DEFAULT_TYPE);
            default:
                return null;
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
        ArrayList<String> parts = new ArrayList<String>(5);

        // Complex if strucutre here is that we only generate string that can be parsed.
        // even if we are currently set with an invalid combination of properties.
        if( groupId==null ) {
            parts.add(artifactId);
        } else {
            parts.add(groupId);
            parts.add(artifactId);
            if( version!=null ) {
                if( type!=null ) {
                    parts.add(type);
                    if( classifier!=null ) {
                        parts.add(classifier);
                    }
                }
                parts.add(version);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if( sb.length()!=0 ) {
                sb.append(':');
            }
            sb.append(part);
        }
        return sb.toString();
    }
}