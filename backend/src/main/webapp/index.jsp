<%@ page pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <title>UCI BioSci — TA Application System</title>
  <meta name="ctx" content="<%= request.getContextPath() %>">
  <jsp:include page="/common/head.jsp"/>
</head>
<body class="min-h-screen bg-zinc-950 text-white selection:bg-zinc-200 selection:text-zinc-900 overflow-x-hidden">
  <%
    String ctx = request.getContextPath();
  %>
  <nav class="fixed inset-x-0 top-0 z-50 flex h-20 items-center justify-between px-8 bg-zinc-950/40 backdrop-blur-lg border-b border-white/5 transition-all" id="navbar">
    <div class="flex items-center gap-3 font-semibold text-xl tracking-tight">
      <a href="<%= ctx %>/index.jsp" class="uci-app-brand" aria-label="UCI BioSci TA Application System home"><span>UCI BioSci</span><small>TA Application System</small></a>
    </div>
    <div class="flex items-center gap-6">
      <a href="<%= ctx %>/auth/login.jsp" class="text-sm font-medium text-zinc-400 hover:text-white transition-colors">Log in</a>
      <a href="<%= ctx %>/auth/signup.jsp" class="rounded-full bg-white px-5 py-2.5 text-sm font-semibold text-zinc-950 hover:bg-zinc-200 transition-colors shadow-sm">Get Started</a>
    </div>
  </nav>

  <!-- Parallax Hero Section with Canvas -->
  <div class="fixed inset-0 z-0 h-screen w-full dynamic-bg overflow-hidden parallax-bg" id="parallax-layer">
    <canvas id="hero-canvas" class="absolute inset-0 z-0 opacity-70"></canvas>
    <div class="absolute inset-0 bg-gradient-to-b from-transparent via-zinc-950/20 to-zinc-950/80 pointer-events-none"></div>
  </div>

  <main class="relative z-10 pt-48 pb-20 flex flex-col items-center text-center px-6 parallax-bg" id="parallax-content">
    <div class="uci-hero-identity"><jsp:include page="/common/uci-wordmark.jsp"/></div>
    <div class="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-1.5 text-sm font-medium text-zinc-300 mb-10 shadow-sm slide-up-element backdrop-blur-sm">
      <i data-lucide="zap" class="h-4 w-4 text-indigo-400"></i>
      <span>TA applications for UCI BioSci</span>
    </div>

    <h1 class="text-6xl md:text-8xl font-black tracking-tight text-white max-w-5xl w-full slide-up-element delay-100 leading-tight drop-shadow-2xl min-h-[160px] md:min-h-[248px] flex flex-col items-center justify-center">
      <span>Elevate your</span>
      <span id="typewriter-text" class="text-indigo-400 block mt-2"></span>
    </h1>

    <p class="mt-12 text-xl leading-relaxed text-left text-zinc-300 w-full max-w-4xl slide-up-element delay-200">
      A unified workspace for UCI BioSci students, faculty, and administrators to manage TA applications, reviews, and teaching assignments.
    </p>

    <div class="mt-14 flex flex-col sm:flex-row items-center gap-4 w-full justify-center slide-up-element delay-300 relative z-20">
      <a href="<%= ctx %>/auth/signup.jsp" class="flex items-center justify-center gap-2 rounded-full bg-white px-10 py-5 text-lg font-bold text-zinc-950 hover:bg-zinc-200 transition-all hover:scale-105 active:scale-95 shadow-[0_0_40px_rgba(255,255,255,0.2)] w-full sm:w-auto">
        Apply now
        <i data-lucide="arrow-right" class="h-5 w-5"></i>
      </a>
      <a href="<%= ctx %>/auth/login.jsp" class="flex items-center justify-center gap-2 rounded-full border border-white/20 bg-black/20 backdrop-blur-md px-10 py-5 text-lg font-bold text-white hover:bg-white/10 transition-all w-full sm:w-auto hover:scale-105 active:scale-95">
        View as Applicant
      </a>
    </div>

    <!-- Biology laboratory scene -->
    <div class="mt-20 w-full max-w-6xl relative slide-up-element delay-500 z-30">
      <div class="relative rounded-3xl border border-zinc-800 bg-zinc-900/50 p-2 shadow-2xl backdrop-blur-sm" data-tilt>
        <img src="https://images.unsplash.com/photo-1578496479914-7ef3b0193be3?auto=format&fit=crop&w=1600&q=80"
             alt="Laboratory technician observing cells through a microscope" width="1600" height="900"
             class="w-full aspect-video rounded-2xl object-cover shadow-[0_20px_50px_rgba(0,0,0,0.5)]">
        <div class="absolute inset-0 rounded-2xl shadow-[inset_0_0_0_1px_rgba(255,255,255,0.1)] pointer-events-none"></div>
      </div>
      <!-- Glow behind laboratory photo -->
      <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[120%] h-[120%] bg-indigo-500/20 blur-[120px] rounded-full pointer-events-none -z-10"></div>
    </div>
  </main>

  <section class="bg-zinc-50 text-zinc-950 rounded-t-[4rem] py-32 relative z-40 shadow-[0_-30px_60px_rgba(0,0,0,0.6)] overflow-hidden">
    <!-- Stitch Interactive Matrix Canvas -->
    <div class="absolute inset-0 z-0 pointer-events-none"><canvas id="stitch-matrix-index" class="w-full h-full"></canvas></div>

    <div class="max-w-7xl mx-auto px-6 relative z-10 pointer-events-none">
      <div class="pointer-events-auto">

      <div class="text-center mb-24 slide-up-element">
        <h2 class="text-4xl md:text-5xl font-black tracking-tight mb-6">Designed for scale and clarity.</h2>
        <p class="text-xl text-zinc-500 max-w-2xl mx-auto">Everything you need to run a world-class teaching assistant program, packed into an impossibly fast interface.</p>
      </div>

      <!-- Zig Zag 1: Text Left, Image Right -->
      <div class="flex flex-col lg:flex-row items-center gap-16 mb-40">
        <div class="w-full lg:w-1/2 slide-up-element">
          <div class="inline-flex items-center gap-2 rounded-full border border-indigo-200 bg-indigo-50 px-4 py-1.5 text-sm font-bold text-indigo-700 mb-6">
            <i data-lucide="users" class="h-4 w-4"></i>
            <span>Applicant Profiles</span>
          </div>
          <h3 class="text-4xl font-bold tracking-tight mb-6 leading-tight">One profile.<br>Unlimited applications.</h3>
          <p class="text-lg text-zinc-600 mb-8 leading-relaxed">Students create a master profile with their transcripts, CVs, and availability. They can apply to dozens of modules with a single click, saving hours of repetitive form-filling.</p>
          <ul class="space-y-4">
            <li class="flex items-center gap-3 text-zinc-700 font-medium">
              <div class="h-8 w-8 rounded-full bg-zinc-100 flex items-center justify-center text-zinc-900"><i data-lucide="check" class="h-4 w-4"></i></div>
              Auto-sync GPA & Credits
            </li>
            <li class="flex items-center gap-3 text-zinc-700 font-medium">
              <div class="h-8 w-8 rounded-full bg-zinc-100 flex items-center justify-center text-zinc-900"><i data-lucide="check" class="h-4 w-4"></i></div>
              Centralized Resume Storage
            </li>
          </ul>
        </div>
        <div class="w-full lg:w-1/2 relative slide-up-element delay-200">
          <div class="rounded-[2.5rem] bg-zinc-100 border border-zinc-200 p-2 shadow-2xl relative" data-tilt data-tilt-scale="1.02">
            <img src="https://images.unsplash.com/photo-1741699428220-65f37f3fbbcb?auto=format&fit=crop&w=1200&q=80" alt="University student studying with books in a library" loading="lazy" decoding="async" width="1200" height="675" class="w-full rounded-[2rem] object-cover aspect-video">
          </div>
          <div class="absolute -top-10 -right-10 w-64 h-64 bg-indigo-400/20 blur-[60px] rounded-full pointer-events-none -z-10"></div>
        </div>
      </div>

      <!-- Zig Zag 2: Image Left, Text Right -->
      <div class="flex flex-col-reverse lg:flex-row items-center gap-16 mb-40">
        <div class="w-full lg:w-1/2 relative slide-up-element delay-200">
          <div class="rounded-[2.5rem] bg-zinc-100 border border-zinc-200 p-2 shadow-2xl relative" data-tilt data-tilt-scale="1.02">
            <img src="https://images.unsplash.com/photo-1762512346988-045f4d5ad2b3?auto=format&fit=crop&w=1200&q=80" alt="University students studying together around a library table" loading="lazy" decoding="async" width="1200" height="675" class="w-full rounded-[2rem] object-cover aspect-video">
          </div>
          <div class="absolute -top-10 -left-10 w-64 h-64 bg-purple-400/20 blur-[60px] rounded-full pointer-events-none -z-10"></div>
        </div>
        <div class="w-full lg:w-1/2 slide-up-element">
          <div class="inline-flex items-center gap-2 rounded-full border border-purple-200 bg-purple-50 px-4 py-1.5 text-sm font-bold text-purple-700 mb-6">
            <i data-lucide="calendar-clock" class="h-4 w-4"></i>
            <span>Schedule Compatibility</span>
          </div>
          <h3 class="text-4xl font-bold tracking-tight mb-6 leading-tight">Compare availability with section times.</h3>
          <p class="text-lg text-zinc-600 mb-8 leading-relaxed">Students provide weekly availability and course staff maintain section meeting times. Reviewers can check schedule compatibility and record assignments with conflict warnings.</p>
          <ul class="space-y-4">
            <li class="flex items-center gap-3 text-zinc-700 font-medium">
              <div class="h-8 w-8 rounded-full bg-zinc-100 flex items-center justify-center text-zinc-900"><i data-lucide="check" class="h-4 w-4"></i></div>
              Weekly Availability
            </li>
            <li class="flex items-center gap-3 text-zinc-700 font-medium">
              <div class="h-8 w-8 rounded-full bg-zinc-100 flex items-center justify-center text-zinc-900"><i data-lucide="check" class="h-4 w-4"></i></div>
              Section Conflict Checks
            </li>
          </ul>
        </div>
      </div>

      </div> <!-- End pointer events wrapper -->
    </div>
  </section>

  <!-- Dark Footer -->
  <footer class="bg-zinc-950 py-12 border-t border-zinc-900 text-center relative z-40">
    <div class="mx-auto w-full max-w-sm px-8 py-4"><jsp:include page="/common/uci-wordmark.jsp"/></div>
    <p class="mt-6 text-sm text-blue-200">UCI BioSci · TA Application System</p>
  </footer>

  <script src="<%= ctx %>/static/js/api.js"></script>
  <script src="<%= ctx %>/static/js/particles.js"></script>
  <script src="<%= ctx %>/static/js/tilt.js"></script>
  <script src="<%= ctx %>/static/js/typewriter.js"></script>
  <script src="<%= ctx %>/static/js/stitch-matrix.js?v=<%= System.currentTimeMillis() %>"></script>
  <script>
    lucide.createIcons();

    // Initialize interactive particles
    window.addEventListener('DOMContentLoaded', () => {
      if(window.initParticles) {
        window.initParticles('hero-canvas');
      }
      if(window.initStitchMatrix) {
        window.initStitchMatrix('stitch-matrix-index');
      }

      // Initialize Simple Text Typewriter
      if(window.initSimpleTypewriter) {
        const words = [
          "academic workflow.",
          "TA applications.",
          "section assignments.",
          "candidate tracking."
        ];
        window.initSimpleTypewriter('typewriter-text', words);
      }
    });

    // Intersection Observer for slide-up scroll animations
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.1, rootMargin: '0px 0px -50px 0px' });

    document.querySelectorAll('.slide-up-element').forEach((el) => {
      observer.observe(el);
    });

    // Parallax & Fade effect on scroll for the hero section
    const parallaxLayer = document.getElementById('parallax-layer');
    const parallaxContent = document.getElementById('parallax-content');
    const navbar = document.getElementById('navbar');

    window.addEventListener('scroll', () => {
      let scrollY = window.scrollY;

      if(scrollY < window.innerHeight) {
        let scale = 1 + (scrollY * 0.0005);
        let translateY = scrollY * 0.4;
        parallaxLayer.style.transform = `translateY(${translateY}px) scale(${scale})`;

        // Navbar blur adjust
        if(scrollY > 50) {
          navbar.classList.add('bg-zinc-950/80');
          navbar.classList.remove('bg-zinc-950/40');
        } else {
          navbar.classList.remove('bg-zinc-950/80');
          navbar.classList.add('bg-zinc-950/40');
        }
      }
    });
  </script>
</body>
</html>
