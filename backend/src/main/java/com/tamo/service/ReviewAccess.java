package com.tamo.service;
import com.tamo.model.*;
import java.util.*;
public class ReviewAccess{
 public static final String CONFIGURE_COURSE = "course.configure";
 public static final String MANAGE_ASSIGNMENTS = "assignment.manage";
 public static final Set<String> PERMISSIONS=Set.of("review.read","review.note","review.recommend","review.approve");
 public static final Set<String> ADMIN_DELEGATIONS=Set.of("review.note","review.recommend","review.approve",CONFIGURE_COURSE,MANAGE_ASSIGNMENTS);
 public static final Set<String> ROLES=Set.of("applicant","organiser","admin","reviewer","decision_maker","viewer");
 public static Set<String> defaults(String role){return switch(role==null?"":role){case "admin"->Set.of("review.read");case "organiser"->PERMISSIONS;case "reviewer"->Set.of("review.read","review.note","review.recommend");case "decision_maker"->Set.of("review.read","review.note","review.approve");case "viewer"->Set.of("review.read");default->Set.of();};}
 public static User user(DataStore ds,String id){return ds.getUsers().stream().filter(u->Objects.equals(id,u.getId())&&"active".equals(u.getStatus())).findFirst().orElseThrow(()->new QualificationService.Problem(403,"An active staff account is required."));}
 public static boolean can(User u,String permission){if("admin".equals(u.getRole())&&"review.read".equals(permission))return true;if("applicant".equals(u.getRole()))return false;return (u.getPermissions()==null?defaults(u.getRole()):u.getPermissions()).contains(permission);}
 public static boolean course(User u,com.tamo.model.Module m){return "admin".equals(u.getRole())||("organiser".equals(u.getRole())?Objects.equals(u.getId(),m.getOrganiserId()):u.getCourseIds()!=null&&u.getCourseIds().contains(m.getId()));}
 public static boolean canWrite(User user,com.tamo.model.Module module,String permission){
  if(module==null || !course(user,module)) return false;
  if("admin".equals(user.getRole())){
   return ADMIN_DELEGATIONS.contains(permission) && can(user,permission)
       && user.getCourseIds()!=null && user.getCourseIds().contains(module.getId());
  }
  if(CONFIGURE_COURSE.equals(permission)) return "organiser".equals(user.getRole());
  if(MANAGE_ASSIGNMENTS.equals(permission)) return "organiser".equals(user.getRole()) && can(user,"review.read");
  return can(user,permission);
 }
 public static com.tamo.model.Module module(DataStore ds,Job job){
  return ds.getModules().stream().filter(m->Objects.equals(m.getId(),job.getModuleId())).findFirst().orElse(null);
 }
 public static String currentOrganiser(DataStore ds,Job job){return job==null?null:ds.getModules().stream().filter(m->Objects.equals(m.getId(),job.getModuleId())).map(com.tamo.model.Module::getOrganiserId).filter(Objects::nonNull).findFirst().orElse(null);}
 public static boolean organiserOwns(DataStore ds,String id,Job job){return id!=null&&Objects.equals(id,currentOrganiser(ds,job))&&ds.getUsers().stream().anyMatch(u->Objects.equals(id,u.getId())&&"organiser".equals(u.getRole())&&"active".equals(u.getStatus()));}
}
