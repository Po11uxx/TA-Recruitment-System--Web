/**
 * Stitch-inspired Dot Matrix Engine
 * Renders an undulating 3D wave of dots using procedural math, 
 * with a dynamic spotlight interaction that scales and colors the dots based on mouse proximity.
 */
class StitchMatrix {
  constructor(canvasId, options = {}) {
    this.canvas = document.getElementById(canvasId);
    if (!this.canvas) return;
    this.ctx = this.canvas.getContext('2d');
    
    // Configuration
    this.spacing = options.spacing || 22;      // Distance between dots
    this.baseRadius = options.baseRadius || 1.2; // Smallest dot size
    this.mouseRadius = options.mouseRadius || 350; // Size of the interactive spotlight
    
    // State
    this.mouse = { x: -1000, y: -1000, targetX: -1000, targetY: -1000 };
    this.time = 0;
    
    this.resize();
    
    // Event Listeners
    window.addEventListener('resize', () => this.resize());
    
    // Text Collision elements
    this.avoidSelector = options.avoidSelector || 'h1, h2, h3, h4, p, label, .input-pill, .avoid-matrix';
    this.textRects = [];
    
    // Update rects periodically to handle dynamic content or animations
    setInterval(() => this.updateTextRects(), 1000);
    
    // Track mouse globally to ensure smooth transition when entering/leaving sections
    window.addEventListener('mousemove', (e) => {
      const rect = this.canvas.getBoundingClientRect();
      // Only track if the canvas is currently visible/relevant, but for smoothness we track globally relative to the canvas rect
      this.mouse.targetX = e.clientX - rect.left;
      this.mouse.targetY = e.clientY - rect.top;
    });
    
    // Initial jump to mouse if it's already on screen (avoids dots flying in from corner)
    this.mouse.x = this.mouse.targetX;
    this.mouse.y = this.mouse.targetY;
    
    this.animate();
  }
  
  resize() {
    this.width = this.canvas.offsetWidth;
    this.height = this.canvas.offsetHeight;
    
    // Handle High DPI (Retina) displays for perfectly crisp circles
    const dpr = window.devicePixelRatio || 1;
    this.canvas.width = this.width * dpr;
    this.canvas.height = this.height * dpr;
    this.ctx.scale(dpr, dpr);
    
    this.updateTextRects();
  }
  
  updateTextRects() {
    this.textRects = [];
    const canvasRect = this.canvas.getBoundingClientRect();
    
    // We only want to avoid elements that are inside the same container as the canvas
    // Or we can just select globally and check bounds
    const elements = document.querySelectorAll(this.avoidSelector);
    
    elements.forEach(el => {
      const rect = el.getBoundingClientRect();
      if (rect.width > 0 && rect.height > 0) {
        this.textRects.push({
          left: rect.left - canvasRect.left - 15, // 15px padding
          right: rect.right - canvasRect.left + 15,
          top: rect.top - canvasRect.top - 15,
          bottom: rect.bottom - canvasRect.top + 15
        });
      }
    });
  }
  
  animate() {
    this.time += 0.015; // Speed of the background wave
    
    // Smooth mouse interpolation (easing)
    this.mouse.x += (this.mouse.targetX - this.mouse.x) * 0.12;
    this.mouse.y += (this.mouse.targetY - this.mouse.y) * 0.12;
    
    this.ctx.clearRect(0, 0, this.width, this.height);
    
    // Calculate grid dimensions
    const cols = Math.ceil(this.width / this.spacing) + 1;
    const rows = Math.ceil(this.height / this.spacing) + 1;
    
    for (let i = 0; i < cols; i++) {
      for (let j = 0; j < rows; j++) {
        const x = i * this.spacing;
        const y = j * this.spacing;
        
        // --- 1. Procedural Noise (The 3D Wave) ---
        // Combine multiple sine waves for an organic, non-repeating fluid look
        const wave1 = Math.sin((x * 0.004) + this.time * 1.2);
        const wave2 = Math.cos((y * 0.005) + this.time * 0.8);
        const wave3 = Math.sin((x + y) * 0.003 - this.time);
        
        // Normalize noise to value between 0 and 1
        const rawNoise = (wave1 + wave2 + wave3) / 3;
        const normalizedNoise = (rawNoise + 1) / 2;
        
        // --- 2. Mouse Interaction (The Spotlight) ---
        const dx = x - this.mouse.x;
        const dy = y - this.mouse.y;
        const dist = Math.sqrt(dx * dx + dy * dy);
        
        let mouseInfluence = 0;
        if (dist < this.mouseRadius) {
           // Smoothstep falloff for a soft spotlight edge
           const t = 1 - (dist / this.mouseRadius);
           mouseInfluence = t * t * (3 - 2 * t);
        }
        
        // --- Text Collision Detection ---
        let attenuation = 1.0;
        for (let k = 0; k < this.textRects.length; k++) {
          const tr = this.textRects[k];
          if (x > tr.left && x < tr.right && y > tr.top && y < tr.bottom) {
            attenuation = 0.15; // Drastically reduce visibility behind text
            mouseInfluence *= 0.1; // Prevent mouse spotlight from blinding text
            break;
          }
        }
        
        // --- 3. Compute Final Properties ---
        // Radius: Base size + Wave breathing + Mouse expansion
        let r = this.baseRadius + (normalizedNoise * 1.0) + (mouseInfluence * 3.5);
        r *= Math.max(0.3, attenuation); // Make dots smaller behind text
        
        // Alpha: Base alpha + Mouse glow
        const baseAlpha = 0.05 + (normalizedNoise * 0.12);
        let finalAlpha = Math.min(1, baseAlpha + mouseInfluence * 0.7);
        finalAlpha *= attenuation; // Dim opacity behind text
        
        // Color Interpolation:
        // Base color: deep blue (rgb(21, 52, 91))
        // Highlight color: blue-500 (rgb(59, 130, 246))
        const rVal = Math.round(21 + (59 - 21) * mouseInfluence);
        const gVal = Math.round(52 + (130 - 52) * mouseInfluence);
        const bVal = Math.round(91 + (246 - 91) * mouseInfluence);
        
        // --- 4. Draw ---
        this.ctx.fillStyle = `rgba(${rVal}, ${gVal}, ${bVal}, ${finalAlpha})`;
        
        this.ctx.beginPath();
        // Add a slight displacement based on the wave to create a 3D parallax feel
        const offsetX = x + (wave1 * 3);
        const offsetY = y + (wave2 * 3);
        
        this.ctx.arc(offsetX, offsetY, Math.max(0.1, r), 0, Math.PI * 2);
        this.ctx.fill();
      }
    }
    
    requestAnimationFrame(() => this.animate());
  }
}

// Global initializer
window.initStitchMatrix = function(canvasId, options = {}) {
  new StitchMatrix(canvasId, options);
};
