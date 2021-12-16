# me.bbrks.dev.cbl.docreplicatortest

A simple test app to upsert documents into a CBL app, and stop and start replication against Sync Gateway. Used for manual testing of replication.

![Screenshot 2021-12-16 at 17 54 50](https://user-images.githubusercontent.com/1525809/146423723-261f6a43-cae0-4fdc-bda1-979a11f794bd.png)

## Prerequisites

- Create a user in Sync Gateway. E.g:
`curl -X PUT http://localhost:4985/db1/_user/demo -d '{"password":"password"}'`

## Instructions
1. Change constants in `MainActivity.java` to configure Sync Gateway URL, db name, and user credentials.
2. Run the app and perform actions through the App's UI for manual testing.

## Change CBL version
- CBL version defined in build.gradle

## Disclaimer

I'm not an Android developer. Lots of things are hard-coded. This is terrible code to use as a reference for anything other than running a quick test.
