"""Custom MCP server (FastMCP) for Homework 5, Task 4.

Concepts:
- Resources are URIs Claude can read from (e.g., files, APIs).
- Tools are actions Claude can call to perform operations (e.g., reading a file).

This server exposes:
- a Resource that returns the first `word_count` words (default 30) of `lorem-ipsum.md`
- a Tool named `read` that does the same, with an optional `word_count` parameter
"""

from pathlib import Path

from fastmcp import FastMCP

# Resolve the source file relative to THIS file, so the server works regardless of
# the current working directory the MCP client launches it from.
LOREM_PATH = Path(__file__).resolve().parent / "lorem-ipsum.md"
DEFAULT_WORD_COUNT = 30

mcp = FastMCP("custom-lorem")


def get_words(word_count: int = DEFAULT_WORD_COUNT) -> str:
    """Return exactly the first `word_count` words from lorem-ipsum.md.

    Non-positive counts yield an empty string; counts larger than the file
    return all available words.
    """
    count = max(0, int(word_count))
    words = LOREM_PATH.read_text(encoding="utf-8").split()
    return " ".join(words[:count])


@mcp.resource("lorem://words")
def lorem_default() -> str:
    """Default resource: first 30 words of lorem-ipsum.md."""
    return get_words(DEFAULT_WORD_COUNT)


@mcp.resource("lorem://words/{word_count}")
def lorem_words(word_count: int) -> str:
    """Parameterized resource: first `word_count` words of lorem-ipsum.md."""
    return get_words(word_count)


@mcp.tool
def read(word_count: int = DEFAULT_WORD_COUNT) -> str:
    """Read `word_count` words (default 30) from the lorem-ipsum resource."""
    return get_words(word_count)


if __name__ == "__main__":
    mcp.run()
