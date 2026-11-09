# DuoDuo

Pierwsza wersja natywnej aplikacji na Androida do krótkich lekcji językowych i programistycznych. Kotlin + Jetpack Compose. Interfejs po polsku. Działa lokalnie, bez kont, serwera, reklam i uprawnień sieciowych.

## Uruchomienie

Otwórz projekt w Android Studio obsługującym Android Gradle Plugin 8.9.2, ustaw JDK 17 i zainstaluj Android SDK 35. Ustaw `sdk.dir` w lokalnym, ignorowanym przez Git pliku `local.properties`. Telefon: Android 8.0 lub nowszy (API 26+).

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
```

APK powstaje w `app/build/outputs/apk/debug/app-debug.apk`. Można zainstalować go na telefonie z pliku lub przez `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Jest to kompilacja testowa, nie wydanie do sklepu.

Wersje narzędzi są przypięte; [oficjalna tabela zgodności AGP 8.9](https://developer.android.com/build/releases/agp-8-9-0-release-notes) wskazuje Gradle 8.11.1 i JDK 17.

## Dostępne funkcje

- Biblioteka kursów, wybór lekcji i osobna karta powtórek.
- Import pojedynczego pliku JSON przez systemowy wybór pliku, walidacja i podgląd.
- Osiem wariantów pytań: wybór jednej/wielu odpowiedzi, wpisanie słowa, wpisywanie luk w zdaniu, bank słów, zaznaczanie słów, układanie kafelków, podpis obrazka i wybór odpowiedzi do obrazka.
- Losowy dobór do ośmiu różnych ćwiczeń w sesji, z preferencją dla nowych zadań i wymagających powtórki.
- Błędne zadanie wraca po maksymalnie dwóch innych ćwiczeniach, aż zostanie zaliczone. Nie ma limitu żyć, podpowiedzi ani kar blokujących naukę.
- Po błędzie widoczna jest poprawna odpowiedź. Po literówce widoczne są wpisana i poprawna odpowiedź.
- Utrwalanie: odstępy 1, 3, 7, 14, 30 dni po kolejnych poprawnych próbach. Błąd zeruje serię. Literówka jest zaliczana, ale ustawia powtórkę na kolejny dzień.
- Postęp, wyniki zatwierdzonych odpowiedzi i stan lekcji zapisywane atomowo w prywatnym katalogu aplikacji. Wznowienie nie nalicza ponownie ostatniej odpowiedzi. Przycisk „Zapisz i wróć” zapisuje także aktualnie wpisaną odpowiedź.
- Dwa wbudowane kursy demonstracyjne po osiem ćwiczeń. To przykłady formatu, a nie zatwierdzony materiał badawczy.

## Treści kursów

Zobacz [opis formatu](docs/COURSE_FORMAT.md), [schemat JSON](courses/course.schema.json) oraz przykłady [językowy](courses/language-demo.json) i [programistyczny](courses/programming-demo.json).

Gotowy kurs [Włoski — codzienność i świat zwierząt](courses/italian.json) powstał na podstawie [notatek z zajęć](courses/italian.md). Zawiera 77 lekcji po 2–8 ćwiczeń, łącznie 542 ćwiczenia i 225 haseł słownikowych. Obejmuje wybór jednej i wielu odpowiedzi, wpisywanie słów, luki w zdaniach, bank słów, zaznaczanie i układanie zdań. Aby go dodać, przenieś `italian.json` na telefon i wybierz w aplikacji „Importuj kurs z JSON”; przebudowanie APK nie jest potrzebne.

W kursie włoskim zachowano materiał z sześciu zajęć, usunięto powtórzenia słownictwa i dodano polskie objaśnienia. Przysłowia przepisano z osadzonej planszy. Pytania o zwierzęta służą ćwiczeniu języka, bez dopisywania odpowiedzi zoologicznych, których nie było w notatkach. Pominięto niepewne rozróżnienie `calamaro/totano` oraz ćwiczenia obrazkowe: podpisy na ilustracjach zdradzałyby odpowiedzi. Rozróżnienia gramatyczne i podobne słowa, np. `visto/vista` oraz `mente/mento`, wymagają dokładnej pisowni. Apostrof prosty i typograficzny są akceptowane jako równoważne warianty odpowiedzi. Kurs można odtworzyć przez `python3 scripts/build_italian_course.py`; po późniejszej zmianie jego treści należy zwiększyć `version` w generatorze.

Obrazki PNG/JPEG są osadzone w `imageBase64`; plik jest samowystarczalny i działa offline. Limit pliku to 8 MB, obrazka 2 MB i 2048 × 2048 px. Import nie pobiera zasobów z internetu. ZIP i zewnętrzne ścieżki do obrazków nie są obsługiwane w pierwszej wersji.

Nowa treść istniejącego kursu wymaga większej `version`. Import nowej wersji zastępuje poprzednią i rozpoczyna jej postęp od nowa; podgląd wyraźnie o tym informuje. Ponowny import identycznego kursu zachowuje postęp. Limit: 20 kursów i 24 MB tekstu Base64 łącznie.

## Organizacja kodu

- `Models.kt` — format kursów, odpowiedzi, postępu i zapisanej sesji.
- `CourseValidator.kt` — ścisła walidacja importu i identyfikatorów.
- `LearningEngine.kt` — ocenianie, literówki, planowanie i kolejka powtórek, bez zależności od Androida.
- `CourseRepository.kt` — odczyt plików, kontrola obrazków i zapis atomowy.
- `LearningViewModel.kt` — obsługa akcji, importu i stanu ekranów.
- `MainActivity.kt` — interfejs Compose.

Przykłady i proste diagramy demonstracyjne można odtworzyć przez `python3 scripts/create_examples.py` (wymaga Pillow). Skrypt zapisuje kopie kursów do `courses/` i zasobów aplikacji.

## Świadome ograniczenia tej wersji

Nie ma panelu badacza, identyfikacji uczestników, eksportu badawczego, synchronizacji ani powiadomień. Kod nie jest wykonywany. Odpowiedzi kodowe porównywane są z podanymi wzorcami, z rozróżnianiem wielkości liter i bez tolerancji literówek. Objaśnienia są zwykłym tekstem; Markdown nie jest renderowany.

Adaptacja jest prostą, opisaną heurystyką, a nie zweryfikowanym modelem badawczym. Zegar urządzenia określa terminy powtórek. Wyczyszczenie danych lub odinstalowanie aplikacji usuwa lokalny postęp. Niezatwierdzona odpowiedź może zniknąć po wymuszonym zatrzymaniu procesu, jeśli użytkownik wcześniej nie wybrał „Zapisz i wróć”.

## Weryfikacja pierwszej wersji

Kompilacja debug APK, 31 testów jednostkowych i jeden test całej lekcji przez interfejs Compose/Robolectric. Test interfejsu przechodzi przez osiem wariantów ćwiczeń, błąd, ponowną próbę, literówkę oraz podsumowanie i sprawdza zapis ostatniej odpowiedzi. Generuje zrzuty do `app/build/screenshots/`. Lint: bez błędów; ostrzeżenia dotyczą między innymi dostępności nowszych wersji zależności. Przykładowe kursy sprawdzono również względem JSON Schema. Nie przeprowadzono testu na fizycznym telefonie ani testu systemowego okna wyboru pliku.
