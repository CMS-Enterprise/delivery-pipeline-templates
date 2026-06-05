export default function Home() {
  return (
    <>
      <section className="hero">
        <h1>The Future of Healthcare is Robotic</h1>
        <p className="hero-subtitle">
          RoboCare Health delivers next-generation robotic systems that assist surgeons, accelerate rehabilitation, and bring precision diagnostics to hospitals worldwide.
        </p>
      </section>

      <section className="features">
        <div className="feature-grid">
          <div className="feature-card">
            <h3>Robotic Surgery</h3>
            <p>Our surgical robots provide sub-millimeter precision, enabling minimally invasive procedures with faster patient recovery and reduced complication rates.</p>
          </div>
          <div className="feature-card">
            <h3>AI Diagnostics</h3>
            <p>Machine learning models analyze medical imaging in real-time, detecting anomalies that human eyes might miss — from early-stage tumors to micro-fractures.</p>
          </div>
          <div className="feature-card">
            <h3>Rehabilitation Robotics</h3>
            <p>Adaptive exoskeletons and robotic therapy arms guide patients through personalized recovery programs, tracking progress with sensor-driven feedback.</p>
          </div>
        </div>
      </section>

      <section className="stats">
        <div className="feature-grid">
          <div className="stat-card">
            <span className="stat-number">1,200+</span>
            <span className="stat-label">Surgeries Assisted</span>
          </div>
          <div className="stat-card">
            <span className="stat-number">98.7%</span>
            <span className="stat-label">Diagnostic Accuracy</span>
          </div>
          <div className="stat-card">
            <span className="stat-number">40%</span>
            <span className="stat-label">Faster Recovery</span>
          </div>
        </div>
      </section>
    </>
  );
}
