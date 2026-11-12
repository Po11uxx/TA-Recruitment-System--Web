package com.tamo.util;
/** Display labels only; authorization continues to use the stored role keys. */
public final class RoleLabels {
 private RoleLabels() {}
 public static String display(String role) {
  if(role==null)return "Unknown role";
  return switch(role){case "applicant"->"Applicant";case "organiser"->"Teacher";case "admin"->"admin";case "reviewer"->"Advanced review role: Reviewer";case "decision_maker"->"Advanced review role: Decision-maker";case "viewer"->"Advanced review role: Viewer";default->"Unknown role";};
 }
}
