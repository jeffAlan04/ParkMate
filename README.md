## ParkMate Project
Mobile Applications Lab project \
Bachelor's Degree in Computer Science for Management \
Academic year 2025/2026

Android app for parking management: it tracks where and when you left each vehicle, how much you are spending and when a ticket expires.

## Features

- **Vehicles:** register cars, motorcycles and bicycles, with plate and a type icon.
- **Parking:** start a free, hourly or ticket stop, using your current position or a saved place, with a photo taken through the system camera.
- **Summary:** elapsed time (or remaining time for tickets) and accrued cost, updated every second.
- **Expiry notifications:** a warning with the advance chosen by the user and an alert at ticket expiry.
- **Saved places and geofencing:** on arrival at a place the app offers to start a parking; on departure it offers to end it if the vehicle is still parked.
- **Map:** parking locations on Google Maps, with a heat map.
- **History and statistics:** past stops, total spending and a breakdown by parking type.
- **Light and dark theme**, following the system setting.

## Prerequisites

Make sure you have:

- A recent **Android Studio**, with **JDK 17+** (bundled with Android Studio)
- A device or emulator running **Android 7.0 (API 24)** or higher, with **Google Play Services**
- A **Maps SDK for Android** API key (Google Cloud Console)

Permissions requested by the app: notifications, precise and background location, camera.

## First setup

Clone the repository:

~~~sh
git clone https://github.com/jeffAlan04/ParkMate.git
~~~

Add the Maps key to the `local.properties` file in the project root (the file is not tracked by Git):

~~~properties
MAPS_API_KEY=your_key_here
~~~

## Running

Open the project in Android Studio, wait for the Gradle sync and run the app on a device or emulator.

Alternatively, from a terminal with a connected device:

~~~sh
./gradlew installDebug
~~~

> To try notifications and geofencing, use a real device or an emulator with simulated location.
