# Generating Javadoc Documentation

## Prerequisites

- Java 17 or higher (the `javadoc` tool is bundled with the JDK)
- Run all commands from the **project root** directory

## Quick start

```bash
javadoc -d docs/javadoc \
        -sourcepath src \
        -encoding UTF-8 \
        -charset UTF-8 \
        -author \
        -version \
        -windowtitle "Book Catalogue API" \
        -doctitle "Book Catalogue — Javadoc" \
        src/*.java
```

Open `docs/javadoc/index.html` in any browser to browse the generated documentation.

## Parameters explained

| Flag | Purpose |
|---|---|
| `-d docs/javadoc` | Output directory for the generated HTML |
| `-sourcepath src` | Root directory where Java source files are located |
| `-encoding UTF-8` | Source file encoding |
| `-charset UTF-8` | HTML output encoding |
| `-author` | Include `@author` tags in the output |
| `-version` | Include `@version` tags in the output |
| `-windowtitle` | Browser tab/window title |
| `-doctitle` | H1 heading on the overview page |

## What gets documented

Javadoc generates documentation for **public** and **protected** members by default.
All five classes are covered:

| Class | Role |
|---|---|
| `Publication` | Base class — shared title/year fields |
| `Book` | Concrete subclass — adds author, publisher, genre |
| `Catalogue` | Business logic — CRUD, search, export, persistence |
| `BookNotFoundException` | Checked exception for missing titles |
| `BookGUI` | Swing UI — view + controller layer |

## Creating a documentation archive

To package the generated HTML for submission:

```bash
cd docs
zip -r ../javadoc-docs.zip javadoc/
```

## Keeping documentation up to date

Javadoc must be regenerated every time public interfaces change.
Add the generation step to your build process so it never falls out of sync:

```bash
# Example: add to a Makefile or build.sh
javac -d out src/*.java && javadoc -d docs/javadoc src/*.java
```

## GitHub Pages deployment

The `docs/javadoc/` folder is configured as the GitHub Pages source.
After merging to `main` and enabling Pages in repository Settings → Pages → Source: `main` branch, `/docs` folder, the documentation is available at:

```
https://marianazarenkostud.github.io/Library/
```

## Warnings about private fields

`javadoc` may report `warning: no comment` for **private** fields.
This is expected — private members are not part of the public API and do not require Javadoc.
All public and protected interfaces are fully documented.
