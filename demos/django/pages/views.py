"""Views for Pages"""

from django.shortcuts import render


def home(request):
    """Home View"""
    return render(request, "pages/home.html")


def about(request):
    """About View"""
    return render(request, "pages/about.html")
