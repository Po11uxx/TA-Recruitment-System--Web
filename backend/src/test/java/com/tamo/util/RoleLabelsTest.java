package com.tamo.util;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RoleLabelsTest {
 @Test void mainRolesKeepCompatibleKeysAndExactDisplayNames(){assertEquals("Applicant",RoleLabels.display("applicant"));assertEquals("Teacher",RoleLabels.display("organiser"));assertEquals("admin",RoleLabels.display("admin"));}
 @Test void advancedRolesAreExplicitAndUnknownInputIsSafe(){for(String role:new String[]{"reviewer","decision_maker","viewer"})assertTrue(RoleLabels.display(role).startsWith("Advanced review role: "));assertEquals("Unknown role",RoleLabels.display(null));assertEquals("Unknown role",RoleLabels.display("<script>"));}
}
