# Go Site - RoboCare Health

## Project Overview
Go web application using net/http and html/template for the RoboCare Health marketing site. Serves on port 3005.

## Tech Stack
- Go 1.21+
- net/http (standard library)
- html/template (standard library)

## Project Structure
- `main.go` - HTTP server, route handlers
- `templates/` - HTML templates (layout.html, home.html, about.html)
- `static/css/` - Stylesheets
- `tests/jmeter/` - JMeter test plans

## Development
- Run: `go run main.go`
- Build: `go build -o go-site`

## Conventions
- Use standard library only (no external dependencies)
- Templates use Go's html/template with define/template blocks
- Static files served from /static/ path
