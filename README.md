# The Everything Game

You have (re)discovered the source to the server and GWT client for The Everything Game.
Congratulations!

## Running and testing

To build and run the server for development and testing, do the following:

    % mvn package
    % ./bin/everyserver

If you have access to JRebel, set REBEL_HOME to your JRebel installation and the server will
automatically run itself using JRebel. Then you can recompile server classes (via SBT or Eclipse or
"mvn compile -pl server -am") and they will be automatically reloaded by the running server.

To build and run the GWT client for development and testing, do the following:

    % mvn integration-test -Pdevmode

This will compile the GWT app in superdevmode, and run the superdevmode server which will handle
incremental recompiling of the GWT code as you modify and reload it. For more info on GWT's
superdevmode, see:

http://www.gwtproject.org/articles/superdevmode.html

## Shipping

If your name is MDB and you have the right git remotes set up, and the proper privileges, you can
ship code to the Everything Candidate deployment like so:

    % git push staging

This will push the code to the Heroku staging deployment, which will automatically rebuild and
redeploy it. You can then test said code on Facebook at:

https://apps.facebook.com/everythingcandidate/

You can view the candidate server logs thusly:

    heroku logs -t --app everything-candidate

When you're ready to ship it to the world, push like so:

    % git push heroku

You can view the server logs thusly:

    heroku logs -t --app everything

Don't forget to do a plain old `git push` to upload your commits to the Github repo as well!
