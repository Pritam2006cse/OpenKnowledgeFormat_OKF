import pymupdf
import sys
from pathlib import Path

def pdf_to_markdown(pdf_path,output_path):
    pdf_path = Path(pdf_path)
    output_path = Path(output_path)

    document = pymupdf.open(pdf_path)
    markdown = []

    title = pdf_path.stem.replace("_", " ").replace("-", " ").title()
    markdown.append(f"# {title}\n")

    for page_number,page in enumerate(document,start=1):
        text = page.get_text("text").strip()
        if not text:
            continue
        markdown.append(f"##{page_number}\n")
        markdown.append(text)
        markdown.append("\n")

    output_path.write_text("\n".join(markdown),encoding="utf-8")
    document.close()
    print("PDF converted successfully.")
    print(f"Input : {pdf_path}")
    print(f"Output: {output_path}")

if __name__ == "__main__":
    if len(sys.argv) != 3:

        print(
            "Usage: python pdf_to_markdown.py "
            "<input.pdf> <output.md>"
        )

        sys.exit(1)

    pdf_to_markdown(
        sys.argv[1],
        sys.argv[2]
    )
