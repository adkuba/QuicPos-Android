# Table of contents
  - [What I've learned](#what-ive-learned)
  - [Important](#important)
  - [Android](#android)
    - [Structure](#structure)
    - [Other](#other)
  - [Apollo](#apollo)
  - [SSL](#ssl)
    - [Add certificate to the local memory](#add-certificate-to-the-local-memory)



# What I've learned
- How to create client application in **Kotlin** for Android devices
- Working with **Apollo GraphQL**
- Deploying application to the **Play Store**
- Checking and configuring **SSL**



# Important
- Create <code>AppVariables.kt</code> file with data class containing password String value and default constructor.



# Android
### Structure
Notes about main Android application structure:
- Activity - one view, window, screen, similar to frontend's views. We can switch between different activities with for example Intents or Navigation Controller. There can be Scroll Activity, Nacigation Activity etc.
- Fragment - just like frontend's components. There can be multiple Fragments in one Activity, we can reuse them.
- Intent - like an action, for example action to open another activity or to look for pictures in gallery.
- Adapters - list views have adapters to display for example multiple, the same structure, fragments in this list.
- Menu - define your own menu and replace default.
- Other .xml files - define variables (strings, ints...), color values and styles/themes definition for your app.

### Other
- Interesting code, method to infrom about click from <code>SavedListAdapter</code>to <code>Saved</code> activity. I implemented listener through interface.
- In previous versions to store already downloaded posts I used <code>ViewModel</code> structure - I think it works similar to for example Vue Store. Now I'm using static object.



# Apollo
Similar to iOS [application](https://github.com/adkuba/QuicPos-IOS), see [tutorial](https://www.apollographql.com/docs/android/essentials/get-started-java/). To download schema from the server, execute in main folder:
```sh
./gradlew downloadApolloSchema --endpoint="https://akuba.pl/api/quicpos/query" --schema="app/src/main/graphql/com/example/schema.json"
``` 



# SSL
During downloading schema I came across an error <code>TSL handshake error</code> related to badly configured SSL certificate on my server. The solution:
- Check if server has good SSL certificate: <code>openssl s_client -debug -connect www.api.quicpos.com:443</code>
- In my case, crt file on [server](https://github.com/adkuba/QuicPos-Server) was missing Intermediate CA part. I added it to the end of the main certificate (open it in notepad) and it solved my problem. You may need to add it to the beginning.

### Add certificate to the local memory
If you can't repair SSL on server, add untrusted certificate to the local memory. HTTPS connection will work only on your machine:
- Java has special keystore, my was located in: <code>/etc/ssl/certs/java/cacert</code>
- Add downloaded (through browser) certificate to the keystore: <code>
sudo $JAVA_HOME/bin/keytool -import -alias quicposapi -keystore ./cacerts -file ~/Downloads/www.api.quicpos.com</code>
- Reboot