BUILD
-----

Build instructions for rega-genotype.

* requires Java 1.5 or later and maven2
* create jwt jar
	* checkout jwt from cvs repos
	  (:pserver:anonymous@cvs.webtoolkit.eu:/opt/cvs)
	* run ant in the jwt root dir
	* install the jwt jars in the mvn repos
		* mvn install:install-file -Dfile=jwt.2.1.5.jar
		  -DgroupId=jwt -DartifactId=jwt -Dversion=2.1.5
		  -Dpackaging=jar -DgeneratePom=true

* download jargs (http://jargs.sourceforge.net/)
	* install the downloaded file into your local maven repos with the
	  following command (replace $filetopath by the downloaded jar's
	  filename)
		* mvn install:install-file -Dfile=$pathtofile
		  -DgroupId=jargs -DartifactId=jargs -Dversion=1.0
		  -Dpackaging=jar -DgeneratePom=true

* command line:
	* go to the rega-genotype directory
	* mvn package
	* after the package procedure the target directory will contain
	  a war and jar

* eclipse:
	* install eclipse-maven plugin:
		* update-site http://m2eclipse.sonatype.org/update/
			* Maven integration for Eclipse
			* (Maven Central repository index)
	* right click project -> Run as -> Maven package
	* after the package procedure the target directory will contain a
	  war and jar
