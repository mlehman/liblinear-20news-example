# Liblinear-java 20 Newsgroups Example

An example showing how to use [liblinear-java](http://liblinear.bwaldvogel.de) for text classification in Scala with the 20 newsgroups data set. Additionally, this example shows how [Apache Lucene](http://lucene.apache.org/) can be leveraged for tokenizing, shingling, and more.

##Downloading the Data Set

The 20 newsgroups data set is available here. This example requires the "bydate" version.

- [http://qwone.com/~jason/20Newsgroups/](http://qwone.com/~jason/20Newsgroups/)
- [Direct Download - http://qwone.com/~jason/20Newsgroups/20news-bydate.tar.gz](http://qwone.com/~jason/20Newsgroups/20news-bydate.tar.gz)

Unzip and place this directory in the root of this project.


## Building & Running the Example

Requires java and [sbt](http://www.scala-sbt.org/). 

Building and running:

    $ sbt run
