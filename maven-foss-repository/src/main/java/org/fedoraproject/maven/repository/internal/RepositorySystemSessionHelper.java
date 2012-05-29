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

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class RepositorySystemSessionHelper {
    protected static void invalidSession(String name) {
        throw new IllegalArgumentException("Invalid repository system session: " + name + " is not set.");
    }

    protected static void validateSession(RepositorySystemSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Invalid repository system session: the session may not be null.");
        }
        if (session.getLocalRepositoryManager() == null) {
            invalidSession("LocalRepositoryManager");
        }
        if (session.getSystemProperties() == null) {
            invalidSession("SystemProperties");
        }
        if (session.getUserProperties() == null) {
            invalidSession("UserProperties");
        }
        if (session.getConfigProperties() == null) {
            invalidSession("ConfigProperties");
        }
        if (session.getMirrorSelector() == null) {
            invalidSession("MirrorSelector");
        }
        if (session.getProxySelector() == null) {
            invalidSession("ProxySelector");
        }
        if (session.getAuthenticationSelector() == null) {
            invalidSession("AuthenticationSelector");
        }
        if (session.getArtifactTypeRegistry() == null) {
            invalidSession("ArtifactTypeRegistry");
        }
        if (session.getDependencyTraverser() == null) {
            invalidSession("DependencyTraverser");
        }
        if (session.getDependencyManager() == null) {
            invalidSession("DependencyManager");
        }
        if (session.getDependencySelector() == null) {
            invalidSession("DependencySelector");
        }
        if (session.getDependencyGraphTransformer() == null) {
            invalidSession("DependencyGraphTransformer");
        }
        if (session.getData() == null) {
            invalidSession("Data");
        }
    }
}
