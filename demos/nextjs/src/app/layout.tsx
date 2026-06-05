import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'RoboCare Health — Robotic Health Care Solutions',
  description: 'Pioneering the future of healthcare with advanced robotics, AI diagnostics, and precision surgical systems.',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <nav className="navbar">
          <div className="container">
            <a href="/" className="nav-brand">RoboCare Health</a>
            <ul className="nav-links">
              <li><a href="/">Home</a></li>
              <li><a href="/about">About</a></li>
            </ul>
          </div>
        </nav>
        <main className="container">{children}</main>
        <footer className="footer">
          <div className="container">
            <p>&copy; 2026 RoboCare Health. Advancing healthcare through robotics.</p>
          </div>
        </footer>
      </body>
    </html>
  );
}
