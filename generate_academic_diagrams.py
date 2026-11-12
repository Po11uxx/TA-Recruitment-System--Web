from PIL import Image, ImageDraw, ImageFont
from pathlib import Path
import math

out = Path('report_assets')
out.mkdir(exist_ok=True)

try:
    font_bold = ImageFont.truetype('/System/Library/Fonts/Supplemental/Times New Roman Bold.ttf', 20)
    font = ImageFont.truetype('/System/Library/Fonts/Supplemental/Times New Roman.ttf', 18)
    font_small = ImageFont.truetype('/System/Library/Fonts/Supplemental/Times New Roman.ttf', 16)
    font_tiny = ImageFont.truetype('/System/Library/Fonts/Supplemental/Times New Roman.ttf', 14)
except Exception:
    font_bold = font = font_small = font_tiny = ImageFont.load_default()

BG = '#FFFFFF'
FG = '#000000'
GRAY = '#E5E5E5'

def draw_rect(d, x, y, w, h, fill=BG, outline=FG, width=2):
    d.rectangle([x, y, x+w, y+h], fill=fill, outline=outline, width=width)

def draw_text(d, x, y, text, font, fill=FG, align='left'):
    d.text((x, y), text, font=font, fill=fill, align=align)

def text_bbox(d, text, font):
    return d.textbbox((0,0), text, font=font)

def draw_arrow(d, p1, p2, fill=FG, width=2, arrow_size=10):
    d.line([p1, p2], fill=fill, width=width)
    ang = math.atan2(p2[1]-p1[1], p2[0]-p1[0])
    pts = [
        p2,
        (p2[0] - arrow_size * math.cos(ang - math.pi/6), p2[1] - arrow_size * math.sin(ang - math.pi/6)),
        (p2[0] - arrow_size * math.cos(ang + math.pi/6), p2[1] - arrow_size * math.sin(ang + math.pi/6))
    ]
    d.polygon(pts, fill=fill)

def draw_dashed_arrow(d, p1, p2, fill=FG, width=2, dash_len=8, arrow_size=10):
    dist = math.hypot(p2[0]-p1[0], p2[1]-p1[1])
    ang = math.atan2(p2[1]-p1[1], p2[0]-p1[0])
    curr = 0
    while curr < dist:
        end = min(curr + dash_len, dist)
        sp = (p1[0] + curr*math.cos(ang), p1[1] + curr*math.sin(ang))
        ep = (p1[0] + end*math.cos(ang), p1[1] + end*math.sin(ang))
        d.line([sp, ep], fill=fill, width=width)
        curr += dash_len * 2
    
    pts = [
        p2,
        (p2[0] - arrow_size * math.cos(ang - math.pi/6), p2[1] - arrow_size * math.sin(ang - math.pi/6)),
        (p2[0] - arrow_size * math.cos(ang + math.pi/6), p2[1] - arrow_size * math.sin(ang + math.pi/6))
    ]
    d.line([pts[1], pts[0], pts[2]], fill=fill, width=width)

def generate_architecture():
    W, H = 1400, 850
    img = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(img)
    
    # Client Node
    draw_rect(d, 100, 50, 350, 600)
    draw_text(d, 110, 60, "<<node>>\nClient Browser", font_bold)
    
    draw_rect(d, 130, 130, 290, 120)
    draw_text(d, 140, 140, "<<component>>\nJSP UI View", font_bold)
    draw_text(d, 140, 190, "- Tailwind CSS\n- Vanilla JS", font)
    
    draw_rect(d, 130, 330, 290, 120)
    draw_text(d, 140, 340, "<<component>>\nFetch API Client", font_bold)
    draw_text(d, 140, 390, "- api.js\n- Async requests", font)
    
    draw_arrow(d, (275, 250), (275, 330))
    
    # Server Node
    draw_rect(d, 600, 50, 700, 720)
    draw_text(d, 610, 60, "<<node>>\nEmbedded Tomcat 10.1 (Servlet Container)", font_bold)
    
    # Filter
    draw_rect(d, 650, 130, 600, 80)
    draw_text(d, 660, 140, "<<component>>\nAuthFilter (RBAC & Session Management)", font_bold)
    
    # Controllers
    draw_rect(d, 650, 260, 600, 120)
    draw_text(d, 660, 270, "<<component>>\nREST Controllers (Servlets)", font_bold)
    draw_text(d, 660, 320, "- ApplicantServlet\n- OrganiserServlet\n- AdminServlet", font)
    
    # Services
    draw_rect(d, 650, 430, 600, 120)
    draw_text(d, 660, 440, "<<component>>\nBusiness Services", font_bold)
    draw_text(d, 660, 490, "- ApplicantService    - OrganiserService\n- AdminService        - MessageService", font)
    
    # Persistence
    draw_rect(d, 650, 600, 600, 120)
    draw_text(d, 660, 610, "<<component>>\nData Access Layer", font_bold)
    draw_text(d, 660, 660, "- JsonDataService (Singleton)\n- Gson Serialization", font)
    
    # Connections
    draw_dashed_arrow(d, (420, 390), (600, 390))
    draw_text(d, 460, 365, "HTTP/JSON", font)
    
    draw_dashed_arrow(d, (950, 210), (950, 260))
    draw_dashed_arrow(d, (950, 380), (950, 430))
    draw_dashed_arrow(d, (950, 550), (950, 600))
    
    # File storage
    draw_rect(d, 650, 770, 200, 60)
    draw_text(d, 660, 785, "<<file>> data.json", font_bold)
    draw_dashed_arrow(d, (750, 720), (750, 770))
    
    img.save(out / 'diagram_arch.png', quality=95)

def generate_domain():
    W, H = 1400, 900
    img = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(img)
    
    def uml_class(x, y, w, name, attrs, methods=[]):
        ah = len(attrs) * 22 + 10
        mh = len(methods) * 22 + 10
        th = 40
        h = th + ah + (mh if methods else 0)
        draw_rect(d, x, y, w, h)
        draw_rect(d, x, y, w, th)
        draw_text(d, x + w/2, y + 10, name, font_bold, align='center')
        
        yy = y + th + 5
        for a in attrs:
            draw_text(d, x + 10, yy, a, font)
            yy += 22
        
        if methods:
            d.line([x, y + th + ah, x + w, y + th + ah], fill=FG, width=2)
            yy = y + th + ah + 5
            for m in methods:
                draw_text(d, x + 10, yy, m, font)
                yy += 22
        return (x+w/2, y, x+w/2, y+h, x, y+h/2, x+w, y+h/2)
    
    # Entities
    u_c = uml_class(100, 50, 250, "User", ["- id: String", "- email: String", "- role: RoleEnum", "- department: String"])
    p_c = uml_class(100, 300, 300, "Profile", ["- studentId: String", "- gpa: Double", "- degree: String", "- skills: List<String>", "- documents: List<Document>", "- experience: List<Experience>"])
    j_c = uml_class(600, 50, 280, "Job", ["- id: String", "- moduleCode: String", "- title: String", "- positions: int", "- workDates: List<String>", "- status: JobStatus"])
    a_c = uml_class(600, 380, 280, "Application", ["- id: String", "- status: AppStatus", "- appliedAt: Date", "- selectedDocIds: List<String>"])
    i_c = uml_class(1050, 380, 250, "Interview", ["- id: String", "- date: String", "- startTime: String", "- type: String"])
    m_c = uml_class(1050, 50, 250, "Module", ["- code: String", "- name: String", "- department: String", "- term: String"])
    
    def ortho_assoc(pts, label, mult1, mult2, lbl_offset=(0,0), m1_offset=(0,0), m2_offset=(0,0)):
        for i in range(len(pts)-1):
            d.line([pts[i], pts[i+1]], fill=FG, width=2)
        
        # label placed near middle segment
        mid_idx = len(pts) // 2
        pA, pB = pts[mid_idx-1], pts[mid_idx]
        draw_text(d, (pA[0]+pB[0])/2 + lbl_offset[0], (pA[1]+pB[1])/2 + lbl_offset[1], label, font_small)
        
        # mults placed near start/end
        draw_text(d, pts[0][0] + m1_offset[0], pts[0][1] + m1_offset[1], mult1, font_small)
        draw_text(d, pts[-1][0] + m2_offset[0], pts[-1][1] + m2_offset[1], mult2, font_small)

    # User (bottom) -> Profile (top)
    ortho_assoc([(u_c[2], u_c[3]), (u_c[2], (u_c[3]+p_c[1])/2), (p_c[0], (u_c[3]+p_c[1])/2), (p_c[0], p_c[1])], 
                "owns", "1", "0..1", lbl_offset=(10, -10), m1_offset=(5, 5), m2_offset=(5, -20))
                
    # User (right) -> Job (left)
    ortho_assoc([(u_c[6], u_c[7]-20), (j_c[4], j_c[5]-20)], 
                "creates", "1", "0..*", lbl_offset=(0, -20), m1_offset=(10, -25), m2_offset=(-30, -25))
                
    # Module (left) -> Job (right)
    ortho_assoc([(m_c[4], m_c[5]), (j_c[6], j_c[7])], 
                "advertises", "1", "0..*", lbl_offset=(0, -20), m1_offset=(-20, -20), m2_offset=(10, -20))
                
    # Job (bottom) -> Application (top)
    ortho_assoc([(j_c[2], j_c[3]), (a_c[0], a_c[1])], 
                "receives", "1", "0..*", lbl_offset=(10, -10), m1_offset=(5, 5), m2_offset=(5, -20))
                
    # User (right) -> Application (left)
    ortho_assoc([(u_c[6], u_c[7]+20), (500, u_c[7]+20), (500, a_c[5]), (a_c[4], a_c[5])],
                "submits", "1", "0..*", lbl_offset=(10, 0), m1_offset=(10, -20), m2_offset=(-30, -20))
                
    # Application (right) -> Interview (left)
    ortho_assoc([(a_c[6], a_c[7]), (i_c[4], i_c[5])], 
                "results in", "1", "0..1", lbl_offset=(0, -20), m1_offset=(10, -20), m2_offset=(-30, -20))

    img.save(out / 'diagram_domain.png', quality=95)

def generate_workflow():
    W, H = 1400, 850
    img = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(img)
    
    lanes = [("Organiser", 100, 400), ("System", 400, 700), ("Applicant", 700, 1000)]
    for name, lx1, lx2 in lanes:
        draw_rect(d, lx1, 20, lx2-lx1, 800)
        draw_text(d, lx1 + (lx2-lx1)/2 - 50, 35, name, font_bold)
        d.line([lx1, 70, lx2, 70], fill=FG, width=2)
    
    def action(x, y, text):
        w, h = 220, 60
        d.rounded_rectangle([x, y, x+w, y+h], radius=15, fill=BG, outline=FG, width=2)
        lines = text.split('\n')
        yy = y + h/2 - len(lines)*10
        for line in lines:
            tw = text_bbox(d, line, font)[2]
            draw_text(d, x + w/2 - tw/2, yy, line, font)
            yy += 20
        return (x+w/2, y, x+w/2, y+h, x, y+h/2, x+w, y+h/2)
    
    d.ellipse([235, 90, 265, 120], fill=FG)
    draw_arrow(d, (250, 120), (250, 160))
    
    a1 = action(140, 160, "Create Job Posting\n(Set Work Dates)")
    draw_arrow(d, (a1[6], a1[5]), (440, a1[5]))
    
    a2 = action(440, 160, "Validate & Persist Job")
    draw_arrow(d, (a2[0], a2[1]), (a2[0], 260))
    
    a3 = action(440, 260, "Publish Job")
    draw_arrow(d, (a3[6], a3[5]), (740, a3[5]))
    
    a4 = action(740, 245, "Browse Jobs &\nView Schedules")
    draw_arrow(d, (a4[0], a4[1]), (a4[0], 350))
    
    a5 = action(740, 350, "Submit Application\n(Select Profile/Docs)")
    draw_arrow(d, (a5[4], a5[5]), (660, a5[5]))
    
    a6 = action(440, 350, "Verify RBAC &\nDuplicate Check")
    draw_arrow(d, (a6[4], a6[5]), (360, a6[5]))
    
    a7 = action(140, 350, "Review Kanban\nPipeline")
    draw_arrow(d, (a7[0], a7[1]), (a7[0], 460))
    
    dx, dy = 250, 490
    pts = [(dx, dy-30), (dx+50, dy), (dx, dy+30), (dx-50, dy)]
    d.polygon(pts, outline=FG, fill=BG, width=2)
    draw_text(d, dx-20, dy-10, "Offer?", font)
    
    draw_arrow(d, (250, 520), (250, 580))
    draw_text(d, 260, 535, "[Yes]", font)
    
    draw_arrow(d, (300, 490), (440, 490))
    draw_text(d, 330, 465, "[No]", font)
    
    a8 = action(140, 580, "Issue Offer &\nSchedule Details")
    draw_arrow(d, (a8[6], a8[5]), (440, a8[5]))
    
    a9 = action(440, 580, "Update Status\n(Persist to JSON)")
    draw_arrow(d, (a9[6], a9[5]), (740, a9[5]))
    
    a10 = action(740, 580, "View Offer &\nWork Calendar")
    
    a11 = action(440, 460, "Update Status\n(Rejected)")
    
    d.ellipse([835, 700, 865, 730], outline=FG, width=2)
    d.ellipse([842, 707, 858, 723], fill=FG)
    draw_arrow(d, (850, 640), (850, 700))
    
    img.save(out / 'diagram_workflow.png', quality=95)

def generate_quality():
    W, H = 1400, 750
    img = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(img)
    
    headers = ["Quality Attribute", "Architectural Tactic / Mechanism", "Implementation Evidence"]
    rows = [
        ("Functional Suitability", "Role-specific APIs & Workflows", "Distinct Servlets (Applicant, Organiser, Admin)\nFull Kanban application pipeline"),
        ("Security (Authorization)", "Role-Based Access Control (RBAC)", "AuthFilter validating HttpSession attributes\nServer-side object ownership checks"),
        ("Maintainability", "Separation of Concerns (MVC)", "JSP (View), Servlet (Controller), Service (Business)\nSingle JsonDataService for data access"),
        ("Usability", "Defensive UI & Inline Feedback", "Client-side validation, Toast notifications\nPre-filled forms for application submission"),
        ("Reliability", "Idempotency & Data Integrity", "Duplicate application prevention in ApplicantService\nCascade deletion warnings in Admin editor"),
        ("Portability", "Container-agnostic Execution", "Embedded Tomcat via Maven exec:java\nZero external DB dependencies (data.json)")
    ]
    
    x, y = 80, 50
    col_w = [300, 450, 500]
    rh = 80
    
    for i, h in enumerate(headers):
        draw_rect(d, x + sum(col_w[:i]), y, col_w[i], 40, fill=GRAY)
        draw_text(d, x + sum(col_w[:i]) + 10, y + 10, h, font_bold)
    
    y += 40
    for r in rows:
        for i, text in enumerate(r):
            draw_rect(d, x + sum(col_w[:i]), y, col_w[i], rh)
            draw_text(d, x + sum(col_w[:i]) + 10, y + 15, text, font)
        y += rh
        
    img.save(out / 'quality_coverage.png', quality=95)

def generate_persistence():
    W, H = 1000, 650
    img = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(img)
    
    def actor(x, name):
        draw_rect(d, x-70, 20, 140, 50)
        draw_text(d, x-50, 35, name, font_bold)
        d.line([x, 70, x, 600], fill=FG, width=1) # Lifeline
        return x

    c = actor(200, ":Controller")
    s = actor(500, ":Service")
    j = actor(800, ":JsonDataService")
    
    def msg(y, p1, p2, text, dashed=False):
        if dashed:
            draw_dashed_arrow(d, (p1, y), (p2, y))
        else:
            draw_arrow(d, (p1, y), (p2, y))
        draw_text(d, min(p1,p2)+10, y-25, text, font_small)
        return y+60

    y = 120
    y = msg(y, c, s, "1: updateProfile(updates)")
    draw_rect(d, s-10, 110, 20, 400)
    
    y = msg(y, s, j, "2: getData()")
    draw_rect(d, j-10, y-50, 20, 80)
    y = msg(y, j, s, "3: return DataStore", dashed=True)
    
    d.line([s+10, y-20, s+60, y-20, s+60, y+10, s+10, y+10], fill=FG, width=2)
    draw_arrow(d, (s+60, y+10), (s+10, y+10))
    draw_text(d, s+20, y-40, "4: mutate objects", font_small)
    y += 60
    
    y = msg(y, s, j, "5: save()")
    draw_rect(d, j-10, y-50, 20, 160)
    
    d.line([j+10, y-20, j+60, y-20, j+60, y+10, j+10, y+10], fill=FG, width=2)
    draw_arrow(d, (j+60, y+10), (j+10, y+10))
    draw_text(d, j+20, y-40, "6: acquire lock", font_small)
    y += 60
    
    d.line([j+10, y-20, j+60, y-20, j+60, y+10, j+10, y+10], fill=FG, width=2)
    draw_arrow(d, (j+60, y+10), (j+10, y+10))
    draw_text(d, j+20, y-40, "7: Gson.toJson(file)", font_small)
    y += 60
    
    y = msg(y, j, s, "8: return void", dashed=True)
    y = msg(y, s, c, "9: Response OK", dashed=True)
    
    img.save(out / 'diagram_persistence.png', quality=95)

def generate_rbac():
    W, H = 800, 300
    img = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(img)
    
    draw_rect(d, 50, 100, 140, 60, fill=BG)
    draw_text(d, 60, 120, "HTTP Request", font)
    
    draw_arrow(d, (190, 130), (250, 130))
    
    pts = [(330, 70), (410, 130), (330, 190), (250, 130)]
    d.polygon(pts, outline=FG, fill=BG, width=2)
    draw_text(d, 275, 120, "AuthFilter:\nRole match?", font)
    
    draw_arrow(d, (330, 70), (330, 40))
    d.line([(330, 40), (600, 40)], fill=FG, width=2)
    draw_arrow(d, (600, 40), (600, 70))
    draw_text(d, 350, 15, "No Match / No Session", font)
    draw_rect(d, 530, 70, 140, 40, fill=BG)
    draw_text(d, 550, 80, "401 / 403 Error", font)
    
    draw_arrow(d, (410, 130), (530, 130))
    draw_text(d, 450, 105, "Yes", font)
    
    draw_rect(d, 530, 110, 140, 40, fill=BG)
    draw_text(d, 545, 120, "Execute Servlet", font)
    
    img.save(out / 'diagram_rbac.png', quality=95)

generate_architecture()
generate_domain()
generate_workflow()
generate_quality()
generate_persistence()
generate_rbac()

print("Generated all academic diagrams.")
