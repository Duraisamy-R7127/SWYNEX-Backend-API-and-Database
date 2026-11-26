import { Link } from 'react-router-dom'
import { Search, MapPin, Shield, Bell, ArrowRight, CheckCircle, Users, Package } from 'lucide-react'

export default function LandingPage() {
  return (
    <div className="landing">
      {/* Navbar */}
      <nav className="landing__nav">
        <div className="landing__nav-inner">
          <Link to="/" className="landing__nav-logo">
            <Search size={22} />
            <span>College L&amp;F Hub</span>
          </Link>
          <div className="landing__nav-links">
            <Link to="/items" className="landing__nav-link">Found Items</Link>
            <Link to="/admin/login" className="landing__nav-link">Admin Portal</Link>
            <Link to="/login" className="btn btn--ghost">Student Sign In</Link>
            <Link to="/register" className="btn btn--primary">Student Register</Link>
          </div>
        </div>
      </nav>

      {/* Hero */}
      <section className="hero">
        <div className="hero__content">
          <div className="hero__badge">🎓 College Official Lost &amp; Found System</div>
          <h1 className="hero__title">
            Recover Your<br />
            <span className="hero__title-accent">Campus Belongings</span>
          </h1>
          <p className="hero__subtitle">
            Search items officially found and registered by the college administration,
            report lost belongings, and submit ownership claims for verified return.
          </p>
          <div className="hero__actions">
            <Link to="/items" className="btn btn--primary btn--lg">
              Search Found Items <ArrowRight size={18} />
            </Link>
            <Link to="/login" className="btn btn--outline btn--lg">
              Student Login
            </Link>
            <Link to="/admin/login" className="btn btn--ghost btn--lg">
              <Shield size={18} /> Admin Login
            </Link>
          </div>
        </div>
        <div className="hero__visual">
          <div className="hero__card hero__card--1">
            <div className="hero__card-icon hero__card-icon--green"><CheckCircle size={20} /></div>
            <div>
              <div className="hero__card-title">Item Returned!</div>
              <div className="hero__card-sub">Verified by College Admin</div>
            </div>
          </div>
          <div className="hero__card hero__card--2">
            <div className="hero__card-icon hero__card-icon--blue"><MapPin size={20} /></div>
            <div>
              <div className="hero__card-title">Official Found Desk</div>
              <div className="hero__card-sub">Main Library &amp; Campus</div>
            </div>
          </div>
          <div className="hero__card hero__card--3">
            <div className="hero__card-icon hero__card-icon--purple"><Bell size={20} /></div>
            <div>
              <div className="hero__card-title">Claim Approved</div>
              <div className="hero__card-sub">Ready for campus pickup</div>
            </div>
          </div>
          <div className="hero__blob" />
        </div>
      </section>

      {/* Features */}
      <section className="features">
        <div className="features__inner">
          <div className="section-header">
            <h2 className="section-title">How It Helps</h2>
            <p className="section-sub">Official campus lost and found recovery</p>
          </div>
          <div className="features__grid">
            {[
              { icon: Shield, color: 'green', title: 'Admin-Verified Listings', desc: 'Official found items published by college staff.' },
              { icon: Search, color: 'blue', title: 'Search & Filter', desc: 'Filter by name, category, location, and date.' },
              { icon: Package, color: 'teal', title: 'Lost Reports', desc: 'Report lost items with photos and details.' },
              { icon: CheckCircle, color: 'purple', title: 'Ownership Claims', desc: 'Submit proof and get admin-verified approval.' },
              { icon: Bell, color: 'orange', title: 'Notifications', desc: 'Instant updates on claim approvals and returns.' },
              { icon: Users, color: 'red', title: 'All Categories', desc: 'Phones, ID cards, wallets, bags, books, and more.' },
            ].map(({ icon: Icon, color, title, desc }) => (
              <div key={title} className="feature-card">
                <div className={`feature-card__icon feature-card__icon--${color}`}><Icon size={24} /></div>
                <h3 className="feature-card__title">{title}</h3>
                <p className="feature-card__desc">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* How it works */}
      <section className="how-it-works">
        <div className="how-it-works__inner">
          <div className="section-header">
            <h2 className="section-title">3 Simple Steps</h2>
          </div>
          <div className="steps">
            {[
              { num: '01', title: 'Search or Report', desc: 'Search official found items or report a lost item.' },
              { num: '02', title: 'Submit Claim', desc: 'Provide ownership proof for a matching item.' },
              { num: '03', title: 'Collect Item', desc: 'Admin verifies and hands over your belonging.' },
            ].map((step) => (
              <div key={step.num} className="step">
                <div className="step__num">{step.num}</div>
                <h3 className="step__title">{step.title}</h3>
                <p className="step__desc">{step.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="cta">
        <div className="cta__inner">
          <h2 className="cta__title">Lost Something on Campus?</h2>
          <p className="cta__sub">Sign in with your student account to report lost items and claim found belongings.</p>
          <div className="cta__actions">
            <Link to="/register" className="btn btn--white btn--lg">Student Register</Link>
            <Link to="/items" className="btn btn--outline-white btn--lg">Search Found Items</Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="landing__footer">
        <div className="landing__footer-inner">
          <div className="landing__footer-logo">
            <Search size={18} />
            <span>College Lost &amp; Found Hub</span>
          </div>
          <p className="landing__footer-copy">
            <Link to="/admin/login" style={{ color: 'inherit', textDecoration: 'underline' }}>Admin Portal</Link>
          </p>
        </div>
      </footer>
    </div>
  )
}
