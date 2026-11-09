"""Build the hand-curated Italian course from courses/italian.md.

Only the import artifact is written. Source notes and built-in app courses stay intact.
Run: python3 scripts/build_italian_course.py
"""
from __future__ import annotations

import hashlib
import json
import random
import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "courses/italian.md"
OUTPUT = ROOT / "courses/italian.json"

# The order is pedagogical: everyday language, animals, descriptions, idioms.
# Each line is an Italian expression and its Polish meaning in the source context.
VOCABULARY = [
    ("powroty", "Powroty, lato i czas", """
luglio | lipiec
due giorni fa | dwa dni temu
d'estate | latem
l'estate | lato
durante il fine settimana | podczas weekendu
a due passi | dwa kroki stąd; bardzo blisko
"""),
    ("codziennosc", "Codzienność i zmiany", """
duro | twardy
curato | zadbany
molto azzeccato | bardzo trafiony, np. prezent
il posto | miejsce
la rotonda | rondo
troppo | za dużo
nessun cambiamento | żadna zmiana
un sacco di cambiamenti | mnóstwo zmian
nonostante tutti i cambiamenti | pomimo wszystkich zmian
"""),
    ("zdrowie", "Zdrowie i samopoczucie", """
la malattia | choroba
la varicella | ospa wietrzna
la sinusite | zapalenie zatok
la voce rauca | zachrypnięty głos
avere la voce rauca | mieć chrypę
problematico | problematyczny
meglio | lepiej
"""),
    ("przedmioty", "Przedmioty na co dzień", """
il lettino | leżak płaski, przypominający łóżko
la sedia a sdraio | leżak w kształcie krzesła
gli stivali di gomma | kalosze
la gomma da masticare | guma do żucia
l'impermeabile | płaszcz przeciwdeszczowy
la scritta | napis
la scritta sulla maglietta | napis na koszulce
la frase | zdanie
"""),
    ("podroze", "Podróże i widoki", """
il visto | wiza
la vista | widok lub wzrok
la vista dalla finestra | widok z okna
la vista sul mare | widok na morze
con vista mare | z widokiem na morze
il Giappone | Japonia
giapponese | japoński
occidentale | zachodni
un veicolo fuoristrada | samochód terenowy
l'allerta terremoto | ostrzeżenie przed trzęsieniem ziemi
"""),
    ("ludzie", "Ludzie i przydatne wyrażenia", """
le persone | ludzie, osoby — rzeczownik w liczbie mnogiej
gli uomini | mężczyźni; także ludzie
la gente | ludzie — rzeczownik w liczbie pojedynczej
il compleanno del figlio dei miei amici | urodziny syna moich przyjaciół
avere bisogno di | potrzebować
a malincuore | z ciężkim sercem
rendere la vita più piacevole | czynić życie przyjemniejszym
il soprannome | przezwisko
"""),
    ("domowe", "Zwierzęta domowe", """
il coniglio | królik
il criceto | chomik
il pappagallo | papuga
il pesce rosso | złota rybka
il porcellino d'India | świnka morska
"""),
    ("gospodarskie", "Zwierzęta gospodarskie", """
l'asino | osioł
il cavallo | koń
il pony | kucyk
il gallo | kogut
la gallina | kura
il maiale | świnia
la pecora | owca
il tacchino | indyk
il toro | byk
la mucca | krowa
"""),
    ("dzikie-a", "Dzikie zwierzęta — od kameleona do żyrafy", """
il camaleonte | kameleon
il canguro | kangur
il capibara | kapibara
il cervo | jeleń
il cinghiale | dzik
il coccodrillo | krokodyl
il ghepardo | gepard
la giraffa | żyrafa
"""),
    ("dzikie-b", "Dzikie zwierzęta — od hipopotama do nosorożca", """
l'ippopotamo | hipopotam
il koala | koala
il leone | lew
il leopardo | lampart
il lupo | wilk
il panda | panda
il riccio | jeż
il rinoceronte | nosorożec
"""),
    ("dzikie-c", "Dzikie zwierzęta — od wiewiórki do słonia", """
lo scoiattolo | wiewiórka
la tigre | tygrys
la volpe | lis
la zebra | zebra
l'orso | niedźwiedź
la talpa | kret
la scimmia | małpa
il cammello | wielbłąd
l'elefante | słoń
la renna | renifer
il bradipo | leniwiec
la faina | kuna domowa
"""),
    ("wodne", "Zwierzęta wodne", """
l'anguilla | węgorz
la balena | wieloryb
il delfino | delfin
il granchio | krab
la foca | foka
il salmone | łosoś
la sardina | sardynka
lo squalo | rekin
il tonno | tuńczyk
la trota | pstrąg
il polpo | ośmiornica
la balenottera azzurra | płetwal błękitny
"""),
    ("ptaki-a", "Ptaki — od orła do mewy", """
l'aquila | orzeł
la cicogna | bocian
il cigno | łabędź
il corvo | kruk
il falco | sokół
il fenicottero | flaming
il gabbiano | mewa
"""),
    ("ptaki-b", "Ptaki — od sowy do kaczki", """
il gufo | sowa
il passero | wróbel
il pavone | paw
il pinguino | pingwin
il piccione | gołąb
lo struzzo | struś
l'anatra | kaczka
l'uccello | ptak
"""),
    ("owady", "Owady", """
lo scarafaggio | karaluch
l'ape | pszczoła
la farfalla | motyl
la formica | mrówka
la vespa | osa
la zanzara | komar
la coccinella | biedronka
la mosca | mucha
"""),
    ("inne-zwierzeta", "Inne zwierzęta", """
il serpente | wąż
il topo | mysz
la lucertola | jaszczurka
il pipistrello | nietoperz
il ragno | pająk
il ratto | szczur
il rospo | ropucha
la tartaruga | żółw
la rana | żaba
"""),
    ("mlode", "Młode zwierząt", """
il cucciolo | szczeniaczek
un piccolo di giraffa | młode żyrafy
la cucciolata | miot
il micio | kotek — pieszczotliwie, niekoniecznie młody
il nido | gniazdo
schiudersi | wykluwać się z jajka
"""),
    ("zachowania", "Zachowania i opieka nad zwierzętami", """
addomesticare | oswoić
addestrare | wytresować
cacciare | polować
cavalcare | jeździć konno
impollinare i fiori | zapylać kwiaty
rotolarsi nel fango | tarzać się w błocie
rinfrescarsi | ochłodzić się
accoppiarsi | kopulować
"""),
    ("cialo", "Ciało i zmysły", """
la mente | umysł
il mento | broda — część twarzy
la palpebra | powieka
le sopracciglia | brwi
la retina | siatkówka oka
la pupilla | źrenica
l'olfatto | węch
l'esofago | przełyk
il volto umano | ludzka twarz
il subconscio | podświadomość
le impronte digitali | odciski palców
"""),
    ("anatomia", "Anatomia zwierząt", """
i tentacoli | macki; w notatkach także czułki ślimaka
le antenne | czułki, np. motyla
la gobba | garb
le chele | szczypce, np. kraba
la proboscide | trąba
la cresta | grzebień koguta; także irokez
il pelo | sierść
il sangue | krew
la gravidanza | ciąża
l'elefantessa | słonica
"""),
    ("ruch", "Ruch, postrzeganie i czynności", """
percepire | odczuwać, odbierać bodźce
annusare | wąchać
sollevare | podnieść
proteggere da | chronić przed
muovendo il collo | ruszając szyją
lo strofinamento | ocieranie się
girare la testa | obracać głową
dilatarsi | rozszerzać się
afferrare oggetti | chwytać przedmioty
pompare il sangue | pompować krew
"""),
    ("przyroda", "Przyroda i otoczenie", """
la sabbia | piasek
la tempesta | burza
l'alluvione | powódź
la corteccia | kora drzewa
la radice | korzeń
nei cespugli | w krzakach
la fessura | szczelina
il buco | dziura
la preda | ofiara, zdobycz drapieżnika
l'essere umano | istota ludzka
"""),
    ("cechy", "Cechy, opisy i porównania", """
vasto | szeroki, rozległy
variegato | różnorodny
erbivoro | roślinożerny
carnivoro | mięsożerny
indigeno | rdzenny
testardo | uparty
più facile | łatwiejszy; łatwiej
a seconda di | w zależności od
il paragone | porównanie
"""),
    ("nauka", "Zajęcia, przedmioty i miary", """
la ristrutturazione | remont
alle 6 di mattina | o szóstej rano
prendere gli appunti | robić notatki
il laboratorio | warsztaty; laboratorium
il telaio | krosno
il liquido | płyn
il diametro | średnica
la tonnellata | tona
lo specchio | lustro
il luogo | miejsce
"""),
    ("czasowniki", "Czynności i dźwięki", """
disattivare | wyłączyć, dezaktywować
miagolare | miauczeć
il sibilio | syk
rimproverare | zganić, udzielić reprymendy
nascondersi da | ukryć się przed
nascondere | ukryć coś
mettere | umieścić, położyć
abbaiare | szczekać
mordere | gryźć
"""),
    ("wyrazenia", "Przydatne wyrażenia i kultura", """
a portata di mano | pod ręką
non avere sale in zucca | nie mieć oleju w głowie
quattro di numero | dokładnie cztery
il vizio | wada, przywara, zły nawyk
la danza | taniec
il programma | program
il documentario | film dokumentalny
"""),
]


def entries(block):
    return [tuple(part.strip() for part in line.split("|", 1)) for line in block.strip().splitlines()]


def normalized(text):
    text = text.replace("’", "'").replace("‘", "'")
    text = re.sub(r"\\([!_\-=.])", r"\1", text)
    return text.replace("**", "").lower()


def forms(text):
    # Accept both keyboard apostrophes without reporting punctuation as a typo.
    return list(dict.fromkeys([text, text.replace("'", "’")]))


def rng(key):
    return random.Random(int(hashlib.sha256(key.encode()).hexdigest()[:16], 16))


def options(words):
    return [{"id": f"o{i + 1}", "text": value} for i, value in enumerate(words)]


LESSONS = []
SOURCE_PLAIN = normalized(SOURCE.read_text())


def add_lessons(key, title, exercises):
    chunks = [exercises[i:i + 8] for i in range(0, len(exercises), 8)]
    for i, chunk in enumerate(chunks, 1):
        LESSONS.append({"id": f"{key}-{i}", "title": title + (f" · {i}/{len(chunks)}" if len(chunks) > 1 else ""), "exercises": chunk})


def input_task(key, prompt, template, answers, explanation, *, strict=True, kind="sentence_input"):
    return {"id": key, "type": kind, "prompt": prompt, "template": template,
            "answers": [forms(a) if isinstance(a, str) else list(dict.fromkeys(v for item in a for v in forms(item))) for a in answers],
            "answerPolicy": {"allowTypos": not strict, "caseSensitive": False, "trim": True}, "explanation": explanation}


def choice_task(key, prompt, correct, distractors, explanation):
    words = list(correct) + list(distractors)
    assert len(set(words)) == len(words), key
    rng(key).shuffle(words)
    return {"id": key, "type": "choice", "prompt": prompt, "options": options(words),
            "correctOptionIds": [f"o{i + 1}" for i, word in enumerate(words) if word in correct],
            "multiple": len(correct) > 1, "explanation": explanation}


def bank_task(key, prompt, template, correct, distractors, explanation):
    # Identities matter when the same word occurs twice (e.g. né … né).
    pool = options(list(correct) + list(distractors))
    right = [item["id"] for item in pool[:len(correct)]]
    rng(key).shuffle(pool)
    return {"id": key, "type": "sentence_bank", "prompt": prompt, "template": template,
            "options": pool, "correctOptionIds": right, "explanation": explanation}


def order_task(key, prompt, tokens, explanation):
    pool = options(tokens)
    right = [item["id"] for item in pool]
    rng(key).shuffle(pool)
    return {"id": key, "type": "sentence_order", "prompt": prompt, "options": pool,
            "correctOptionIds": right, "explanation": explanation}


def select_task(key, prompt, words, positions, explanation):
    return {"id": key, "type": "sentence_select", "prompt": prompt, "options": options(words),
            "correctOptionIds": [f"o{i + 1}" for i in positions], "explanation": explanation}


def distance_one(left, right):
    if abs(len(left) - len(right)) > 1:
        return False
    if len(left) == len(right):
        positions = [i for i in range(len(left)) if left[i] != right[i]]
        return len(positions) == 1 or (len(positions) == 2 and positions[1] == positions[0] + 1
                                     and left[positions[0]] == right[positions[1]] and left[positions[1]] == right[positions[0]])
    short, long = sorted([left, right], key=len)
    return any(long[:i] + long[i + 1:] == short for i in range(len(long)))


def production_parts(italian):
    # Put the article in the template, so a word exercise really asks for one word.
    article = re.match(r"^(?:il |lo |la |i |gli |le |un |una |uno |l')", italian)
    prefix = article.group() if article else ""
    remaining = italian[len(prefix):]
    if " " not in remaining:
        return prefix + "{{1}}", remaining, "word_input"
    # For expressions, the fixed context makes the requested answer unambiguous.
    head, last = italian.rsplit(" ", 1)
    return head + " {{1}}", last, "sentence_input"


ALL = [pair for _, _, block in VOCABULARY for pair in entries(block)]
LEXEMES = set(production_parts(it)[1].lower() for it, _ in ALL)
STRICT = {left for left in LEXEMES if any(distance_one(left, right) for right in LEXEMES if left != right)}
STRICT.update({"mente", "mento", "visto", "vista", "posto", "posti", "gente", "persone"})

for group, title, block in VOCABULARY:
    pairs = entries(block)
    tasks = []
    for index, (italian, polish) in enumerate(pairs, 1):
        # A genuine source phrase, except the explicitly expanded slash abbreviation.
        assert normalized(italian) in SOURCE_PLAIN or italian == "un piccolo di giraffa", italian
        key = f"v-{group}-{index:02d}"
        explanation = f"{italian} — {polish}."
        distractors = [pl for it, pl in pairs if pl != polish]
        rng(key).shuffle(distractors)
        tasks.append(choice_task(key + "-znaczenie", f"Co znaczy „{italian}”?", [polish], distractors[:3], explanation))
        template, answer, kind = production_parts(italian)
        prompt = f"Uzupełnij włoskie wyrażenie: „{polish}”." if kind == "sentence_input" else f"Wpisz brakujące słowo: „{polish}”."
        tasks.append(input_task(key + "-wpisz", prompt, template, [answer], explanation,
                                strict=answer.lower() in STRICT, kind=kind))
    add_lessons("v-" + group, title, tasks)


# Grammar and sentence exercises are curated separately from vocabulary cards.
add_lessons("zdania-powroty", "Zdania — powroty i wspomnienia", [
    input_task("s-manca", "Uzupełnij: „Brakuje mi kursu”.", "Mi {{1}} il corso.", ["manca"], "Mi manca il corso. — Brakuje mi kursu; tęsknię za kursem."),
    input_task("s-mancate", "Uzupełnij: „Tęsknię za wami”.", "Mi {{1}}.", ["mancate"], "Mi mancate. — Tęsknię za wami."),
    order_task("s-era-dura", "Ułóż: „Wiem, że było ciężko”.", ["So", "che", "era", "dura."], "So che era dura. — Wiem, że było ciężko."),
    input_task("s-tornata", "Uzupełnij: „Wróciłam”. Mówi kobieta.", "{{1}} tornata.", ["Sono"], "Sono tornata. — Wróciłam."),
    bank_task("s-milano", "Uzupełnij: „Wróciłam z Mediolanu”.", "Sono {{1}} {{2}} Milano.", ["tornata", "da"], ["tornato", "con", "a"], "Sono tornata da Milano. Przyimek da wskazuje tutaj, skąd wróciłam."),
    choice_task("s-estate-rodzaj", "Jaki rodzaj ma włoski rzeczownik „estate” (lato)?", ["żeński"], ["męski", "nijaki"], "Estate jest rodzaju żeńskiego: la mia estate; l'estate è finita."),
    input_task("s-estate-finita", "Uzupełnij: „Lato się skończyło”.", "L'estate è {{1}}.", ["finita"], "L'estate è finita. Forma finita zgadza się z żeńskim rzeczownikiem estate."),
    input_task("s-bambina", "Uzupełnij: „Kiedy byłam mała…”. Mówi kobieta.", "Quando ero {{1}}…", [["bambina", "piccola"]], "W notatkach są dwa warianty: Quando ero bambina… / Quando ero piccola…"),
])

add_lessons("zdania-roznice", "Zdania — da/con, gente/persone", [
    input_task("s-da-danzica", "Uzupełnij przyimek: „Wyruszam z Gdańska”.", "Parto {{1}} Danzica.", ["da"], "Parto da Danzica. Przyimek da wskazuje punkt wyjazdu."),
    input_task("s-con-marito", "Uzupełnij przyimek: „Jadę z moim mężem”.", "Vado {{1}} mio marito.", ["con"], "Vado con mio marito. Przyimek con oznacza tutaj towarzystwo."),
    bank_task("s-da-con", "Uzupełnij: „Wyruszam z Gdańska. Jadę z moim mężem”.", "Parto {{1}} Danzica. Vado {{2}} mio marito.", ["da", "con"], ["di", "per"], "Z miejsca: da Danzica. Z kimś: con mio marito."),
    input_task("s-molta-gente", "Uzupełnij odpowiednik „dużo”: „Nie było dużo ludzi”.", "Non c'era {{1}} gente.", ["molta"], "Gente to rzeczownik żeński w liczbie pojedynczej: c'era molta gente."),
    input_task("s-molte-persone", "Uzupełnij odpowiednik „dużo”: „Nie było dużo osób”.", "Non c'erano {{1}} persone.", ["molte"], "Persone to liczba mnoga: c'erano molte persone."),
    choice_task("s-gente-persone", "Zaznacz oba poprawne zdania z notatek.", ["Non c'era molta gente.", "Non c'erano molte persone."], ["Non c'erano molte gente.", "Non c'era molta persone."], "Liczba pojedyncza: gente → c'era, molta. Liczba mnoga: persone → c'erano, molte."),
    select_task("s-przyimki", "Zaznacz przyimki oznaczające „z Gdańska” i „z mężem”.", ["Parto", "da", "Danzica.", "Vado", "con", "mio", "marito."], [1, 4], "Da Danzica — z Gdańska; con mio marito — z moim mężem."),
    order_task("s-gente-zdanie", "Ułóż: „Nie było dużo ludzi”, używając słowa gente.", ["Non", "c'era", "molta", "gente."], "Non c'era molta gente. Gente łączy się z liczbą pojedynczą."),
])

add_lessons("zdania-komunikacja", "Zdania — pytania i reakcje", [
    input_task("s-quale", "Uzupełnij: „Nie wiem, które miejsce…”.", "Non so {{1}} posto…", ["quale"], "Quale posto — które miejsce; liczba pojedyncza."),
    input_task("s-quali", "Uzupełnij: „Nie wiem, które miejsca…”.", "Non so {{1}} posti…", ["quali"], "Quali posti — które miejsca; liczba mnoga."),
    input_task("s-perche", "Uzupełnij „ponieważ” w zdaniu z notatek.", "Sono felice {{1}} ho fatto il compito.", ["perché"], "Sono felice perché ho fatto il compito. — Cieszę się, ponieważ zrobiłam pracę domową. Piszemy perché z akcentem é."),
    order_task("s-piaccio", "Ułóż: „Nie podobam ci się”.", ["Non", "ti", "piaccio."], "Non ti piaccio. Osoba mówiąca nie podoba się rozmówcy."),
    input_task("s-perso-voce", "Uzupełnij: „Straciłam/straciłem głos”.", "Ho {{1}} la voce.", ["perso"], "Ho perso la voce. — Straciłam/straciłem głos."),
    input_task("s-temporaneo", "Uzupełnij: „To tymczasowe”.", "È {{1}}.", ["temporaneo"], "È temporaneo. — To tymczasowe.", strict=False),
    choice_task("s-eccolo", "Co znaczy „Eccolo qui!”?", ["Oto on!"], ["Tęsknię za wami!", "Straciłam głos!", "Będzie lepiej!"], "Eccolo (qui)! — Oto on!"),
    order_task("s-sapevi", "Ułóż zwrot z notatek: „Czy wiesz, że…”.", ["Lo", "sapevi", "che…"], "Lo sapevi che… — dosłownie „Czy wiedziałeś/wiedziałaś, że…”, w notatkach: „Czy wiesz, że…”."),
])

add_lessons("zdania-utrwalanie", "Zdania — utrwalanie konstrukcji", [
    bank_task("s-spero", "Uzupełnij zdanie z notatek: „Mam nadzieję, że będzie lepiej”.", "Spero che {{1}} {{2}}.", ["andrà", "meglio"], ["andava", "peggio"], "Spero che andrà meglio. — Mam nadzieję, że będzie lepiej."),
    order_task("s-neve", "Ułóż: „Myślę, że śnieg się roztopi”.", ["Penso", "che", "la", "neve", "si", "scioglierà."], "Penso che la neve si scioglierà."),
    input_task("s-ne-ne", "Uzupełnij oba słowa odpowiadające polskiemu „ani … ani …”.", "{{1}} questo {{2}} quello", ["né", "né"], "Né … né … — ani … ani … . Oba né zapisujemy z akcentem. Przykład: né questo né quello — ani to, ani tamto."),
    order_task("s-volendo", "Ułóż zdanie z notatek: „Na upartego można zjeść nawet świnkę morską”.", ["Volendo", "si", "può", "mangiare", "anche", "il", "porcellino", "d'India."], "Volendo si può mangiare anche il porcellino d'India. To ćwiczenie językowe oparte na zdaniu z notatek."),
    input_task("s-bisogno", "Uzupełnij wyrażenie „potrzebować”.", "avere {{1}} di", ["bisogno"], "Avere bisogno di — potrzebować.", strict=False),
    input_task("s-malincuore", "Uzupełnij wyrażenie „z ciężkim sercem”.", "a {{1}}", ["malincuore"], "A malincuore — z ciężkim sercem, niechętnie.", strict=False),
    bank_task("s-portata", "Uzupełnij wyrażenie „pod ręką”.", "a {{1}} di {{2}}", ["portata", "mano"], ["testa", "posto"], "A portata di mano — pod ręką."),
    select_task("s-ludzie-w-zdaniu", "Zaznacz dwa rzeczowniki oznaczające ludzi lub osoby.", ["Non", "c'era", "molta", "gente.", "Non", "c'erano", "molte", "persone."], [3, 7], "Gente — liczba pojedyncza; persone — liczba mnoga."),
])

COMPARISONS = [
    ("sano", "un", "pesce", "zdrowy jak ryba"),
    ("lento", "una", "lumaca", "powolny jak ślimak"),
    ("stupido", "una", "gallina", "głupi jak kura"),
    ("testardo", "un", "mulo", "uparty jak muł"),
    ("furbo", "una", "volpe", "sprytny jak lis"),
    ("fedele", "un", "cane", "wierny jak pies"),
    ("curioso", "una", "scimmia", "ciekawski jak małpa"),
]
comparison_tasks = []
for i, (adjective, article, animal, meaning) in enumerate(COMPARISONS, 1):
    phrase = f"{adjective} come {article} {animal}"
    explanation = f"{phrase} — {meaning}."
    comparison_tasks.append(input_task(f"porownanie-{i}-wpisz", f"Uzupełnij porównanie: „{meaning}”.", f"{adjective} come {article} {{{{1}}}}", [animal], explanation, strict=animal in STRICT, kind="word_input"))
    comparison_tasks.append(order_task(f"porownanie-{i}-uloz", f"Ułóż porównanie: „{meaning}”.", [adjective, "come", article, animal], explanation))
comparison_tasks += [
    choice_task("porownanie-upor", "Które porównanie oznacza „uparty jak muł”?", ["testardo come un mulo"], ["furbo come una volpe", "fedele come un cane", "sano come un pesce"], "Testardo — uparty; un mulo — muł."),
    select_task("porownanie-zwierzeta", "Zaznacz nazwy zwierząt w obu porównaniach.", ["sano", "come", "un", "pesce;", "lento", "come", "una", "lumaca"], [3, 7], "Pesce — ryba; lumaca — ślimak."),
]
add_lessons("porownania", "Porównania do zwierząt", comparison_tasks)

IDIOMS = [
    ("abbassare la cresta", "spuścić z tonu, przestać się wywyższać"),
    ("avere la pelle d'oca", "mieć gęsią skórkę"),
    ("avere la tartaruga", "mieć kaloryfer na brzuchu"),
    ("essere un coniglio", "być tchórzem"),
    ("essere quattro gatti", "być garstką ludzi"),
    ("fare la gatta morta", "udawać niewiniątko, ukrywając swoje zamiary"),
]
idiom_tasks = []
for i, (phrase, meaning) in enumerate(IDIOMS, 1):
    wrong = [m for p, m in IDIOMS if p != phrase][:3]
    explanation = f"{phrase} — {meaning}. To znaczenie przenośne, nie dosłowne."
    idiom_tasks.append(choice_task(f"idiom-{i}-znaczenie", f"Co znaczy idiom „{phrase}”?", [meaning], wrong, explanation))
    idiom_tasks.append(order_task(f"idiom-{i}-uloz", f"Ułóż idiom: „{meaning}”.", phrase.split(), explanation))
idiom_tasks += [
    input_task("idiom-sale", "Uzupełnij idiom „nie mieć oleju w głowie”.", "non avere {{1}} in zucca", ["sale"], "Non avere sale in zucca — dosłownie: nie mieć soli w dyni; przenośnie: nie mieć oleju w głowie."),
    choice_task("idiom-quattro", "Co podkreśla wyrażenie „quattro di numero”?", ["Dokładnie cztery, a nie około czterech."], ["Bardzo dużą liczbę ludzi.", "Cztery różne gatunki zwierząt.", "Czwarte miejsce w kolejce."], "Quattro di numero — dokładnie cztery."),
    input_task("idiom-abbassa", "Uzupełnij zwrot „Spuść z tonu!” z notatek.", "Abbassa la {{1}}!", ["cresta"], "Abbassa la cresta! — Spuść z tonu! Dosłownie: opuść grzebień.", strict=False),
    select_task("idiom-czesci-ciala", "Zaznacz rzeczowniki oznaczające części ciała: grzebień koguta i skórę.", ["abbassare", "la", "cresta;", "avere", "la", "pelle", "d'oca"], [2, 5], "La cresta — grzebień koguta; la pelle — skóra."),
]
add_lessons("idiomy", "Idiomy — zwierzęta w języku", idiom_tasks)

# Transcribed directly from embedded image1. No external zoological answers added.
add_lessons("przyslowia", "Przysłowia z notatek", [
    input_task("p-lupo", "Uzupełnij: „Wilk gubi sierść, ale nie złą naturę”.", "Il lupo perde il {{1}} ma non il {{2}}.", ["pelo", "vizio"], "Il lupo perde il pelo ma non il vizio. Człowiek może zmienić wygląd, ale wraca do dawnych złych nawyków."),
    choice_task("p-lupo-sens", "Jaki jest sens „Il lupo perde il pelo ma non il vizio”?", ["Zmiana wyglądu nie musi oznaczać zmiany natury i nawyków."], ["Lepiej przyjąć coś pewnego dziś niż czekać na więcej.", "Nie warto wywoływać uśpionego problemu.", "Można osiągnąć dwa cele jednym działaniem."], "Polski odpowiednik z notatek: natury nie oszukasz."),
    input_task("p-cane", "Uzupełnij: „Pies, który szczeka, nie gryzie”.", "Can che {{1}} non {{2}}.", ["abbaia", "morde"], "Can che abbaia non morde. W przysłowiu występuje skrócona forma can zamiast cane."),
    order_task("p-cane-uloz", "Ułóż przysłowie: „Pies, który szczeka, nie gryzie”.", ["Can", "che", "abbaia", "non", "morde."], "Can che abbaia non morde."),
    order_task("p-dorme", "Ułóż: „Nie budź psa, który śpi”.", ["Non", "svegliare", "il", "can", "che", "dorme."], "Non svegliare il can che dorme. Odpowiednik z notatek: nie wywołuj wilka z lasu."),
    input_task("p-uovo", "Uzupełnij: „Lepsze jajko dzisiaj niż kura jutro”.", "Meglio un {{1}} oggi che una {{2}} domani.", ["uovo", "gallina"], "Meglio un uovo oggi che una gallina domani. Odpowiednik: lepszy wróbel w garści niż gołąb na dachu."),
    bank_task("p-piccioni", "Uzupełnij idiom odpowiadający „upiec dwie pieczenie na jednym ogniu”.", "Prendere {{1}} piccioni con {{2}} fava.", ["due", "una"], ["tre", "un"], "Prendere due piccioni con una fava — osiągnąć dwa cele jednym działaniem."),
    input_task("p-vizio", "Uzupełnij zdanie: „Palenie to zły nawyk”.", "Fumare è un {{1}}.", ["vizio"], "Fumare è un vizio. Il vizio — wada, przywara, zły nawyk.", strict=False),
])

add_lessons("kontrasty", "Uwaga na podobne słowa", [
    input_task("k-mente", "Wpisz słowo oznaczające „umysł”, a nie „brodę”.", "la {{1}}", ["mente"], "La mente — umysł; il mento — broda. Tutaj różnica jednej litery zmienia znaczenie.", kind="word_input"),
    input_task("k-mento", "Wpisz nazwę części twarzy: „broda”.", "il {{1}}", ["mento"], "Il mento — broda (część twarzy); la mente — umysł.", kind="word_input"),
    input_task("k-visto", "Wpisz słowo oznaczające „wizę”, dokument podróżny.", "il {{1}}", ["visto"], "Il visto — wiza; la vista — widok lub wzrok.", kind="word_input"),
    input_task("k-vista", "Wpisz słowo oznaczające „widok” lub „wzrok”.", "la {{1}}", ["vista"], "La vista — widok lub wzrok; il visto — wiza.", kind="word_input"),
    choice_task("k-miejsca", "Które dwa rzeczowniki w notatkach oznaczają „miejsce”?", ["il posto", "il luogo"], ["il mento", "il visto"], "Zarówno il posto, jak i il luogo mogą oznaczać miejsce."),
    choice_task("k-owady", "Zaznacz wszystkie nazwy owadów.", ["l'ape", "la zanzara", "la coccinella"], ["il ragno", "il pipistrello"], "Pszczoła, komar i biedronka są w grupie Insetti. Pająk i nietoperz są w notatkach w grupie Altri."),
    select_task("k-nazwy-zwierzat", "Zaznacz nazwy zwierząt; pomiń nazwy części ciała.", ["il", "gufo;", "la", "pupilla;", "la", "renna;", "la", "retina"], [1, 5], "Il gufo — sowa; la renna — renifer. La pupilla — źrenica; la retina — siatkówka."),
    choice_task("k-szkolenie", "Które słowo znaczy „wytresować”, w odróżnieniu od „oswoić”?", ["addestrare"], ["addomesticare", "cacciare", "cavalcare"], "Addestrare — wytresować; addomesticare — oswoić; cacciare — polować; cavalcare — jeździć konno."),
])

# Image2 contains questions, not answers. Practice their wording, not animal trivia.
add_lessons("pytania-zwierzeta", "Jak pytać o zwierzęta?", [
    input_task("q-quanti", "Uzupełnij pytanie „Ile nosów ma ślimak?”.", "{{1}} nasi ha la lumaca?", ["Quanti"], "Quanti nasi ha la lumaca? To ćwiczenie słowa pytającego, nie wiedzy o anatomii."),
    choice_task("q-distanza", "Co znaczy „Da quale distanza…”?", ["Z jakiej odległości…?"], ["O której godzinie…?", "W jakim kolorze…?", "Z jakiego powodu…?"], "Na planszy: Da quale distanza riesce a sentire il delfino? — Z jakiej odległości delfin potrafi słyszeć?"),
    input_task("q-come", "Uzupełnij „Jak komunikują się żyrafy?”.", "{{1}} comunicano le giraffe?", ["Come"], "Come comunicano le giraffe? — Jak komunikują się żyrafy?"),
    choice_task("q-piu-veloce", "Co oznacza „Chi corre più veloce…”?", ["Kto biegnie szybciej…?"], ["Kto je więcej…?", "Kto śpi dłużej…?", "Kto skacze wyżej…?"], "Chi corre più veloce: il cavallo o l'orso bruno? — Kto biegnie szybciej: koń czy niedźwiedź brunatny?"),
    bank_task("q-perche", "Uzupełnij początek pytania „Dlaczego wielbłądy mają trzy powieki?”.", "{{1}} i cammelli hanno {{2}} palpebre?", ["Perché", "tre"], ["Come", "due"], "Perché i cammelli hanno 3 palpebre? — pytanie z planszy. Tre to zapis słowny liczby 3."),
    choice_task("q-saltare", "Co znaczy „Qual è l'unico mammifero a non saper saltare?”?", ["Który ssak jako jedyny nie potrafi skakać?"], ["Który ptak jako jedyny nie potrafi latać?", "Który ssak jako jedyny potrafi pływać?", "Które zwierzę biega najszybciej?"], "To tłumaczenie pytania z planszy, bez dopowiadania odpowiedzi zoologicznej."),
    order_task("q-giraffe", "Ułóż pytanie „Jak komunikują się żyrafy?”.", ["Come", "comunicano", "le", "giraffe?"], "Come comunicano le giraffe?"),
    select_task("q-pytajniki", "Zaznacz słowa pytające odpowiadające „ile” i „jak”.", ["Quanti", "nasi", "ha", "la", "lumaca?", "Come", "comunicano", "le", "giraffe?"], [0, 5], "Quanti — ile (tu z rzeczownikiem nasi); come — jak."),
])

add_lessons("deser", "Na deser — variegato i amarena", [
    input_task("d-gelato", "Wpisz brakujące słowo „lody” w wyrażeniu z notatek.", "il {{1}} variegato all'amarena", ["gelato"], "Il gelato variegato all'amarena — lody z wiśniowym dodatkiem, w notatkach: lody z wiśniami.", strict=False, kind="word_input"),
    choice_task("d-variegato", "Jakie ogólne znaczenie ma przymiotnik „variegato” w notatkach?", ["różnorodny"], ["roślinożerny", "uparty", "zachodni"], "Variegato — różnorodny; w nazwie lodów odnosi się do dodatku tworzącego smugi lub przełożenie."),
    input_task("d-amarena", "Uzupełnij nazwę lodów z wiśniowym dodatkiem z notatek.", "il gelato variegato all'{{1}}", ["amarena"], "Il gelato variegato all'amarena.", strict=False),
    order_task("d-uloz", "Ułóż nazwę deseru z notatek: „lody z wiśniami”.", ["il", "gelato", "variegato", "all'amarena"], "Il gelato variegato all'amarena."),
])

COURSE = {
    "schemaVersion": 1, "id": "italian-notes-2026", "version": 1,
    "title": "Włoski — codzienność i świat zwierząt",
    "description": "Kurs na podstawie notatek z 24.09–05.11.2026: słownictwo, zdania, zwierzęta, porównania i idiomy. Polecenia i objaśnienia po polsku.",
    "subject": "language", "lessons": LESSONS,
}


def main():
    tasks = [exercise for lesson in LESSONS for exercise in lesson["exercises"]]
    assert len(LESSONS) <= 100
    assert len(tasks) <= 1000
    assert len({task["id"] for task in tasks}) == len(tasks)
    assert all(1 <= len(lesson["exercises"]) <= 8 for lesson in LESSONS)
    OUTPUT.write_text(json.dumps(COURSE, ensure_ascii=False, indent=2) + "\n")
    print(f"{OUTPUT.name}: {len(LESSONS)} lekcji, {len(tasks)} ćwiczeń, {len(ALL)} haseł słownikowych")
    print(dict(Counter(task["type"] for task in tasks)))


if __name__ == "__main__":
    main()
