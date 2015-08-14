NEBA
====

NEBA integrates the Spring Framework into Sling. It makes available all of Spring's features to allow using mature, best-practice solutions for the majority of system-level concerns common to Sling-based applications. NEBA does so in full compliance with the open core philosophy - using exclusively mature, standardized open source software - gemini blueprint - for the integration into OSGi.

All Spring and Sling features are accessible to developers using a lightweight, well documented API that does not couple the domain -specific implementation code to any implementation details of Sling, Spring or NEBA.

Downloading artifacts
----------------------
NEBA releases are published to maven central. The configuration as well as further information are available at 
[http://neba.io/download.html](http://neba.io/download.html).

Documentation
--------------------
The project documentation resides at [http://neba.io/](http://neba.io/).

Getting support
--------------------
Consult the documentation or ask a question in the site comments at [http://neba.io](http://neba.io),
ask a question at [Stack overflow](http://stackoverflow.com/) or drop us a mail at neba at unic.com.

Building from source
--------------------
NEBA uses a [Maven](http://maven.apache.org/) based build. invoking

    mvn clean install
    
In the project's root directory will build and install NEBA. We are using [git flow](http://nvie.com/posts/a-successful-git-branching-model/),
yo you might want to do so on the "develop" branch.

