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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MavenJPackageDepmap {
    //private static boolean PROCESS_VERSIONS = (System.getProperty("maven.ignore.versions") == null && System.getProperty("maven.local.mode") == null);
    private static boolean PROCESS_VERSIONS = false;

    private static class ArtifactDefinition {
        String groupId = null;
        String artifactId = null;
        String version = null;
    }

    /**
     * @author Stanislav Ochotnicky <sochotnicky@redhat.com>
     *         <p/>
     *         This class is used to wrap around fragments that are mapping
     *         artifacts to jar files in our _javadir. These used to be
     *         processed in a macro after every package installation. Fragments
     *         themselves are not proper xml files (they have no root element)
     *         so we have to fix them by wrapping them in one root element.
     */
    private static class WrapFragmentStream extends InputStream {
        String startTag = "<deps>";
        String endTag = "</deps>";
        byte fragmentContent[];
        int position;

        WrapFragmentStream(String fragmentPath) throws IOException {
            FileInputStream fin = new FileInputStream(fragmentPath);
            int nBytes = fin.available();
            byte tmpContent[] = new byte[nBytes];
            fin.read(tmpContent);
            fin.close();
            byte startBytes[] = startTag.getBytes();
            byte endBytes[] = endTag.getBytes();
            fragmentContent = new byte[nBytes + startBytes.length
                    + endBytes.length];
            System.arraycopy(startBytes, 0, fragmentContent, 0,
                    startBytes.length);
            System.arraycopy(tmpContent, 0, fragmentContent, startBytes.length,
                    tmpContent.length);
            System.arraycopy(endBytes, 0, fragmentContent, startBytes.length
                    + tmpContent.length, endBytes.length);
            position = 0;
        }

        public int read() throws IOException {
            if (position < fragmentContent.length) {
                return fragmentContent[position++];
            } else {
                return -1;
            }
        }
    }

    private static MavenJPackageDepmap instance;
    private static Hashtable<String, String> jppArtifactMap;

    private MavenJPackageDepmap() {
        jppArtifactMap = new Hashtable<String, String>();
        buildJppArtifactMap();
    }

    public static MavenJPackageDepmap getInstance() {
        if (instance == null) {
            instance = new MavenJPackageDepmap();
        }

        return instance;
    }

    public Hashtable<String, String> getMappedInfo(
            Hashtable<String, String> mavenDep) {

        return getMappedInfo((String) mavenDep.get("group"),
                (String) mavenDep.get("artifact"),
                (String) mavenDep.get("version"));
    }

    public Hashtable<String, String> getMappedInfo(
            String groupId,
            String artifactId,
            String version) {

        Hashtable<String, String> jppDep;
        String idToCheck, jppCombination;

        if (PROCESS_VERSIONS) {
            idToCheck = groupId + "," + artifactId + "," + version;
        } else {
            idToCheck = groupId + "," + artifactId;
        }

        jppCombination = (String) jppArtifactMap.get(idToCheck);
        jppDep = new Hashtable<String, String>();
        if (jppCombination != null && jppCombination != "") {

            StringTokenizer st = new StringTokenizer(jppCombination, ",");

            jppDep.put("group", st.nextToken());
            jppDep.put("artifact", st.nextToken());
            jppDep.put("version", st.nextToken());

        } else {
            jppDep.put("group", groupId);
            jppDep.put("artifact", artifactId);
            jppDep.put("version", version);
        }

        return jppDep;
    }

    /**
     * Returns whether or not the given dependency should be dropped.
     */
    public boolean shouldEliminate(
            String groupId,
            String artifactId,
            String version) {

        String idToCheck;

        if (PROCESS_VERSIONS) {
            idToCheck = groupId + "," + artifactId + "," + version;
        } else {
            idToCheck = groupId + "," + artifactId;
        }

        return jppArtifactMap.get(idToCheck) != null
                && jppArtifactMap.get(idToCheck).equals("");

    }

    private static void buildJppArtifactMap() {
        if (!PROCESS_VERSIONS) {
            debug("Processing file: /usr/share/java-utils/xml/maven2-versionless-depmap.xml");
            processDepmapFile("/etc/maven/maven2-versionless-depmap.xml");
        }

        // process fragments in etc
        File fragmentDir = new File("/etc/maven/fragments");
        String flist[] = fragmentDir.list();
        if (flist != null) {
            java.util.Arrays.sort(flist);
            for (String fragFilename : flist)
                processDepmapFile("/etc/maven/fragments/" + fragFilename);
        }

        // process fragments is usr. Once packages are rebuilt, we can skip
        // fragments in /etc
        fragmentDir = new File("/usr/share/maven-fragments");
        flist = fragmentDir.list();
        if (flist != null) {
            java.util.Arrays.sort(flist);
            for (String fragFilename : flist)
                processDepmapFile("/usr/share/maven-fragments/" + fragFilename);
        }

        String customFileName = System.getProperty("fre.depmap.file",
                null);
        if (customFileName != null) {
            debug("Processing file: " + customFileName);
            processDepmapFile(customFileName);
        }

    }

    private static void processDepmapFile(String fileName) {
        Document mapDocument;
        debug("Loading depmap file: " + fileName);
        try {
            DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
            fact.setNamespaceAware(true);
            DocumentBuilder builder = fact.newDocumentBuilder();
            // we can wrap even old depmaps, no harm done
            WrapFragmentStream wfs = new WrapFragmentStream(fileName);
            mapDocument = builder.parse(wfs);
            wfs.close();
        } catch (FileNotFoundException fnfe) {
            System.err.println("ERROR: Unable to find map file: " + fileName);
            fnfe.printStackTrace();
            return;
        } catch (IOException ioe) {
            System.err
                    .println("ERROR: I/O exception occured when opening map file");
            ioe.printStackTrace();
            return;
        } catch (ParserConfigurationException pce) {
            System.err
                    .println("ERROR: Parsing of depmap file failed - configuration");
            pce.printStackTrace();
            return;
        } catch (SAXException se) {
            System.err.println("ERROR: Parsing of depmap file failed");
            se.printStackTrace();
            return;
        }

        NodeList depNodes = (NodeList) mapDocument
                .getElementsByTagName("dependency");

        for (int i = 0; i < depNodes.getLength(); i++) {
            Element depNode = (Element) depNodes.item(i);

            NodeList mavenNodeList = (NodeList) depNode
                    .getElementsByTagName("maven");
            if (mavenNodeList.getLength() != 1) {
                debug("Number of maven sub-elements is not 1. Bailing from depmap generation");
                debug("Maven node: " + depNode.getTextContent());
                return;
            }
            ArtifactDefinition mavenAD = getArtifactDefinition((Element) mavenNodeList
                    .item(0));

            ArtifactDefinition jppAD = null;
            NodeList jppNodeList = (NodeList) depNode
                    .getElementsByTagName("jpp");

            if (jppNodeList.getLength() == 1) {
                jppAD = getArtifactDefinition((Element) jppNodeList.item(0));
                if (PROCESS_VERSIONS) {
                    debug("*** Adding: " + mavenAD.groupId + ","
                            + mavenAD.artifactId + "," + mavenAD.version
                            + " => " + jppAD.groupId + "," + jppAD.artifactId
                            + "," + jppAD.version + " to map...");

                    jppArtifactMap.put(mavenAD.groupId + ","
                            + mavenAD.artifactId + "," + mavenAD.version,
                            jppAD.groupId + "," + jppAD.artifactId + ","
                                    + jppAD.version);
                } else {
                    debug("*** Adding: " + mavenAD.groupId + ","
                            + mavenAD.artifactId + " => " + jppAD.groupId + ","
                            + jppAD.artifactId + "," + jppAD.version
                            + " to map...");

                    jppArtifactMap.put(mavenAD.groupId + ","
                            + mavenAD.artifactId, jppAD.groupId + ","
                            + jppAD.artifactId + "," + jppAD.version);
                }
            } else {
                debug("Number of jpp sub-elements is not 1. Dropping dependency for "
                        + mavenAD.groupId + ":" + mavenAD.artifactId);
                jppArtifactMap.put(mavenAD.groupId + "," + mavenAD.artifactId,
                        "JPP/maven,empty-dep," + mavenAD.version);
            }
        }
    }

    private static ArtifactDefinition getArtifactDefinition(Element element) {
        ArtifactDefinition ad = new ArtifactDefinition();

        NodeList nodes = element.getElementsByTagName("groupId");
        if (nodes.getLength() != 1) {
            debug("groupId definition not found in depmap");
            return null;
        }
        ad.groupId = nodes.item(0).getTextContent();

        nodes = element.getElementsByTagName("artifactId");
        if (nodes.getLength() != 1) {
            debug("artifactId definition not found in depmap");
            return null;
        }
        ad.artifactId = nodes.item(0).getTextContent();

        nodes = element.getElementsByTagName("version");
        if (nodes.getLength() != 1) {
            ad.version = "DUMMY_VER";
        } else {
            ad.version = nodes.item(0).getTextContent();
        }
        return ad;
    }

    public static void debug(String msg) {
        if (System.getProperty("maven.local.debug") != null)
            System.err.println(msg);
    }
}
