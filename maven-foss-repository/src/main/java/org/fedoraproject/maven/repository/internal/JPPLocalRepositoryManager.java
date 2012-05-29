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
package org.fedoraproject.maven.repository.internal;

import java.io.File;

import org.fedoraproject.maven.repository.jpp.JavadirWorkspaceReader;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.repository.LocalArtifactRegistration;
import org.sonatype.aether.repository.LocalArtifactRequest;
import org.sonatype.aether.repository.LocalArtifactResult;
import org.sonatype.aether.repository.LocalMetadataRegistration;
import org.sonatype.aether.repository.LocalMetadataRequest;
import org.sonatype.aether.repository.LocalMetadataResult;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.WorkspaceReader;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class JPPLocalRepositoryManager implements LocalRepositoryManager {
    private final LocalRepository repository;

    private WorkspaceReader jppRepository = new JavadirWorkspaceReader();

    JPPLocalRepositoryManager() {
        repository = new LocalRepository("/usr/share/java");
    }

    @Override
    public LocalRepository getRepository() {
        return repository;
    }

    @Override
    public String getPathForLocalArtifact(Artifact artifact) {
        throw new RuntimeException(
                "NYI: org.fedoraproject.maven.repository.internal." +
                "JPPLocalRepositoryManager.getPathForLocalArtifact");
    }

    @Override
    public String getPathForRemoteArtifact(
            Artifact artifact,
            RemoteRepository repository,
            String context) {

        throw new RuntimeException(
                "NYI: org.fedoraproject.maven.repository.internal." +
                "JPPLocalRepositoryManager.getPathForRemoteArtifact");
    }

    @Override
    public String getPathForLocalMetadata(Metadata metadata) {
        throw new RuntimeException(
                "NYI: org.fedoraproject.maven.repository.internal." +
                "JPPLocalRepositoryManager.getPathForLocalMetadata");
    }

    @Override
    public String getPathForRemoteMetadata(
            Metadata metadata,
            RemoteRepository repository,
            String context) {

        throw new RuntimeException(
                "NYI: org.fedoraproject.maven.repository.internal." +
                "JPPLocalRepositoryManager.getPathForRemoteMetadata");
    }

    @Override
    public LocalArtifactResult find(
            RepositorySystemSession session,
            LocalArtifactRequest request) {

        final LocalArtifactResult result = new LocalArtifactResult(request);
        final File file = jppRepository.findArtifact(request.getArtifact());
        if (file != null) {
            result.setFile(file);
            result.setAvailable(true);
        }
        return result;
    }

    @Override
    public void add(
            RepositorySystemSession session,
            LocalArtifactRegistration request) {

        throw new RuntimeException(
                "NYI: org.fedoraproject.maven.repository.internal." +
                "JPPLocalRepositoryManager.add");
    }

    @Override
    public LocalMetadataResult find(
            RepositorySystemSession session,
            LocalMetadataRequest request) {

//        System.err.println("JPPLocalRepositoryManager: find(" + session + ", " +
//                request + ")");
        final LocalMetadataResult result = new LocalMetadataResult(request);
        // TODO: for the moment we don't support metadata
        return result;
//        throw new RuntimeException("find");
    }

    @Override
    public void add(
            RepositorySystemSession session,
            LocalMetadataRegistration request) {

        throw new RuntimeException(
                "NYI: org.fedoraproject.maven.repository.internal." +
                "JPPLocalRepositoryManager.add");
    }
}
