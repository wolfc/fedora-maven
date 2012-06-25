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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.impl.ArtifactResolver;
import org.sonatype.aether.impl.internal.DefaultRepositorySystem;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.*;
import org.sonatype.aether.test.util.impl.StubVersion;
import org.sonatype.aether.transfer.ArtifactNotFoundException;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.version.Version;

import java.io.IOException;

import static org.apache.maven.artifact.Artifact.LATEST_VERSION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:mark.cafaro@gmail.com">Mark Cafaro</a>
 */
public class FossRepositorySystemTest {

    private FossRepositorySystem repositorySystem;

    private RemoteRepository fossRepository;

    @Mock private ArtifactResolver artifactResolver;

    @Mock private DefaultRepositorySystem defaultRepositorySystem;

    @Mock private RepositorySystemSession session;

    private Artifact artifact;

    @Before
    public void setup() throws IOException, ArtifactResolutionException {
        MockitoAnnotations.initMocks(this);

        // allow the session to pass validation
        // should we be stubbing the validator instead?
        session = mock(RepositorySystemSession.class, Mockito.RETURNS_MOCKS);

        repositorySystem = new FossRepositorySystem()
                .setDefaultRepositorySystem(defaultRepositorySystem)
                .setArtifactResolver(artifactResolver)
                .setUseJpp(true);

        fossRepository = repositorySystem.getRemoteRepository();
        artifact = new DefaultArtifact("gid", "aid", "", "ext", "ver");

        // default behavior
        doThrow(ArtifactResolutionException.class)
                .when(artifactResolver).resolveArtifact(
                any(RepositorySystemSession.class), any(ArtifactRequest.class));
    }

    @Test
    public void testResolveVersionRangeSuccessful()
            throws VersionRangeResolutionException {

        VersionRangeRequest request =
                new VersionRangeRequest(artifact, null, null);

        Version version = new StubVersion(null);
        VersionRangeResult successfulResult = new VersionRangeResult(request)
                .addVersion(version)
                .setRepository(version, fossRepository);

        when(defaultRepositorySystem.resolveVersionRange(
                eq(session), isValidVersionRangeRequest(artifact.getVersion())))
                .thenReturn(successfulResult);

        VersionRangeResult actualResult =
                repositorySystem.resolveVersionRange(session, request);

        assertEquals(successfulResult, actualResult);
    }

    @Test
    public void testResolveArtifactSuccessful()
            throws ArtifactResolutionException {

        ArtifactRequest request = new ArtifactRequest(artifact, null, null);

        ArtifactResult successfulResult = new ArtifactResult(request)
                .setArtifact(artifact)
                .setRepository(fossRepository);

        doReturn(successfulResult).when(artifactResolver).resolveArtifact(
                eq(session), isValidArtifactRequest(artifact.getVersion()));

        ArtifactResult actualResult =
                repositorySystem.resolveArtifact(session, request);

        assertEquals(successfulResult, actualResult);
    }

    @Test
    public void testResolveLatestArtifactSuccessful()
            throws ArtifactResolutionException {

        ArtifactRequest request = new ArtifactRequest(artifact, null, null);

        Artifact latestArtifact = new DefaultArtifact(artifact.getGroupId(),
                artifact.getArtifactId(), artifact.getClassifier(),
                LATEST_VERSION);

        ArtifactResult successfulResult = new ArtifactResult(request)
                .setArtifact(latestArtifact)
                .setRepository(fossRepository);

        doReturn(successfulResult).when(artifactResolver).resolveArtifact(
                eq(session), isValidArtifactRequest(LATEST_VERSION));

        ArtifactResult actualResult =
                repositorySystem.resolveArtifact(session, request);

        assertEquals(successfulResult, actualResult);
    }

    @Test
    public void testResolveJppArtifactSuccessful()
            throws ArtifactResolutionException {

        ArtifactRequest request = new ArtifactRequest(artifact, null, "");

        ArtifactResult successfulResult = new ArtifactResult(request)
                .setArtifact(artifact)
                .setRepository(fossRepository);

        doReturn(successfulResult).when(artifactResolver).resolveArtifact(
                isJppSession(), isValidArtifactRequest(artifact.getVersion()));

        ArtifactResult actualResult =
                repositorySystem.resolveArtifact(session, request);

        assertEquals(successfulResult, actualResult);
    }

    @SuppressWarnings("unchecked")
    @Test (expected = ArtifactResolutionException.class)
    public void testResolveArtifactUnsuccessful()
            throws ArtifactResolutionException {

        ArtifactRequest request = new ArtifactRequest(artifact, null, "");

        // relying on default artifact resolver behavior to throw exception
        repositorySystem.resolveArtifact(session, request);
    }

    // custom argument matchers
    // violating DRY a little bit here but I don't think we have much of a
    // choice given that requests share no common interface
    class IsValidVersionRangeRequest
            extends ArgumentMatcher<VersionRangeRequest> {

        private String version;

        public IsValidVersionRangeRequest(String version) {
            this.version = version;
        }

        public boolean matches(Object rangeRequest) {
            VersionRangeRequest request = ((VersionRangeRequest) rangeRequest);

            return request.getRepositories().size() == 1
                    && request.getRepositories().get(0) == fossRepository
                    && request.getArtifact().getVersion().equals(version);
        }
    }

    public VersionRangeRequest isValidVersionRangeRequest(String version) {
        return argThat(new IsValidVersionRangeRequest(version));
    }

    class IsValidArtifactRequest
            extends ArgumentMatcher<ArtifactRequest> {

        private String version;

        public IsValidArtifactRequest(String version) {
            this.version = version;
        }

        public boolean matches(Object artifactRequest) {
            ArtifactRequest request = ((ArtifactRequest) artifactRequest);

            return request.getRepositories().size() == 1
                    && request.getRepositories().get(0) == fossRepository
                    && request.getArtifact().getVersion().equals(version);
        }
    }

    public ArtifactRequest isValidArtifactRequest(String version) {
        return argThat(new IsValidArtifactRequest(version));
    }

    class IsJppSession extends ArgumentMatcher<RepositorySystemSession> {
        public boolean matches(Object session) {

            JPPLocalRepositoryManager manager =
                    repositorySystem.getJppRepositoryManager();

            return ((RepositorySystemSession) session)
                    .getLocalRepositoryManager() == manager;
        }
    }

    public RepositorySystemSession isJppSession() {
        return argThat(new IsJppSession());
    }
}
