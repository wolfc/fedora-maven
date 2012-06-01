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
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.test.impl.TestRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.IOException;

import static org.apache.maven.artifact.Artifact.LATEST_VERSION;
import static org.fedoraproject.maven.repository.internal.StubArtifactResolver.JPP_VERSION;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:mark.cafaro@gmail.com">Mark Cafaro</a>
 */
public class FossRepositorySystemTest {

    private FossRepositorySystem repositorySystem;

    private TestRepositorySystemSession session;

    private Artifact artifact;

    @Before
    public void setup() throws IOException {
        repositorySystem = new FossRepositorySystem();
        repositorySystem.setArtifactResolver(new StubArtifactResolver());

        session = new TestRepositorySystemSession();

        artifact = new DefaultArtifact("gid", "aid", "", "ext", "ver");
    }

    @Test
    public void testResolveArtifact() throws ArtifactResolutionException {
        StubArtifactResolver.mode = StubArtifactResolver.ResolverMode.EXACT;

        ArtifactRequest request = new ArtifactRequest(artifact, null, "");
        ArtifactResult result = repositorySystem.resolveArtifact(session, request);

        assertEquals(repositorySystem.getRepository(), result.getRepository());
        assertEquals(artifact, result.getArtifact());
    }

    @Test
    public void testResolveLatestArtifact() throws ArtifactResolutionException {
        StubArtifactResolver.mode = StubArtifactResolver.ResolverMode.LATEST;

        ArtifactRequest request = new ArtifactRequest(artifact, null, "");
        ArtifactResult result = repositorySystem.resolveArtifact(session, request);

        assertEquals(repositorySystem.getRepository(), result.getRepository());

        Artifact resolved = result.getArtifact();
        assertEquals(artifact.getGroupId(), resolved.getGroupId());
        assertEquals(artifact.getArtifactId(), resolved.getArtifactId());
        assertEquals(LATEST_VERSION, resolved.getVersion());
    }

    @Test
    public void testResolveJppArtifact() throws ArtifactResolutionException {
        StubArtifactResolver.mode = StubArtifactResolver.ResolverMode.JPP;
        repositorySystem.setUseJpp(true);

        ArtifactRequest request = new ArtifactRequest(artifact, null, "");
        ArtifactResult result = repositorySystem.resolveArtifact(session, request);

        assertEquals(repositorySystem.getRepository(), result.getRepository());

        Artifact resolved = result.getArtifact();
        assertEquals(artifact.getGroupId(), resolved.getGroupId());
        assertEquals(artifact.getArtifactId(), resolved.getArtifactId());
        assertEquals(JPP_VERSION, resolved.getVersion());
    }
}
