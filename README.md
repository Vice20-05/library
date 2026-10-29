# Könyvtári rendszer fájlba mentéssel (b-11)

A console program for the faculty library. All data is stored in a file, so it
survives a restart. Saving is **safe**: the program writes a temporary file first and then
renames it in one step, so a power cut during saving can't destroy the old data.

## Features

- Books (ISBN, title, author, year, copies), readers (card number, name), loans
- `equals`/`hashCode`: books by **ISBN**, readers by **card number**
- Search in title **or** author, case-insensitive
- Sorting by title or by year (`Comparator` constants in `Konyv`)
- Borrowing: if there is no free copy you get an error message
- Loan period **14 days**, late fee **100 Ft / day**
- Three storage formats behind one `KonyvtarTar` interface:
  1. `SzerializaltTar`: Java serialization (`ObjectOutputStream`)
  2. `CsvTar`: our own CSV format (`BufferedWriter` / `BufferedReader`)
  3. `JsonTar`: JSON with Gson (stretch task)
- Every stream is in try-with-resources
- A damaged file gives a clear message (`SerultFajlKivetel`) instead of a crash. The program
  copies the damaged file to `*.hibas` and starts with an empty library.

## Requirements

- Java JDK 17 or newer
- `lib/` contains `gson-2.15.0.jar` and the JUnit standalone jar

## Project structure

```
library/
├── src/
│   ├── Main.java              -> console menu, chooses the storage format
│   ├── model/                 -> Konyv, Olvaso, Kolcsonzes, Konyvtar
│   └── tar/                   -> KonyvtarTar (interface), SzerializaltTar, CsvTar, JsonTar,
│                                 BiztonsagosMentes (tmp + ATOMIC_MOVE), SerultFajlKivetel
├── test/
│   ├── model/KonyvtarTest.java     -> search, sort, borrow rules, late fee, equals
│   └── tar/KonyvtarTarTest.java    -> save/load with @TempDir, damaged files
├── data/konyvtar_pelda.csv    -> example data file
├── docs/konyvtar.puml         -> PlantUML class diagram
├── lib/                       -> gson + junit jars
└── .vscode/                   -> settings + run configs (CSV / ser / JSON)
```

## How to run

```bash
javac -d out -cp lib/gson-2.15.0.jar src/Main.java src/model/*.java src/tar/*.java
java -cp out:lib/gson-2.15.0.jar Main csv     # or: ser, json   (default: csv)
```

The data file is `konyvtar.csv` / `konyvtar.ser` / `konyvtar.json` in the current folder.
If it doesn't exist yet, the program starts with some sample data.
To try the example file: `cp data/konyvtar_pelda.csv konyvtar.csv`, then run with `csv`.

**In VS Code:** Run and Debug (Ctrl+Shift+D) has three configurations:
*Library (CSV)*, *Library (serialization)*, *Library (JSON)*. They run in the integrated
terminal, so you can type into the menu.

## How to run the tests

```bash
javac -d out -cp lib/gson-2.15.0.jar src/Main.java src/model/*.java src/tar/*.java
javac -d out_test -cp out:lib/gson-2.15.0.jar:lib/junit-platform-console-standalone-6.1.3.jar test/model/*.java test/tar/*.java
java -jar lib/junit-platform-console-standalone-6.1.3.jar \
  execute --class-path out:out_test:lib/gson-2.15.0.jar --scan-class-path
```

There are 22 tests in total. The most important ones:

- save → load gives the same state with **all three** storage formats (`@TempDir`)
- late fee: on time = 0 Ft, 3 days late = 300 Ft
- search is case-insensitive and also looks at the author
- no free copy → `IllegalStateException`
- damaged CSV / SER / JSON and a half-written SER file → `SerultFajlKivetel`
- no `.tmp` file is left after saving

## The CSV format

- UTF-8 text file, **one record per line**, fields separated by **`;`**
- The first line is always the header: `# KONYVTAR CSV v1`. Without it the file is rejected.
- The first field is the record type:

| Type         | Fields                                                  |
|--------------|---------------------------------------------------------|
| `KONYV`      | `KONYV;isbn;title;author;year;copies`                   |
| `OLVASO`     | `OLVASO;cardNumber;name`                                |
| `KOLCSONZES` | `KOLCSONZES;isbn;cardNumber;borrowDate;dueDate`         |

- Dates are `YYYY-MM-DD` (ISO, `LocalDate.toString()`)
- Books and readers come first, because the loans refer to them by ISBN / card number
- Empty lines are ignored
- Escaping inside a field: `;` → `\;`, `\` → `\\`, newline → `\n`
- On a wrong line the error message tells the line number, e.g.
  `Error in line 2: For input string: "not-a-year"`

### Example data file (`data/konyvtar_pelda.csv`)

```
# KONYVTAR CSV v1
KONYV;978-0134685991;Effective Java;Joshua Bloch;2018;2
KONYV;978-0132350884;Clean Code;Robert C. Martin;2008;1
KONYV;978-0596009205;Head First Java;Kathy Sierra;2005;3
KONYV;978-9634155000;A Pál utcai fiúk;Molnár Ferenc;1906;2
KONYV;978-0000000001;Tom\; Jerry;Hanna\; Barbera;1990;1
OLVASO;R001;Kiss Anna
OLVASO;R002;Nagy Péter
KOLCSONZES;978-0132350884;R001;2026-10-08;2026-10-22
KOLCSONZES;978-0134685991;R002;2026-10-25;2026-11-08
```

## Safe saving

All three formats save the same way (`BiztonsagosMentes`):

1. write everything into `konyvtar.csv.tmp` (try-with-resources closes the file)
2. `Files.move(tmp, file, ATOMIC_MOVE, REPLACE_EXISTING)` replaces the old file in one step
3. if writing fails, the `.tmp` file is deleted and the old file stays untouched

If the file system can't do an atomic move, a normal move is used as a fallback.

## UML

`docs/konyvtar.puml`: open it with the PlantUML extension (Alt+D). It shows the `KonyvtarTar`
interface with its three implementations, and the composition between `Konyvtar` and its lists.

## Not done (stretch)

- Auto-save every 5 minutes
- Swing UI with JTable

## Note

Claude only checked if the folders have README and gitignore and pushed them so all of them are pushed
