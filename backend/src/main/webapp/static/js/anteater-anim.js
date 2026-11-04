/** Local SVG mascot for authentication forms; no external animation runtime. */
class LoginAnteater {
  constructor(containerId) {
    this.container = document.getElementById(containerId);
    if (!this.container) return;
    this.container.innerHTML = `
      <svg class="auth-anteater" viewBox="0 0 300 300" role="img" aria-label="Friendly anteater mascot">
        <style>
          .auth-anteater { width:100%; height:100%; --scarf:#2563eb; }
          .auth-anteater .mascot-body { animation:anteater-breathe 4s ease-in-out infinite; transform-origin:150px 250px; }
          .auth-anteater .mascot-pupil { transition:transform .2s ease-out; }
          .auth-anteater .mascot-paw { transition:transform .45s cubic-bezier(.2,.8,.2,1); }
          .auth-anteater .mascot-eye { transform-box:fill-box; transform-origin:center; animation:anteater-blink 6s infinite; }
          .auth-anteater.is-private .mascot-paw-left { transform:translate(9px,-81px) rotate(-4deg); }
          .auth-anteater.is-private .mascot-paw-right { transform:translate(-10px,-91px) rotate(6deg); }
          .auth-anteater.is-private .mascot-eye { animation:none; }
          .auth-anteater.is-success .mascot-body { animation:anteater-cheer .6s ease-in-out; }
          .auth-anteater.is-error .mascot-body { animation:anteater-shake .4s ease-in-out; }
          @keyframes anteater-breathe { 50% { transform:translateY(-3px) rotate(-1deg); } }
          @keyframes anteater-blink { 0%,43%,47%,100% { transform:scaleY(1); } 45% { transform:scaleY(.08); } }
          @keyframes anteater-cheer { 35% { transform:translateY(-12px) rotate(-3deg); } 70% { transform:translateY(-5px) rotate(3deg); } }
          @keyframes anteater-shake { 25%,75% { transform:translateX(-5px); } 50% { transform:translateX(5px); } }
          @media(prefers-reduced-motion:reduce) {
            .auth-anteater *, .auth-anteater.is-success .mascot-body, .auth-anteater.is-error .mascot-body { animation:none!important; transition:none!important; }
          }
        </style>
        <circle cx="150" cy="150" r="135" fill="#eff6ff"/>
        <circle cx="231" cy="75" r="24" fill="#dbeafe"/>
        <circle cx="54" cy="109" r="7" fill="#bfdbfe"/>
        <path d="M239 213v12m-6-6h12M62 65v10m-5-5h10" stroke="#93c5fd" stroke-width="3" stroke-linecap="round"/>
        <ellipse cx="147" cy="270" rx="89" ry="10" fill="#bfdbfe" opacity=".5"/>
        <g class="mascot-body">
          <!-- Large bushy tail and the giant anteater's distinctive dark shoulder stripe. -->
          <path d="M112 240C65 268 26 237 24 195L37 204 29 176 45 187 41 159C74 170 92 185 112 211Z" fill="#344963"/>
          <path d="M43 202Q57 234 90 241" fill="none" stroke="#526b88" stroke-width="5" stroke-linecap="round"/>
          <path d="M94 180C79 205 78 240 100 259Q145 278 182 255C199 226 180 186 162 171Z" fill="#c5d2df"/>
          <path d="M95 181L133 230 170 182 155 168 126 198 111 174Z" fill="#15345b" stroke="#f4f0e7" stroke-width="6" stroke-linejoin="round"/>
          <ellipse cx="112" cy="259" rx="24" ry="12" fill="#344963"/>
          <ellipse cx="169" cy="259" rx="24" ry="12" fill="#344963"/>
          <ellipse cx="109" cy="92" rx="17" ry="22" transform="rotate(-18 109 92)" fill="#a9bacb" stroke="#344963" stroke-width="3"/>
          <ellipse cx="109" cy="94" rx="8" ry="12" transform="rotate(-18 109 94)" fill="#e5c2b8"/>
          <ellipse cx="155" cy="91" rx="14" ry="19" transform="rotate(14 155 91)" fill="#a9bacb" stroke="#344963" stroke-width="3"/>
          <ellipse cx="155" cy="92" rx="6" ry="10" fill="#e5c2b8"/>
          <!-- A long, tapering snout gives the mascot its anteater silhouette. -->
          <path d="M89 134C87 100 115 88 143 99C165 105 166 124 184 136L249 164Q261 170 250 179L181 163C159 182 125 183 104 165Q91 155 89 134Z" fill="#d9e2e9" stroke="#344963" stroke-width="3" stroke-linejoin="round"/>
          <path d="M163 144L247 170 184 157Q169 153 163 144" fill="#f4f0e7"/>
          <ellipse cx="250" cy="170" rx="9" ry="6" transform="rotate(17 250 170)" fill="#15345b"/>
          <path d="M207 169Q220 177 232 175" fill="none" stroke="#526b88" stroke-width="2" stroke-linecap="round"/>
          <g class="mascot-eye">
            <ellipse cx="138" cy="128" rx="12" ry="14" fill="white"/>
            <g class="mascot-pupil"><ellipse cx="140" cy="130" rx="6" ry="8" fill="#15345b"/><circle cx="142" cy="127" r="2.5" fill="white"/></g>
          </g>
          <path d="M127 108Q137 103 147 109" fill="none" stroke="#526b88" stroke-width="3" stroke-linecap="round"/>
          <ellipse cx="153" cy="147" rx="9" ry="5" fill="#e7b6b0" opacity=".6"/>
          <path d="M101 169Q133 186 165 169L169 181Q138 199 98 182Z" fill="var(--scarf)"/>
          <path d="M151 185L174 207 157 215 139 191Z" fill="var(--scarf)"/>
          <path d="M157 199L166 207" stroke="#93c5fd" stroke-width="3"/>
          <g class="mascot-paw mascot-paw-left">
            <ellipse cx="119" cy="214" rx="18" ry="23" transform="rotate(-16 119 214)" fill="#a9bacb" stroke="#344963" stroke-width="2.5"/>
            <path d="M108 226l-2 7m10-5v7m8-7 2 7" stroke="#f4f0e7" stroke-width="3" stroke-linecap="round"/>
          </g>
          <g class="mascot-paw mascot-paw-right">
            <ellipse cx="161" cy="215" rx="18" ry="23" transform="rotate(16 161 215)" fill="#a9bacb" stroke="#344963" stroke-width="2.5"/>
            <path d="M152 228l-2 7m10-6v7m8-9 2 7" stroke="#f4f0e7" stroke-width="3" stroke-linecap="round"/>
          </g>
        </g>
      </svg>`;
    this.svg = this.container.querySelector('svg');
    this.pupil = this.container.querySelector('.mascot-pupil');
    document.querySelectorAll('input[type="email"], input[type="text"]').forEach(input => {
      input.addEventListener('focus', () => this.track(input));
      input.addEventListener('input', () => this.track(input));
      input.addEventListener('blur', () => { this.pupil.style.transform = ''; });
    });
    document.querySelectorAll('input[type="password"]').forEach(input => {
      input.addEventListener('focus', () => this.svg.classList.add('is-private'));
      input.addEventListener('blur', () => this.svg.classList.remove('is-private'));
    });
  }

  track(input) {
    const offset = Math.min(input.value.length, 30) / 30 * 8 - 4;
    this.pupil.style.transform = `translate(${offset}px, 2px)`;
  }

  react(state) {
    this.svg.classList.remove('is-success', 'is-error');
    clearTimeout(this.reactionTimer);
    this.svg.classList.add(state);
    this.reactionTimer = setTimeout(() => this.svg.classList.remove(state), 700);
  }

  triggerSuccess() { this.react('is-success'); }
  triggerFail() { this.react('is-error'); }
  setRoleColor(role) {
    this.svg.style.setProperty('--scarf', { applicant: '#2563eb', organiser: '#0284c7', admin: '#1e40af' }[role] || '#2563eb');
  }
}

window.initLoginAnteater = containerId => new LoginAnteater(containerId);
