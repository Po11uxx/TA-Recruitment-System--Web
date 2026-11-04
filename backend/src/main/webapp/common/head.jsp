<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<script src="https://cdn.tailwindcss.com"></script>
<script src="https://unpkg.com/lucide@latest"></script>
<script src="https://unpkg.com/mammoth@1.8.0/mammoth.browser.min.js"></script>
<link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/custom.css?v=<%= System.currentTimeMillis() %>">
<script>
// Shared brand palette for public pages and every authenticated workspace.
const brandBlue = {
  50: '#eff6ff', 100: '#dbeafe', 200: '#bfdbfe', 300: '#93c5fd',
  400: '#60a5fa', 500: '#3b82f6', 600: '#2563eb', 700: '#1d4ed8',
  800: '#1e40af', 900: '#1e3a8a', 950: '#0b1f4a'
};
tailwind.config = {
  theme: {
    extend: {
      fontFamily: { sans: ['Inter', 'system-ui', 'sans-serif'] },
      colors: {
        zinc: {
          50: '#f4f8ff', 100: '#eaf2ff', 200: '#d5e3f5', 300: '#b5c9e3',
          400: '#7c9bbf', 500: '#526f93', 600: '#405d80', 700: '#2e4b70',
          800: '#1c3d68', 900: '#15345b', 950: '#0b1f4a'
        },
        indigo: brandBlue, violet: brandBlue, purple: brandBlue
      }
    }
  }
}
</script>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">

<script>window.roleDisplayLabels = <%= com.tamo.util.JsonUtil.getGson().toJson(java.util.Map.of("applicant",com.tamo.util.RoleLabels.display("applicant"),"organiser",com.tamo.util.RoleLabels.display("organiser"),"admin",com.tamo.util.RoleLabels.display("admin"),"reviewer",com.tamo.util.RoleLabels.display("reviewer"),"decision_maker",com.tamo.util.RoleLabels.display("decision_maker"),"viewer",com.tamo.util.RoleLabels.display("viewer"))) %>; window.displayRole = role => window.roleDisplayLabels[role] || "Unknown role";</script>
