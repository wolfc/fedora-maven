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
