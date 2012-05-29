/*
 * Copyright (c) 2012 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.fedoraproject.maven.repository.jpp;

import java.io.File;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.WorkspaceReader;
import org.sonatype.aether.repository.WorkspaceRepository;

public class JavadirWorkspaceReader implements WorkspaceReader {
    private WorkspaceRepository workspaceRepository;

    private static final char GROUP_SEPARATOR = '.';
    private static final char PATH_SEPARATOR = '/';

    public JavadirWorkspaceReader() {
        workspaceRepository = new WorkspaceRepository("javadir-workspace");
    }

    public WorkspaceRepository getRepository() {
        return workspaceRepository;
    }

    public File findArtifact(Artifact artifact) {
        MavenJPackageDepmap.debug("=============JAVADIRREADER-FIND_ARTIFACT: "
                + artifact.getArtifactId());
        StringBuffer path = new StringBuffer();
        File ret = new File("");
        String artifactId = artifact.getArtifactId();
        String groupId = artifact.getGroupId();
        String version = artifact.getVersion();

        MavenJPackageDepmap.debug("Wanted GROUPID=" + groupId);
        MavenJPackageDepmap.debug("Wanted ARTIFACTID=" + artifactId);

        if (!groupId.startsWith("JPP")) {
            MavenJPackageDepmap map = MavenJPackageDepmap.getInstance();
            Hashtable<String, String> newInfo = map.getMappedInfo(groupId,
                    artifactId, version);

            groupId = (String) newInfo.get("group");
            artifactId = (String) newInfo.get("artifact");
        }
        MavenJPackageDepmap.debug("Resolved GROUPID=" + groupId);
        MavenJPackageDepmap.debug("Resolved ARTIFACTID=" + artifactId);

        if (artifact.getExtension().equals("pom")) {
            path = getPOMPath(groupId, artifactId);
            ret = new File(path.toString());
        } else {
            String repos[] = {"/usr/share/maven/repository/",
                    "/usr/share/maven/repository-java-jni/",
                    "/usr/share/maven/repository-jni/"};
            String relativeArtifactPath = groupId + "/" + artifactId + "."
                    + artifact.getExtension();
            for (String repo : repos) {
                path = new StringBuffer(repo + relativeArtifactPath);
                ret = new File(path.toString());
                if (ret.isFile()) {
                    MavenJPackageDepmap.debug("Returning " + repo
                            + relativeArtifactPath);
                    return ret;
                }
            }
        }

        // if file doesn't exist return null to delegate to other
        // resolvers (reactor/local repo)
        if (ret.isFile()) {
            MavenJPackageDepmap.debug("Returning " + path.toString());
            return ret;
        } else {
            MavenJPackageDepmap.debug("Returning null for gid:aid =>" + groupId
                    + ":" + artifactId);
            return null;
        }
    }

    public List<String> findVersions(Artifact artifact) {
        List<String> ret = new LinkedList<String>();
        ret.add("latest");
        return ret;
    }

    private StringBuffer getPOMPath(String groupId, String artifactId) {
        String fName = groupId.replace(PATH_SEPARATOR, GROUP_SEPARATOR) + "-"
                + artifactId + ".pom";
        File f;
        String[] pomRepos = {"/usr/share/maven2/poms/",
                "/usr/share/maven/poms/", "/usr/share/maven-poms/"};

        for (String pomRepo : pomRepos) {
            f = new File(pomRepo + fName);
            if (f.exists()) {
                return new StringBuffer(f.getPath());
            }
        }

        // final fallback to m2 default poms
        return new StringBuffer("/usr/share/maven2/default_poms/" + fName);
    }
}
