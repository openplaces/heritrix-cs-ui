Heritrix Continuous Seeding UI
==============================

Heritrix UI plugin for exposing continuous seeding via action directories


Installation and start up
-------------------------

1) Download a version of heritrix-cs-ui-_.jar and copy it into the **lib** folder of your Heritrix install.

2) Run Heritrix with the following env variable prepended to the script (all previous arguments are still valid):

   > CLASS_MAIN='com.openplaces.heritrix.HeritrixCS' bin/heritrix -a admin:admin


Usage
-----

1) From the Heritrix web ui, append "/seeding" to the job's base url to visit the continuous seeding page. For example, if your job's url is "https://localhost:8443/engine/job/myjob1", then "https://localhost:8443/engine/job/myjob1/seeding" will bring you to the continuous seeding page.

2) Add any number of seeds with a newline separating each entry. Verify that the flash message says "Seeds added: ..." after you submit the form.


Building
--------

To build against another version of Heritrix:

1) Obtain the heritrix-cs-ui source from http://github.com/openplaces/heritrix-cs-ui

2) Install Gradle (http://www.gradle.org/installation.html)

3) Open up **build.gradle** and modify the "heritrix_version" property, and "version" to your own custom value.

4) Run "gradle jar" from the command line and your newly packaged jar should be in the **build/libs** folder.

