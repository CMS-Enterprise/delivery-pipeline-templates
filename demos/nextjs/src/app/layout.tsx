import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "RoboCare Health — Robotic Health Care Solutions",
  description:
    "Pioneering the future of healthcare with advanced robotics, AI diagnostics, and precision surgical systems.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <nav className="navbar">
          <div className="container">
            <Link href="/" className="nav-brand">
              RoboCare Health
            </Link>
            <ul className="nav-links">
              <li>
                <Link href="/">Home</Link>
              </li>
              <li>
                <Link href="/about">About</Link>
              </li>
            </ul>
          </div>
        </nav>
        <main className="container">{children}</main>
        <footer className="footer">
          <div className="container">
            <p>
              &copy; 2026 RoboCare Health. Advancing healthcare through
              robotics.
            </p>
          </div>
        </footer>
      </body>
    </html>
  );
}
