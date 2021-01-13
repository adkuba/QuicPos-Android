# QuicPos-Android

Z tego co rozumiem aplikacje w Androidzie działają następująco:
  - Najważniejsze są activity - to jest taki jeden ekran, okienko - tak jak na froncie jest to View. Możemy się przemieszczać między różnymi activity. Może być Scroll Activity, Navigation Activity itd.
  - Następnie mamy fragments, są to takie jakby components na frontendzie. Może ich być kilka w ramach jednego activity, a przejścia między nimi może definiować Navigation Controller.

## Schema
Pobieranie schema. W głównym folderze wykonać:
```sh
./gradlew downloadApolloSchema --endpoint="https://api.quicpos.com/query" --schema="app/src/main/graphql/com/example/schema.json"
``` 

# SSL (permanent fix)
Wykonując query w aplikacji na androida ponownie natrafiłem na błąd TSL handshake error. Złym rozwiązaniem byłoby ponowne dodanie certyfikatu to jakiejś pamięci javy w androidzie. **Ewidentnie jest jakiś błąd z certyfikatem na serwerze.** Rozwiązanie:
- Mogę sprawdzić czy serwer ma dobry SSL poprzez komendę <code>openssl s_client -debug -connect www.api.quicpos.com:443
</code> Jeśli jest coś nie tak prawdopodobnie pojawi się błąd weryfikacji.
- Okazało się że u mnie brakowało Łańcucha certyfikatu (Intermediate CA). Wystarczy łańcuch dokleić na koniec certyfikatu głównego (plik .crt otworzyć w notatniku np). Może być na odwrót. To rozwiązało mój problem.

# SSL java (temp fix)
Przy pobieraniu schama przez apollo graphql wyskoczył mi błąd:
```
Execution failed for task ':app:downloadApolloSchema'. > javax.net.ssl.SSLHandshakeException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```

Jest on związany z tym że java nie ma zapisanego certyfikatu <code>www.api.quicpos.com</code>

## Dodawanie certyfikatu do pamięci javy
1) Java ma zasobnik na klucze, mój był w <code>/etc/ssl/certs/java/cacert</code> Ścieżkę można znaleźć przez wejście do folderu <code></code>
2) Wchodzimy na moją stronę i pobieramy certyfikat poprzez przeglądarkę (export po wejściu w details certyfikatu) - zapisujemy jako <code>Base64 encoded ASCII single certificate</code>
3) W folderze w którym jest cacert dodajemy do niego certyfikat komendą:
<code>
sudo $JAVA_HOME/bin/keytool -import -alias quicposapi -keystore ./cacerts -file ~/Downloads/www.api.quicpos.com
</code>
w Downloads mamy ten pobrany certyfikat.
4) Robimy reboot, opcjonalnie sprawdzamy za pomocą pliku SSLPoke.class jest u mnie w repo, komendą:
<code>
java SSLPoke api.quicpos.com 443
</code>
Czy wszystko dobrze się łączy. Powinniśmy dostać success.


## Listener
Bardzo ciekawy kod wykorzystałem przy informowaniu z SavedListAdapter do activity Saved. Mam listener poprzez interfejs!