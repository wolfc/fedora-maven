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

import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.impl.internal.DefaultRepositorySystem;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.*;
import org.sonatype.aether.test.util.impl.StubVersion;
import org.sonatype.aether.version.Version;

import java.util.List;

/**
 * @author <a href="mailto:mark.cafaro@gmail.com">Mark Cafaro</a>
 */
public class StubDefaultRepositorySystem extends DefaultRepositorySystem {

    @Override
    public VersionRangeResult resolveVersionRange(
            RepositorySystemSession session,
            VersionRangeRequest request)
            throws VersionRangeResolutionException {

        VersionRangeResult result = new VersionRangeResult(request);

        Version version = new StubVersion("");
        result.addVersion(version);

        TestUtils.assertSingleRepository(request.getRepositories());
        result.setRepository(version, request.getRepositories().get(0));

        return result;
    }
}
