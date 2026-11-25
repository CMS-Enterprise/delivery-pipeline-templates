"""Views for Pages"""

from django.http import JsonResponse
from django.shortcuts import render
from django.views.decorators.http import require_safe


@require_safe
def health(request):
    """Health View"""
    return JsonResponse({"status": "ok"})


@require_safe
def home(request):
    """Home View"""
    return render(request, "pages/home.html")


@require_safe
def about(request):
    """About View"""
    return render(request, "pages/about.html")
