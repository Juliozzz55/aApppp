# Simple Calendar (Android, Kotlin + Jetpack Compose + Room)

Offline calendar: monthly grid, event dots, add/edit/delete events, upcoming list.
No permissions, no account, no internet.

## Open & run
1. Install Android Studio (Koala 2024.1 or newer; any recent version with JDK 17+ works).
2. File > Open > select this `SimpleCalendar` folder. Wait for Gradle sync (needs internet once, to download dependencies).
3. Plug in your phone (USB debugging on) or start an emulator and press Run.

## Debug APK
Build > Build Bundle(s) / APK(s) > Build APK(s)
Output: app/build/outputs/apk/debug/app-debug.apk

## Signed release APK
Build > Generate Signed App Bundle / APK > APK > create a new keystore > choose "release" > Finish.
Output: app/release/app-release.apk (Android Studio shows a "locate" link).

Command-line alternative: create `keystore.properties` in the project root:
    storeFile=my-release-key.jks
    storePassword=YOUR_PASSWORD
    keyAlias=my-key
    keyPassword=YOUR_PASSWORD
then run `./gradlew assembleRelease`
Output: app/build/outputs/apk/release/app-release.apk

(If `gradlew` is missing, run `gradle wrapper --gradle-version 8.7` once, or just use Android Studio.)

## Supabase setup (v2)
1. In `app/src/main/java/com/example/simplecalendar/data/SupabaseApi.kt` set `BASE_URL` to your Supabase Project URL.
2. In Supabase SQL Editor run the `events` table + RLS policy script, and (optional) turn off "Confirm email".
