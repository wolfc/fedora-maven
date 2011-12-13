package org.fedoraproject.maven.repository.internal;

import org.apache.maven.plugin.version.internal.DefaultPluginVersionResolver;
import org.codehaus.plexus.component.annotations.Component;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Component(role = DefaultPluginVersionResolver.class, hint = "default")
public class MavenDefaultPluginVersionResolver extends DefaultPluginVersionResolver {
}
