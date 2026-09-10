"""Views for Pages"""

from django.http import JsonResponse
from django.shortcuts import render


def health(request):
    """Health View"""
    return JsonResponse({"status": "ok"})


def home(request):
    """Home View"""
    return render(request, "pages/home.html")


def about(request):
    """About View"""
    return render(request, "pages/about.html")
