// Presentation follows the browser/device zone; stored instants and server expiry rules stay unchanged.
window.AssignmentTime = (() => {
  const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone, year:'numeric', month:'short', day:'numeric',
    hour:'2-digit', minute:'2-digit', hourCycle:'h23', timeZoneName:'short'
  });
  return {
    timeZone,
    format(value) {
      if (!value) return 'Not recorded';
      const date = new Date(value);
      return Number.isNaN(date.getTime()) ? 'Not recorded' : formatter.format(date);
    }
  };
})();
