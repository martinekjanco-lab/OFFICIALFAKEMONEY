Fake Money UI - Fabric 1.21.11 (client-side, visual only)

BUILD
 1. Install JDK 21.
 2. Easiest: generate a fresh template at https://fabricmc.net/develop (MC 1.21.11),
    then copy this project's src/ folder, build.gradle and gradle.properties over it
    (check the version numbers in gradle.properties match what the site gives you).
 3. Run:  ./gradlew build
 4. Jar is in build/libs/fakemoney-1.0.0.jar -> put in .minecraft/mods with Fabric API.

COMMANDS
 /ui                      show / hide the right-side menu
 /setmoney <amount>       500, 1k, 1m, 2.5b, 1t, 1q, 1qi ...
 /pay <name> <amount>     subtracts from your balance, chat message + ding
 /paytoggle               enable / disable /pay
