"""URL routing tests for the pages app."""

from django.urls import resolve, reverse

from pages import views


def test_home_url_resolves_to_home_view():
    assert resolve("/").func is views.home


def test_about_url_resolves_to_about_view():
    assert resolve("/about/").func is views.about


def test_home_reverses_to_root():
    assert reverse("home") == "/"


def test_about_reverses_with_trailing_slash():
    assert reverse("about") == "/about/"
