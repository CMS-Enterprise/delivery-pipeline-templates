"""View tests for the pages app."""

import pytest


@pytest.mark.parametrize("path", ["/", "/about/"])
def test_page_returns_ok(client, path):
    assert client.get(path).status_code == 200


def test_home_uses_home_template(client):
    response = client.get("/")
    names = [t.name for t in response.templates]
    assert "pages/home.html" in names
    assert "base.html" in names


def test_about_uses_about_template(client):
    response = client.get("/about/")
    names = [t.name for t in response.templates]
    assert "pages/about.html" in names
    assert "base.html" in names


def test_home_renders_hero_heading(client):
    content = client.get("/").content.decode()
    assert "Where Robotics Meets Patient Care" in content


def test_about_renders_headings(client):
    content = client.get("/about/").content.decode()
    assert "About RoboCare Health" in content
    assert "Clinical Validation" in content
    assert "Global Reach" in content


def test_unknown_path_returns_404(client):
    assert client.get("/no-such-page/").status_code == 404
