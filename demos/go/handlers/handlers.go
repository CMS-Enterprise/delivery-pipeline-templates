// Package handlers are the template handlers
package handlers

import (
	"html/template"
	"net/http"
	"path/filepath"
)

type TemplateHandler struct {
	templates *template.Template
}

// NewTemplateHandler grabs the templates.
func NewTemplateHandler() (*TemplateHandler, error) {
	tmpl := template.Must(template.ParseGlob(filepath.Join("templates", "*.html")))
	return &TemplateHandler{templates: tmpl}, nil
}

// Home is for home.html .
func (h *TemplateHandler) Home(w http.ResponseWriter, r *http.Request) {
	if r.URL.Path != "/" {
		http.NotFound(w, r)
		return
	}
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	err := h.templates.ExecuteTemplate(w, "home.html", map[string]string{"PageTitle": "Home"})
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
	}
}

// About is for about.html .
func (h *TemplateHandler) About(w http.ResponseWriter, r *http.Request) {
	if r.URL.Path != "/about" {
		http.NotFound(w, r)
		return
	}
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	err := h.templates.ExecuteTemplate(w, "about.html", map[string]string{"PageTitle": "About"})
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
	}
}
