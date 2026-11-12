<%@ page pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <title>Sign In - UCI BioSci — TA Application System</title>
  <meta name="ctx" content="<%= request.getContextPath() %>">
  <jsp:include page="/common/head.jsp"/>
</head>
<body class="flex flex-col min-h-screen bg-zinc-50 text-zinc-900 selection:bg-zinc-200 selection:text-zinc-900">
  <%
    String ctx = request.getContextPath();
  %>
  
  <!-- Top Dark Header Section -->
  <div class="relative bg-zinc-950 text-white pt-8 pb-32 px-4 sm:px-8 flex flex-col justify-between dynamic-bg overflow-hidden">
    <canvas id="auth-canvas" class="absolute inset-0 z-0 opacity-50"></canvas>
    <!-- Back to home nav -->
    <div class="flex items-center justify-between w-full max-w-5xl mx-auto relative z-10">
      <a href="<%= ctx %>/index.jsp" class="flex items-center gap-2 text-zinc-400 hover:text-white transition-colors group">
        <i data-lucide="arrow-left" class="h-5 w-5 group-hover:-translate-x-1 transition-transform"></i>
        <span class="font-medium text-sm sm:text-base"><span class="sm:hidden">Home</span><span class="hidden sm:inline">Back to Home</span></span>
      </a>
      <a class="uci-auth-identity" href="https://www.bio.uci.edu/" aria-label="Visit the UCI School of Biological Sciences website"><jsp:include page="/common/uci-wordmark.jsp"/></a>
    </div>
    
    <!-- Title Area -->
    <div class="w-full max-w-5xl mx-auto mt-16 slide-up-element">
      <div class="text-zinc-400 font-medium tracking-wide mb-3 text-lg">Welcome back to UCI BioSci</div>
      <h1 class="text-6xl md:text-8xl font-black tracking-tighter h-[96px] flex flex-col justify-end">
        <span id="typewriter-text" class="text-white"></span>
      </h1>
      <div class="mt-6 text-zinc-400 text-xl flex items-center gap-2 font-medium">
        <span>Continue your BioSci TA journey.</span>
        <span>Don't have an account?</span>
        <a data-create-account href="<%= ctx %>/auth/signup.jsp" class="text-white border-b-2 border-white/30 border-dashed hover:border-solid hover:border-white transition-all ml-1">Create account &rarr;</a>
      </div>
    </div>
  </div>

  <!-- Bottom Form Panel -->
  <div class="flex-grow bg-white rounded-t-[3rem] -mt-16 relative z-10 shadow-[0_-20px_50px_rgba(0,0,0,0.4)] overflow-hidden">
    <!-- Stitch Matrix Canvas -->
    <div class="absolute inset-0 z-0 pointer-events-none"><canvas id="stitch-matrix-login" class="w-full h-full"></canvas></div>
    
    <div class="max-w-5xl mx-auto w-full pt-16 px-6 pb-24 slide-up-element delay-200 relative z-10">
      
      <div class="flex flex-col md:flex-row items-center gap-16 bg-white/70 backdrop-blur-2xl border border-zinc-100 rounded-[2.5rem] p-10 md:p-16 shadow-[0_20px_60px_rgba(0,0,0,0.05)]">
        
        <!-- Left Side: Form -->
        <div class="w-full md:w-1/2">
          <form id="loginForm" class="space-y-8" onsubmit="return handleLogin(event)">
            <!-- Inputs -->
            <div class="space-y-6">
              <div>
                <label class="block text-sm font-bold text-zinc-900 mb-2 pl-2">Email address</label>
                <input type="email" id="email" class="apple-input" placeholder="you@university.edu" required>
              </div>
              <div>
                <div class="flex justify-between mb-2 px-2">
                  <label class="block text-sm font-bold text-zinc-900">Password</label>
                  <a href="#" class="text-sm font-medium text-zinc-500 hover:text-zinc-900 transition-colors">Forgot password?</a>
                </div>
                <input type="password" id="password" class="apple-input" placeholder="••••••••" required>
              </div>
            </div>

            <div id="errorMsg" class="hidden text-sm font-medium text-red-600 bg-red-50 border border-red-200 rounded-xl px-6 py-3 text-center"></div>

            <!-- Buttons -->
            <div class="pt-4 space-y-4">
              <button type="submit" id="submitBtn" class="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-4 px-8 rounded-2xl transition-all duration-300 shadow-xl shadow-blue-600/20 hover:shadow-blue-600/30 flex justify-center items-center gap-2 group">
                Log In
                <i data-lucide="arrow-up-right" class="w-5 h-5 text-zinc-400 group-hover:text-white transition-colors"></i>
              </button>
              <a data-create-account href="<%= ctx %>/auth/signup.jsp" class="w-full block text-center bg-white border-2 border-zinc-200 hover:border-zinc-900 text-zinc-900 font-bold py-4 px-8 rounded-2xl transition-all duration-300">
                Create Account
              </a>
            </div>
          </form>
        </div>

        <!-- Right Side: Interactive Anteater -->
        <div class="w-full md:w-1/2 flex flex-col items-center justify-center relative">
          <!-- Subtle decorative background for the anteater -->
          <div class="absolute inset-0 bg-gradient-to-tr from-indigo-50 to-purple-50 rounded-[3rem] -z-10 blur-3xl opacity-50"></div>
          
          <div id="login-cat-container" class="cat-container w-full max-w-[300px] aspect-square flex items-center justify-center relative z-10 bg-white/40 backdrop-blur-md rounded-[3rem] shadow-[0_8px_32px_rgba(37,99,235,0.15)] border-4 border-white/70 overflow-hidden transition-all duration-500 hover:shadow-[0_12px_40px_rgba(37,99,235,0.25)] hover:border-white/90">
            <!-- Interactive anteater is rendered locally by JavaScript -->
          </div>
          <p class="text-zinc-400 font-medium text-sm mt-8 text-center">Your privacy is perfectly safe.</p>
        </div>
        
      </div>
    </div>
  </div>

  <script src="<%= ctx %>/static/js/auth-email.js"></script>
  <script src="<%= ctx %>/static/js/api.js"></script>
  <script src="<%= ctx %>/static/js/particles.js"></script>
  <script src="<%= ctx %>/static/js/typewriter.js"></script>
  <script src="<%= ctx %>/static/js/stitch-matrix.js?v=<%= System.currentTimeMillis() %>"></script>
  <script src="<%= ctx %>/static/js/anteater-anim.js?v=<%= System.currentTimeMillis() %>"></script>
  <script>
    lucide.createIcons();
    let mascotAnim = null;
    
    // Initialize interactive particles and typewriter
    window.addEventListener('DOMContentLoaded', () => {
      if(window.initParticles) {
        window.initParticles('auth-canvas');
      }
      if(window.initStitchMatrix) {
        window.initStitchMatrix('stitch-matrix-login', { avoidSelector: '.apple-input, button, a' });
      }
      if(window.initSimpleTypewriter) window.initSimpleTypewriter('typewriter-text', ["Log in.", "Welcome back.", "Continue."]);
      if(window.initLoginAnteater) {
        mascotAnim = window.initLoginAnteater('login-cat-container', '<%= ctx %>');
      }
    });

    // Trigger slide up animation
    setTimeout(() => {
      document.querySelectorAll('.slide-up-element').forEach(el => el.classList.add('visible'));
    }, 50);

    async function handleLogin(e) {
      e.preventDefault();
      const errDiv = document.getElementById('errorMsg');
      errDiv.classList.add('hidden');
      
      const submitBtn = document.getElementById('submitBtn');
      const originalText = submitBtn.innerHTML;
      submitBtn.innerHTML = '<i data-lucide="loader-2" class="h-6 w-6 animate-spin"></i> Logging in...';
      submitBtn.disabled = true;
      lucide.createIcons();

      const email = document.getElementById('email').value;
      const pwd = document.getElementById('password').value;
      const ctx = document.querySelector('meta[name="ctx"]').content;

      try {
        const data = await authApi.login(email, pwd);
        const role = data.user?.role;
        if (!['applicant','organiser','admin','reviewer','decision_maker','viewer'].includes(role)) {
          throw new Error('Your account role could not be determined. Please contact an admin.');
        }
        if(mascotAnim) mascotAnim.triggerSuccess();
        setTimeout(() => {
          window.location.href = ctx + (['reviewer','decision_maker','viewer'].includes(role)?'/review/dashboard.jsp':'/' + role + '/dashboard.jsp');
        }, 1000);
      } catch (err) {
        if(mascotAnim) mascotAnim.triggerFail();
        errDiv.textContent = err.message || 'Login failed.';
        errDiv.classList.remove('hidden');
        submitBtn.innerHTML = originalText;
        submitBtn.disabled = false;
        lucide.createIcons();
      }
      return false;
    }
  </script>
</body>
</html>
