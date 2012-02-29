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
import java.util.Map;

import org.sonatype.aether.artifact.Artifact;

/**
 * This thing is to please the MavenPluginValidator.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class JPPArtifact implements Artifact {
    private Artifact delegate;

    JPPArtifact(final Artifact delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getGroupId() {
        return delegate.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return delegate.getArtifactId();
    }

    @Override
    public String getVersion() {
        return delegate.getVersion();
    }

    @Override
    public Artifact setVersion(String version) {
        System.err.println("JPPArtifact: setVersion(" + version + ")");
        delegate = delegate.setVersion(version);
        return this;
    }

    @Override
    public String getBaseVersion() {
        // bypass the unresolved version
        final String version = getVersion();
        assert version != null;
        return version;
    }

    @Override
    public boolean isSnapshot() {
        // JPP should not contain snapshots
        assert delegate.isSnapshot() == false;
        return delegate.isSnapshot();
    }

    @Override
    public String getClassifier() {
        return delegate.getClassifier();
    }

    @Override
    public String getExtension() {
        return delegate.getExtension();
    }

    @Override
    public File getFile() {
        return delegate.getFile();
    }

    @Override
    public Artifact setFile(File file) {
        delegate = delegate.setFile(file);
        return this;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return delegate.getProperty(key, defaultValue);
    }

    @Override
    public Map<String, String> getProperties() {
        return delegate.getProperties();
    }

    @Override
    public Artifact setProperties(Map<String, String> properties) {
        delegate = delegate.setProperties(properties);
        return this;
    }
}
