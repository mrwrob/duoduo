# Format kursu — schemaVersion 1

Kurs to plik UTF-8 JSON, bez komentarzy i bez nieznanych pól. Schemat `courses/course.schema.json` pomaga w edytorze; aplikacja dodatkowo sprawdza zależności między polami, obrazki, unikalność identyfikatorów oraz limity.

## Minimalny przykład

```json
{
  "schemaVersion": 1,
  "id": "english-basic",
  "version": 1,
  "title": "Angielski — podstawy",
  "description": "Pierwsze słowa",
  "subject": "language",
  "lessons": [
    {
      "id": "lesson-1",
      "title": "Woda",
      "exercises": [
        {
          "id": "water",
          "type": "word_input",
          "prompt": "Wpisz po angielsku: woda",
          "answers": [["water"]],
          "answerPolicy": {
            "allowTypos": true,
            "caseSensitive": false,
            "trim": true
          }
        }
      ]
    }
  ]
}
```

`id` kursu, lekcji, zadania i opcji: od 1 do 80 znaków, tylko litery ASCII, cyfry, `_` i `-`. Identyfikatory zadań są unikalne w całym kursie, opcji w obrębie zadania. `version` to dodatnia liczba całkowita. `subject` to `language` albo `programming`. Identyczne słowa w różnych kafelkach muszą mieć różne identyfikatory.

Kurs zawiera 1–100 niepustych lekcji i najwyżej 1000 zadań. Sesja wybiera maksymalnie osiem zadań z wybranej lekcji; kolejne uruchomienia preferują ćwiczenia niepoznane i wymagające powtórki.

## Pola zadania

Wspólne: `id`, `type`, `prompt`, opcjonalne `explanation` (widoczne dopiero po sprawdzeniu odpowiedzi). `template` to tekst lub kod do pokazania przed odpowiedzią; znaczniki `{{1}}`, `{{2}}` reprezentują kolejne luki. Nowe linie kodu zapisuj jako `\n`.

| type | Znaczenie | Wymagane pola specyficzne |
|---|---|---|
| `choice` | Jedna lub wiele odpowiedzi ABC | `options`, `correctOptionIds`; dla wielu: `multiple: true` |
| `word_input` | Wpisanie całego słowa | `answers` z jedną listą wariantów |
| `sentence_input` | Ręczne uzupełnianie luk | `template`, `answers` z listą wariantów dla każdej luki |
| `sentence_bank` | Uzupełnianie luk kafelkami | `template`, `options`, `correctOptionIds` w kolejności luk |
| `sentence_select` | Zaznaczanie słów w gotowym zdaniu | `options` w kolejności zdania, `correctOptionIds` zaznaczanych słów |
| `sentence_order` | Układanie zdania z kafelków | `options`, `correctOptionIds` w poprawnej kolejności |
| `image_input` | Wpisanie słowa do obrazka | `imageBase64`, `imageAlt`, `answers` z jedną listą wariantów |
| `image_choice` | Wybór odpowiedzi do obrazka | `imageBase64`, `imageAlt`, `options`, `correctOptionIds` |

Opcje mają postać `{"id": "a", "text": "apple"}`. Potrzebne są przynajmniej dwie opcje, najwyżej 40. `correctOptionIds` zawiera wyłącznie istniejące i niepowtarzające się identyfikatory. Przy układaniu oraz banku słów kolejność ma znaczenie. Przy ABC i zaznaczaniu porównywany jest dokładny zbiór; nie ma częściowych punktów. `image_choice` także obsługuje `multiple: true`.

`answers` to lista luk, a każda luka zawiera akceptowane odpowiedzi. Przykład dwóch luk:

```json
{
  "id": "sentence-1",
  "type": "sentence_input",
  "prompt": "Uzupełnij zdanie.",
  "template": "I {{1}} the {{2}}.",
  "answers": [["like", "love"], ["colour", "color"]]
}
```

W `sentence_input` i `sentence_bank` wszystkie znaczniki muszą wystąpić kolejno, każdy dokładnie raz, zgodnie z liczbą luk (maksymalnie 12 dla wpisywania). Listy wariantów oceniane są niezależnie. Jeśli poprawność jednej luki zależy od drugiej, rozdziel zadanie lub użyj wyboru gotowych odpowiedzi. Bank słów i układanie obsługują jeden wzorcowy ciąg identyfikatorów. Dodanie dystraktorów jest dozwolone.

## Literówki i kod

Domyślne `answerPolicy`: `allowTypos: true`, `caseSensitive: false`, `trim: true`. Normalizacja Unicode NFC jest zawsze włączona. `trim` usuwa wyłącznie białe znaki na początku i końcu odpowiedzi; wnętrze odpowiedzi i interpunkcja pozostają bez zmian.

Literówka oznacza pojedyncze wstawienie, usunięcie, podmianę albo przestawienie sąsiednich znaków. Zarówno wzorzec, jak i wpisana odpowiedź muszą mieć co najmniej cztery znaki po normalizacji. Słowa krótsze oceniane są dokładnie. Tolerancja jest mechaniczna — nie rozpoznaje, czy literówka tworzy inne istniejące słowo; w zadaniach wymagających dokładności ustaw `allowTypos: false`.

Odpowiedź z literówką jest zaliczana, pokazuje korektę i wraca do powtórki następnego dnia. Wszystkie luki muszą być poprawne lub zaakceptowane jako literówka, aby zadanie zostało zaliczone.

W kursie `programming` wszystkie zadania tekstowe muszą jawnie ustawiać `allowTypos: false` oraz `caseSensitive: true`. Dozwolone odpowiedzi kodowe można rozszerzyć przez warianty w `answers`. Gdy wcięcia mają znaczenie, ustaw również `trim: false`. Aplikacja nie wykonuje kodu.

## Obrazki

`imageBase64` zawiera wyłącznie zakodowane bajty PNG/JPEG, bez prefiksu `data:image/...`. Maksymalnie 2 MB po dekodowaniu, każdy wymiar 1–2048 px. `imageAlt` jest wymaganym opisem dla czytnika ekranu; jego treść powinien przemyśleć autor kursu, ponieważ może ujawniać badaną odpowiedź.

Przykład przygotowania Base64 w Pythonie:

```python
import base64
from pathlib import Path
encoded = base64.b64encode(Path("picture.png").read_bytes()).decode("ascii")
```

Obrazki osadzone w JSON zwiększają rozmiar pliku, ale upraszczają przenoszenie i zapewniają naukę bez internetu. Limit całego pliku: 8 MB. Obrazy są prezentowane pojedynczo, nad polem odpowiedzi lub opcjami; nie ma klikalnych obszarów ani wielu podpisów na jednym obrazku.

## Zmiany kursu

Zachowuj stabilne `id`. Przy zmianie treści zwiększ `version`. Nowa wersja zastępuje kurs lokalnie, kasuje jego dotychczasowy postęp i zapisaną sesję tego kursu. Pozostałe kursy nie są zmieniane. Aplikacja informuje o tym przed importem. Ten sam numer wersji jest akceptowany ponownie wyłącznie dla identycznej zdekodowanej zawartości.
