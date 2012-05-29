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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugin.version.PluginVersionResult;
import org.apache.maven.plugin.version.internal.DefaultPluginVersionResolver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.repository.ArtifactRepository;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.spi.log.NullLogger;

/**
 * The RepositorySystem must return a File in Metadata, which is not available
 * when using the JPP repository. So we need to bypass resolving plugins earlier.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Component(role = PluginVersionResolver.class)
public class FossPluginVersionResolver implements PluginVersionResolver {
    @Requirement
    private Logger logger = NullLogger.INSTANCE;

    @Requirement(hint = "default")
    private DefaultPluginVersionResolver delegate;

    @Override
    public PluginVersionResult resolve(PluginVersionRequest request)
            throws PluginVersionResolutionException {

//        final PluginVersionRequest alternate =
//                new DefaultPluginVersionRequest(plugin(request),
//                        request.getRepositorySession(),
//                        remotes()).setPom(request.getPom());

        logger.warn("No version specified for plugin " + request.getGroupId() +
                ":" + request.getArtifactId() + ", falling back to RELEASE");
        //final PluginVersionResult result = delegate.resolve(request);
//        logger.warn("Would have been " + result.getVersion() + " from " +
//                result.getRepository());
        return new PluginVersionResult() {
            @Override
            public String getVersion() {
                return Artifact.RELEASE_VERSION;
            }

            @Override
            public ArtifactRepository getRepository() {
                throw new RuntimeException("NYI: .getRepository");
            }
        };
    }

    public FossPluginVersionResolver setDefaultPluginVersionResolver(
            DefaultPluginVersionResolver delegate) {

        if (delegate == null) {
            throw new IllegalArgumentException(
                    "default plugin version resolver has not been specified");
        }
        this.delegate = delegate;
        return this;
    }

    public FossPluginVersionResolver setLogger(Logger logger) {
        this.logger = (logger != null) ? logger : NullLogger.INSTANCE;
        return this;
    }
}
