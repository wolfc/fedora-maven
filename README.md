# FOSS Repository Extension

The FOSS Repository Extension (FRE) is an Apache Maven extension that causes Maven to resolve all dependencies from a single specified repository, regardless of POMs or settings files (settings.xml). By restricting Maven in this way, FRE makes it possible to enforce the use of only sanctioned components for any target platform that maintains a sanctioned repository. This allows developers to test Maven projects for dependency issues on platforms they may wish to deploy to but are not currently developing on.

## Usage

FRE is enabled through the use of the `fmvn` startup script. `fmvn` is designed to be a fully-compatible replacement for `mvn`, with the added effect that it enables FRE. All `mvn` options, goals, and phases work with `fmvn`.

The environment variable `FRE_REPO` defines the sole repository used by FRE to resolve dependencies. `FRE_REPO` may specify a local or remote repository, but must be in the form of a URL. Two valid examples are shown below:

    export FRE_REPO=file:///usr/share/maven/repository
    export FRE_REPO=https://repository.jboss.org/nexus/content/groups/sanctioned-set-one
    
In addition to the `FRE_REPO` environment variable, FRE also exposes the following properties:

*   `fre.debug` - if set FRE will cause Maven to print additional resolving information that can be useful for debugging resolver problems.
*   `fre.useJpp` - if set FRE will cause Maven to fallback to a JPP repository in the event that a dependency is not located within the primary repository. This property is Fedora-specific and allows FRE to replace `mvn-rpmbuild`.
*   `fre.depmap.file` - file containing custom dependency mapping between `groupId:artifactId:version` and jar file. This property is Fedora-specific and may be used when `fre.useJpp` is set. See http://fedoraproject.org/wiki/Java/JPPMavenReadme for dependency map file format guidelines.

FRE properties can be set by using the `-D` option when invoking `fmvn`, in the same manner as any typical Maven system property (e.g. `fmvn -Dfre.debug package`).

## Details

FRE causes Maven to resolve dependencies differently. When resolving dependencies with FRE enabled, Maven will first attempt to find the dependency by matching the `groupId:artifactId:version` in the repository specified by `FRE_REPO`. If a match does not exist, Maven will then attempt to find the *latest* version of the dependency matching the `groupId:artifactId` in the repository specified by `FRE_REPO`. If a match still does not exist, Maven will optionally (see `fre.useJpp`) attempt to find the dependency in a JPP repository. Maven will fail to build in the event that each of these routes is unsuccessful.