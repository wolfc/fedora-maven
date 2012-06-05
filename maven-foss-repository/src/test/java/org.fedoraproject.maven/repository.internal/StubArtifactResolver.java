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

import static org.apache.maven.artifact.Artifact.LATEST_VERSION;

import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.impl.ArtifactResolver;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.transfer.ArtifactNotFoundException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:mark.cafaro@gmail.com">Mark Cafaro</a>
 */
public class StubArtifactResolver implements ArtifactResolver {

    public static final String JPP_VERSION = "JPP";

    public enum ResolverMode {
        EXACT, LATEST, JPP, FAIL
    }

    public static ResolverMode mode = ResolverMode.EXACT;

    public ArtifactResult resolveArtifact(
            RepositorySystemSession session,
            ArtifactRequest request)
            throws ArtifactResolutionException {

        return resolveArtifacts(session, Collections.singleton(request)).get(0);
    }

    public List<ArtifactResult> resolveArtifacts(
            RepositorySystemSession session,
            Collection<? extends ArtifactRequest> requests)
            throws ArtifactResolutionException {

        List<ArtifactResult> results =
                new ArrayList<ArtifactResult>(requests.size());
        boolean failures = false;

        for (ArtifactRequest request : requests) {
            List<RemoteRepository> repositories = request.getRepositories();
            if (repositories.size() != 1) {
                throw new IllegalStateException(
                        "Wrong number of repositories in " + repositories);
            }

            ArtifactResult result = new ArtifactResult(request);
            Artifact artifact = request.getArtifact();

            boolean requestedLatest = artifact.getVersion().equals(LATEST_VERSION);
            boolean requestedJpp =
                    session.getLocalRepository().getBasedir().
                            equals(new File("/usr/share/java"));

            if ((mode == ResolverMode.EXACT)
                    || (mode == ResolverMode.LATEST && requestedLatest)
                    || (mode == ResolverMode.JPP && requestedJpp)) {

                switch (mode) {
                    case EXACT: artifact = artifact.setVersion(artifact.getVersion());
                        break;
                    case LATEST: artifact = artifact.setVersion(LATEST_VERSION);
                        break;
                    case JPP: artifact = artifact.setVersion(JPP_VERSION);
                        break;
                }
                result.setArtifact(artifact);
                result.setRepository(repositories.get(0));
            } else {
                failures = true;
                result.addException(new ArtifactNotFoundException(artifact, null));
            }
            results.add(result);
        }

        if (failures) {
            throw new ArtifactResolutionException(results);
        }

        return results;
    }
}
