# Coursework 1

Mobile Platform Development coursework 1 – Cian MacDonald (S2426804), Glasgow Caledonian University.

An Android app (Java, XML layouts) that shows Traffic Scotland's unplanned incidents and current roadworks. It reads the DATEX II feeds, which it parses with `XmlPullParser`. You can browse the incidents and roadworks, search by road or date, find what's near a point, and see everything on a Google map.

## Opening the project

1. In Android Studio, choose **File → Open** and select this folder, or use **Get from VCS** with the repository URL.
2. Let Gradle sync, then run the `app` configuration.

## API keys

The keys are **not** in this repository. They are read from `local.properties`, which is git-ignored, and passed into the manifest as placeholders. To run the app with live data and the map, add these lines to `local.properties` in the project root:

```
TS_CLIENT_ID=your Traffic Scotland client ID
TS_CLIENT_KEY=your Traffic Scotland client key
MAPS_API_KEY=your Google Maps SDK for Android key
```

If the keys are missing, they default to `KEY_HERE` and the project still builds and runs. The lists then show the bundled sample feeds (`app/src/main/assets`) with a message explaining that live data couldn't be loaded, and the map tiles won't load.

## Credits

Icons: Font Awesome Free by Fonticons, Inc., licensed under CC BY 4.0 (https://fontawesome.com/license/free).
Data: Traffic Scotland DATEX II feeds (UnplannedEvents and CurrentRoadworks).
