package org.fedoraproject.maven.repository.internal;

import org.codehaus.plexus.component.annotations.Component;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Component(role = org.apache.maven.repository.internal.DefaultVersionResolver.class, hint = "default")
public class MavenDefaultVersionResolver extends org.apache.maven.repository.internal.DefaultVersionResolver {
}
