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
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.impl.ArtifactResolver;
import org.sonatype.aether.impl.internal.DefaultRepositorySystem;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.*;
import org.sonatype.aether.transfer.MetadataNotFoundException;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.IOException;

import static org.apache.maven.artifact.Artifact.LATEST_VERSION;
import static org.junit.Assert.*;
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
    public void setup()
            throws IOException, ArtifactResolutionException,
            VersionRangeResolutionException, ArtifactDescriptorException {

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

        // default behavior for stubs
        VersionRangeResult emptyVersionRange =
                new VersionRangeResult(new VersionRangeRequest())
                        .addException(mock(MetadataNotFoundException.class));

        doReturn(emptyVersionRange)
                .when(defaultRepositorySystem).resolveVersionRange(
                any(RepositorySystemSession.class), any(VersionRangeRequest.class));

        doThrow(ArtifactDescriptorException.class)
                .when(defaultRepositorySystem).readArtifactDescriptor(
                any(RepositorySystemSession.class), any(ArtifactDescriptorRequest.class));

        doThrow(ArtifactResolutionException.class)
                .when(artifactResolver).resolveArtifact(
                any(RepositorySystemSession.class), any(ArtifactRequest.class));
    }

    @Test
    public void testResolveVersionRangeSuccessful()
            throws VersionRangeResolutionException {

        VersionRangeRequest request =
                new VersionRangeRequest(artifact, null, null);

        VersionRangeResult successfulResult = new VersionRangeResult(request);
        doReturn(successfulResult).when(defaultRepositorySystem)
                .resolveVersionRange(
                        eq(session), isValidVersionRangeRequest(artifact));

        VersionRangeResult actualResult =
                repositorySystem.resolveVersionRange(session, request);

        assertEquals(successfulResult, actualResult);
    }

    @Test
    public void testResolveVersionRangeUnsuccessful()
            throws VersionRangeResolutionException {

        VersionRangeRequest request =
                new VersionRangeRequest(artifact, null, null);

        // relying on default repository system to return empty range
        VersionRangeResult result =
                repositorySystem.resolveVersionRange(session, request);

        assertTrue(result.getVersions().isEmpty());
    }

    @Test
    public void testReadArtifactDescriptorSuccessful()
            throws ArtifactDescriptorException {

        ArtifactDescriptorRequest request =
                new ArtifactDescriptorRequest(artifact, null, null);

        ArtifactDescriptorResult successfulResult =
                new ArtifactDescriptorResult(request);
        doReturn(successfulResult).when(defaultRepositorySystem)
                .readArtifactDescriptor(
                        eq(session), isValidArtifactDescriptorRequest());

        ArtifactDescriptorResult actualResult =
                repositorySystem.readArtifactDescriptor(session, request);

        assertEquals(successfulResult, actualResult);
    }

    @Test (expected = ArtifactDescriptorException.class)
    public void testReadArtifactDescriptorUnsuccessful()
            throws ArtifactDescriptorException {

        ArtifactDescriptorRequest request =
                new ArtifactDescriptorRequest(artifact, null, null);

        // relying on default repository system to throw exception
        repositorySystem.readArtifactDescriptor(session, request);
    }

    @Test
    public void testResolveArtifactSuccessful()
            throws ArtifactResolutionException {

        ArtifactRequest request = new ArtifactRequest(artifact, null, null);

        ArtifactResult successfulResult = new ArtifactResult(request);
        doReturn(successfulResult).when(artifactResolver).resolveArtifact(
                eq(session), isValidArtifactRequest(artifact));

        ArtifactResult actualResult =
                repositorySystem.resolveArtifact(session, request);

        assertEquals(successfulResult, actualResult);
    }

    @Test
    public void testResolveLatestArtifactSuccessful()
            throws ArtifactResolutionException {

        ArtifactRequest request = new ArtifactRequest(artifact, null, null);

        ArtifactResult successfulResult = new ArtifactResult(request);
        Artifact latestArtifact = artifact.setVersion(LATEST_VERSION);
        doReturn(successfulResult).when(artifactResolver).resolveArtifact(
                eq(session), isValidArtifactRequest(latestArtifact));

        ArtifactResult actualResult =
                repositorySystem.resolveArtifact(session, request);

        assertEquals(successfulResult, actualResult);
    }

    @Test
    public void testResolveJppArtifactSuccessful()
            throws ArtifactResolutionException {

        ArtifactRequest request = new ArtifactRequest(artifact, null, "");

        ArtifactResult successfulResult = new ArtifactResult(request);
        doReturn(successfulResult).when(artifactResolver).resolveArtifact(
                isJppSession(), isValidArtifactRequest(artifact));

        ArtifactResult actualResult =
                repositorySystem.resolveArtifact(session, request);

        assertEquals(successfulResult, actualResult);
    }

    @Test (expected = ArtifactResolutionException.class)
    public void testResolveArtifactUnsuccessful()
            throws ArtifactResolutionException {

        ArtifactRequest request = new ArtifactRequest(artifact, null, "");

        // relying on default artifact resolver behavior to throw exception
        repositorySystem.resolveArtifact(session, request);
    }

    // custom argument matchers
    // violating DRY here but I don't think we have much of a choice given that
    // requests share no common interface
    class IsValidVersionRangeRequest
            extends ArgumentMatcher<VersionRangeRequest> {

        private Artifact artifact;

        public IsValidVersionRangeRequest(Artifact artifact) {
            this.artifact = artifact;
        }

        public boolean matches(Object rangeRequest) {
            VersionRangeRequest request = ((VersionRangeRequest) rangeRequest);

            return request.getRepositories().size() == 1
                    && request.getRepositories().get(0) == fossRepository
                    && request.getArtifact().equals(artifact);
        }
    }

    public VersionRangeRequest isValidVersionRangeRequest(Artifact artifact) {
        return argThat(new IsValidVersionRangeRequest(artifact));
    }

    class IsValidArtifactDescriptorRequest
            extends ArgumentMatcher<ArtifactDescriptorRequest> {

        public boolean matches(Object descriptorRequest) {
            ArtifactDescriptorRequest request =
                    ((ArtifactDescriptorRequest) descriptorRequest);

            return request.getRepositories().size() == 1
                    && request.getRepositories().get(0) == fossRepository;
        }
    }

    public ArtifactDescriptorRequest isValidArtifactDescriptorRequest() {
        return argThat(new IsValidArtifactDescriptorRequest());
    }

    class IsValidArtifactRequest
            extends ArgumentMatcher<ArtifactRequest> {

        private Artifact artifact;

        public IsValidArtifactRequest(Artifact artifact) {
            this.artifact = artifact;
        }

        public boolean matches(Object artifactRequest) {
            ArtifactRequest request = ((ArtifactRequest) artifactRequest);

            return request.getRepositories().size() == 1
                    && request.getRepositories().get(0) == fossRepository
                    && request.getArtifact().equals(artifact);
        }
    }

    public ArtifactRequest isValidArtifactRequest(Artifact artifact) {
        return argThat(new IsValidArtifactRequest(artifact));
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
