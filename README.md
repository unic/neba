

![NEBA](README/neba-logo.png "NEBA logo") 

Lightning fast and simple content mapping for Apache Sling and Adobe AEM
====

[![codecov](https://codecov.io/gh/unic/neba/branch/develop/graph/badge.svg)](https://codecov.io/gh/unic/neba/)
![CI workflow](https://github.com/unic/neba/actions/workflows/ci.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/io.neba/io.neba.neba-parent.svg)](https://search.maven.org/search?q=a:io.neba.neba-parent)

NEBA in a nutshell
--------------------
Map Content using a lightweight, [well documented API](https://neba.io/documentation.html).
Use lazy loading and great tooling to deliver high-performance applications. Completely compatible
with Sling Models, HTL (Sightly), JSP and any application build atop the Sling API.

Optional Spring integration
----------------------
NEBA optionally integrates the Spring framework, making available all of Spring's features, including Spring MVC.
Spring is integrated using [gemini blueprint](https://www.eclipse.org/gemini/blueprint/), the OSGi Blueprint specification reference implementation.  

Downloading artifacts
----------------------
NEBA releases are published to maven central. The configuration as well as further information are available at [https://neba.io/download.html](https://neba.io/download.html).

Documentation
--------------------
The project documentation resides at [https://neba.io/](https://neba.io/).

License and included licenses
--------------------
NEBA is licensed under the terms of the Apache License, version 2.0. For the licenses of included products,
see [NOTICE](NOTICE.txt)

Getting support
--------------------
Consult the documentation or ask a question in the site comments at [https://neba.io](https://neba.io), Tweet to [@nebaframework](https://www.twitter.com/nebaframework) ask a question at [Stack overflow](https://stackoverflow.com/search?q=neba) or drop us a mail at neba at unic.com.

Building from source
--------------------
NEBA uses a [Maven](https://maven.apache.org/) based build. invoking

    mvn clean install
    
In the project's root directory will build and install NEBA. We are using [git flow](https://nvie.com/posts/a-successful-git-branching-model/),
yo you might want to do so on the "develop" branch.

Releasing NEBA
--------------------

### Summary
NEBA is released using the [maven jGitFlow plugin] (https://bitbucket.org/atlassian/jgit-flow/wiki/Home). Releasing requires modification rights for the neba github repository and the ability to sign and upload the artifacts to the sonatype OSS staging repository. Finally, the release must be accompanied by a release notes blog post published via the gh-pages branch and an announcement on Twitter. 

### Prerequisites

To release NEBA, credentials for the sonatype [OSS repository](https://oss.sonatype.org/content/repositories/) are required, and must be configured in the maven settings.xml, like so:

````
 <server>
   <id>ossrh</id>
   <username>...</username>
   <password>...</password>
 </server>
````

In addition, a GPG installation executable from the [maven-gpg-plugin](https://maven.apache.org/plugins/maven-gpg-plugin/) must be installed on the local system, e.g. [GPG4Win](https://www.gpg4win.org/) on windows. As the delivery artifacts are signed, you require a valid key pair, and the public key must have been [distributed to a public key server](https://www.gnupg.org/gph/en/manual/x457.html).
 
Furthermore, JDK 1.8 is required for building and releasing NEBA.
 
### Write a release post for the neba.io site
In a separate clone of the neba.io git repo, checkout the gh-pages branch and write a release post, such as https://github.com/unic/neba/blob/gh-pages/_posts/2016-01-22-neba-release-3.9.0.html. Testing the site locally requires running Jekyll, see https://jekyllrb.com/docs/installation/.

### Perform the release
Invoke

    mvn -Prelease jgitflow:release-start
 
and enter the desired release version. We are using the versioning scheme x.x.x, e.g. "4.10.1". All artifacts must have the same release version.

Then, invoke

    mvn -Prelease jgitflow:release-finish

Resulting, the artifacts are pushed to the sonatype OSS staging repository

### Test staged release artifacts

Login to https://oss.sonatype.org/ and select "Staging Repositories". 

![](README/repository.png)


In the list of repositories, select the io-neba staging repo. Download the AEM and Sling deliveries and test them on the local system by installing them and testing that
all contained bundles are started properly.

Then, browse the remaining artifacts (e.g. api, core) in the repository and make sure that the jar, source-jar and javadoc-jar artifacts are present.

### Release the staged repository
In https://oss.sonatype.org/, select the tested neba staging repository and click "close".

![](README/close-repository.png)

 
This triggers an automated workflow testing the repository for compliance. Once this process has finished (after a few minutes), click on "Release".

### Push the release
On you local system, push the *develop* and *master* branch as well as the tags, e.g. using

    git push --tags
    
### Publish the release blog post
simply push the new blog post on the gh-pages branch - the neba.io site is updated automatically. 

### Publication on twitter
Publications are announced via the official [@nebaframework](https://twitter.com/nebaframework) twitter channel and must contain the tag #nebaframework. Tweets with this tag are automatically featured on the neba.io home page.
    