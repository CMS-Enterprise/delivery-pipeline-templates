"""Tests for the shared base layout inherited by every page."""

import pytest


@pytest.mark.parametrize("path", ["/", "/about/"])
def test_layout_chrome_present_on_every_page(client, path):
    content = client.get(path).content.decode()
    assert 'class="navbar"' in content
    assert 'class="footer"' in content
    assert 'href="/"' in content
    assert 'href="/about/"' in content


@pytest.mark.parametrize(
    ("path", "title"),
    [
        ("/", "RoboCare Health — Robotic Health Care Solutions"),
        ("/about/", "About — RoboCare Health"),
    ],
)
def test_each_page_overrides_the_title_block(client, path, title):
    assert f"<title>{title}</title>" in client.get(path).content.decode()


def test_stylesheet_is_linked(client):
    assert "/static/css/style.css" in client.get("/").content.decode()
