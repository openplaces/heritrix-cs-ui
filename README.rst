Heritrix Continuous Seeding UI
==============================

Heritrix UI plugin for exposing continuous seeding via action directories


Getting Started
---------------

1) Download a version of heritrix-cs-ui-_.jar and copy it into the **lib** folder of your Heritrix install.

2) Run Heritrix with the following appended to the script (all previous arguments are still valid):

   > CLASS_MAIN='com.openplaces.heritrix.HeritrixCS' bin/heritrix -a admin:admin


Building
--------

To build against another version of Heritrix:

1) Obtain the heritrix-cs-ui source from http://github.com/openplaces/heritrix-cs-ui

2) Open up **build.xml** and modify the "heritrix-version" property, and "version" to your own custom value.

3) Add a folder in **lib** named "heritrix-{heritrix-version}" that corresponds to the version you defined in the previous step.

4) Run "ant" from the command line and your newly packaged jar should be in the **target** folder.

