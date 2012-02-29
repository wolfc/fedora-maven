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

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.impl.VersionResolver;
import org.sonatype.aether.resolution.VersionRequest;
import org.sonatype.aether.resolution.VersionResolutionException;
import org.sonatype.aether.resolution.VersionResult;
import org.sonatype.aether.spi.locator.Service;
import org.sonatype.aether.spi.locator.ServiceLocator;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.spi.log.NullLogger;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Component(role = VersionResolver.class)
public class DefaultVersionResolver implements VersionResolver, Service {
    @Requirement
    private Logger logger = NullLogger.INSTANCE;

    @Requirement
    private org.apache.maven.repository.internal.DefaultVersionResolver delegate;

    @Override
    public void initService(ServiceLocator locator) {
        setLogger(locator.getService(Logger.class));
    }

    @Override
    public VersionResult resolveVersion(RepositorySystemSession session, VersionRequest request) throws VersionResolutionException {
        throw new RuntimeException("NYI: org.fedoraproject.maven.repository.internal.DefaultVersionResolver.resolveVersion");
    }

    public DefaultVersionResolver setDefaultVersionResolver(org.apache.maven.repository.internal.DefaultVersionResolver delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("default version resolver has not been specified");
        }
        this.delegate = delegate;
        return this;
    }

    public DefaultVersionResolver setLogger(Logger logger) {
        this.logger = (logger != null) ? logger : NullLogger.INSTANCE;
        return this;
    }
}
