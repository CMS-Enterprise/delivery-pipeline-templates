// Package main is the heart of this webapp.
package main

import (
	"log"
	"net/http"
	"time"

	"jason.castonguay/handlers"
)

const (
	ReadSeconds = 5
	IdleSeconds = 15
)

// main sets up our main.
func main() {
	router := http.NewServeMux()
	server := &http.Server{
		Addr:         ":3005",
		Handler:      router,
		ReadTimeout:  ReadSeconds * time.Second,
		WriteTimeout: time.Second,
		IdleTimeout:  IdleSeconds * time.Second,
	}
	handler, err := handlers.NewTemplateHandler()
	if err != nil {
		log.Fatal(err)
	}

	router.Handle("/static/", http.StripPrefix("/static/", http.FileServer(http.Dir("static"))))
	router.HandleFunc("/", handler.Home)
	router.HandleFunc("/about", handler.About)

	log.Println("Starting server on :3005")
	log.Fatal(server.ListenAndServe())
}
