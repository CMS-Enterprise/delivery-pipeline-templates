package main

import (
	"html/template"
	"log"
	"net/http"
	"path/filepath"
)

var templates *template.Template

func init() {
	templates = template.Must(template.ParseGlob(filepath.Join("templates", "*.html")))
}

func homeHandler(w http.ResponseWriter, r *http.Request) {
	if r.URL.Path != "/" {
		http.NotFound(w, r)
		return
	}
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	templates.ExecuteTemplate(w, "home.html", map[string]string{"PageTitle": "Home"})
}

func aboutHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	templates.ExecuteTemplate(w, "about.html", map[string]string{"PageTitle": "About"})
}

func main() {
	http.Handle("/static/", http.StripPrefix("/static/", http.FileServer(http.Dir("static"))))
	http.HandleFunc("/", homeHandler)
	http.HandleFunc("/about", aboutHandler)

	log.Println("Starting server on :3005")
	log.Fatal(http.ListenAndServe(":3005", nil))
}
