"""Generate portable demo courses. Requires Pillow, only for the sample diagrams."""
import base64
import io
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]


def diagram(terminal=False):
    image = Image.new("RGB", (640, 360), "#f1f5ee")
    draw = ImageDraw.Draw(image)
    if terminal:
        draw.rounded_rectangle((60, 45, 580, 315), 20, fill="#20382f")
        draw.line((60, 95, 580, 95), fill="#65756d", width=2)
        for x in (85, 110, 135):
            draw.ellipse((x, 65, x + 10, 75), fill="#d5f47b")
        draw.text((110, 145), "> Hello", fill="#d5f47b", font_size=50)
    else:
        draw.rectangle((100, 90, 540, 270), fill="#d5f47b", outline="#1c5646", width=7)
    buffer = io.BytesIO()
    image.save(buffer, format="PNG")
    return base64.b64encode(buffer.getvalue()).decode()


def options(*words):
    return [{"id": f"o{i+1}", "text": word} for i, word in enumerate(words)]


language = {
    "schemaVersion": 1, "id": "english-demo", "version": 1,
    "title": "Angielski na dobry początek",
    "description": "Kurs demonstracyjny · słowa, zdania i małe odkrycia.",
    "subject": "language",
    "lessons": [{"id": "first-steps", "title": "Pierwsze kroki", "exercises": [
        {"id": "fruit", "type": "choice", "prompt": "Które słowa oznaczają owoce?", "multiple": True,
         "options": options("apple", "table", "banana", "window"), "correctOptionIds": ["o1", "o3"],
         "explanation": "Apple to jabłko, a banana to banan."},
        {"id": "water", "type": "word_input", "prompt": "Uzupełnij zdanie słowem „woda”.",
         "template": "I drink {{1}}.", "answers": [["water"]]},
        {"id": "sentence", "type": "sentence_input", "prompt": "Uzupełnij: „Ona ma czerwony rower”.",
         "template": "She {{1}} a {{2}} bike.", "answers": [["has"], ["red"]]},
        {"id": "bank", "type": "sentence_bank", "prompt": "Uzupełnij: „Lubię czytać książki”.",
         "template": "I {{1}} to read {{2}}.", "options": options("like", "books", "water", "runs"), "correctOptionIds": ["o1", "o2"]},
        {"id": "select", "type": "sentence_select", "prompt": "Zaznacz nazwy zwierząt w zdaniu.",
         "options": options("The", "cat", "and", "the", "dog", "sleep."), "correctOptionIds": ["o2", "o5"]},
        {"id": "order", "type": "sentence_order", "prompt": "Ułóż zdanie: „Lubię uczyć się nowych słów”.",
         "options": options("I", "like", "learning", "new", "words."), "correctOptionIds": ["o1", "o2", "o3", "o4", "o5"]},
        {"id": "picture-input", "type": "image_input", "prompt": "Jak nazywa się ta figura po angielsku?",
         "imageBase64": diagram(), "imageAlt": "Prostokąt o poziomej podstawie dłuższej od wysokości.", "answers": [["rectangle"]]},
        {"id": "picture-choice", "type": "image_choice", "prompt": "Wybierz angielską nazwę figury.",
         "imageBase64": diagram(), "imageAlt": "Prostokąt o poziomej podstawie dłuższej od wysokości.",
         "options": options("circle", "rectangle", "triangle"), "correctOptionIds": ["o2"]}
    ]}]
}

programming = {
    "schemaVersion": 1, "id": "python-demo", "version": 1,
    "title": "Pierwsze kroki w Pythonie",
    "description": "Kurs demonstracyjny · poznaj słowa i składnię kodu.", "subject": "programming",
    "lessons": [{"id": "basics", "title": "Czytamy kod", "exercises": [
        {"id": "boolean", "type": "choice", "prompt": "Które zapisy są wartościami logicznymi w Pythonie?", "multiple": True,
         "options": options("True", "False", "true", "false"), "correctOptionIds": ["o1", "o2"],
         "explanation": "Python rozróżnia wielkie i małe litery."},
        {"id": "print", "type": "word_input", "prompt": "Wpisz nazwę funkcji wypisującej tekst.",
         "template": "{{1}}(\"Hello\")", "answers": [["print"]]},
        {"id": "condition", "type": "sentence_input", "prompt": "Uzupełnij warunek i funkcję wypisującą tekst.",
         "template": "{{1}} ready:\n    {{2}}(\"Hello\")", "answers": [["if"], ["print"]]},
        {"id": "loop", "type": "sentence_bank", "prompt": "Uzupełnij pętlę liczącą od 0 do 2.",
         "template": "{{1}} i in {{2}}(3):\n    print(i)", "options": options("for", "range", "if", "return"), "correctOptionIds": ["o1", "o2"]},
        {"id": "keywords", "type": "sentence_select", "prompt": "Zaznacz słowa kluczowe Pythona.",
         "options": options("if", "ready", "and", "active", ":"), "correctOptionIds": ["o1", "o3"]},
        {"id": "assignment", "type": "sentence_order", "prompt": "Ułóż przypisanie wartości 3 do zmiennej count.",
         "options": options("count", "=", "3"), "correctOptionIds": ["o1", "o2", "o3"]},
        {"id": "output-input", "type": "image_input", "prompt": "Przepisz wynik programu widoczny po znaku >.",
         "imageBase64": diagram(True), "imageAlt": "Okno konsoli z wynikiem Hello po znaku zachęty.", "answers": [["Hello"]]},
        {"id": "output-choice", "type": "image_choice", "prompt": "Który kod wypisze tekst pokazany w konsoli?",
         "imageBase64": diagram(True), "imageAlt": "Okno konsoli pokazujące tekst Hello.",
         "options": options('print("Hello")', 'print("hello")', 'input("Hi")'), "correctOptionIds": ["o1"]}
    ]}]
}
for course in (language, programming):
    for lesson in course["lessons"]:
        for exercise in lesson["exercises"]:
            if exercise["type"] in ("word_input", "sentence_input", "image_input"):
                exercise["answerPolicy"] = {"allowTypos": course["subject"] == "language", "caseSensitive": course["subject"] == "programming", "trim": True}
    name = "programming-demo.json" if course["subject"] == "programming" else "language-demo.json"
    for directory in (ROOT / "courses", ROOT / "app/src/main/assets"):
        directory.mkdir(parents=True, exist_ok=True)
        (directory / name).write_text(json.dumps(course, ensure_ascii=False, indent=2) + "\n")
