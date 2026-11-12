package com.tamo.servlet;
import com.google.gson.*;
import com.tamo.service.*;
import com.tamo.model.*;
import com.tamo.util.JsonUtil;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
public class ReviewServlet extends HttpServlet{
 private JsonDataService data;@Override public void init(){data=JsonDataService.getInstance();}
 private String user(HttpServletRequest r){HttpSession s=r.getSession(false);if(s==null||s.getAttribute("userId")==null)throw new QualificationService.Problem(401,"Not logged in.");return (String)s.getAttribute("userId");}
 private void admin(String id){if(!"admin".equals(ReviewAccess.user(data.getData(),id).getRole()))throw new QualificationService.Problem(403,"Administrator access required.");}
 private JsonObject settings(String actor){JsonObject result=new JsonObject();result.addProperty("actorId",actor);JsonArray users=new JsonArray();for(User u:data.getData().getUsers())if(!"applicant".equals(u.getRole())){JsonObject r=new JsonObject();r.addProperty("id",u.getId());r.addProperty("name",u.getName());r.addProperty("role",u.getRole());r.add("permissions",JsonUtil.getGson().toJsonTree(u.getPermissions()==null?ReviewAccess.defaults(u.getRole()):u.getPermissions()));r.add("courseIds",JsonUtil.getGson().toJsonTree(u.getCourseIds()==null?List.of():u.getCourseIds()));users.add(r);}result.add("users",users);result.add("modules",JsonUtil.getGson().toJsonTree(data.getData().getModules()));return result;}
 @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws IOException{try{String id=user(req);if(req.getServletPath().contains("review-access")){admin(id);JsonUtil.writeJson(resp,settings(id));return;}Map<String,String> f=new HashMap<>();req.getParameterMap().forEach((k,v)->f.put(k,v[0]));JsonUtil.writeJson(resp,new ReviewService(data).list(id,f));}catch(QualificationService.Problem e){JsonUtil.writeError(resp,e.status,e.getMessage());}}
 @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{try{String id=user(req);String path=JsonUtil.getPathInfo(req);if(path.matches("[\\w]+/select-ta")){JsonUtil.writeJson(resp,new TeacherSelectionService(data).select(id,path.split("/")[0],JsonUtil.readRequestBodyAsJson(req)));return;}if(!path.matches("[\\w]+/records")){JsonUtil.writeError(resp,404,"Endpoint not found.");return;}JsonUtil.writeJson(resp,new ReviewService(data).record(id,path.split("/")[0],JsonUtil.readRequestBodyAsJson(req)));}catch(QualificationService.Problem e){JsonUtil.writeError(resp,e.status,e.getMessage());}catch(JsonParseException e){JsonUtil.writeError(resp,400,"Invalid JSON.");}}
 @Override protected void doPut(HttpServletRequest req,HttpServletResponse resp)throws IOException {
  try {
   String actor=user(req);admin(actor);
   if(!req.getServletPath().contains("review-access"))throw new QualificationService.Problem(404,"Endpoint not found.");
   String id=JsonUtil.getPathInfo(req);
   User target=data.getData().getUsers().stream().filter(u->id.equals(u.getId())).findFirst()
       .orElseThrow(()->new QualificationService.Problem(404,"User not found."));
   if("applicant".equals(target.getRole()))throw new QualificationService.Problem(400,"Review grants are configured for staff accounts.");
   boolean delegate="admin".equals(target.getRole());
   if(delegate&&actor.equals(id))throw new QualificationService.Problem(403,"Another admin must manage your course delegation.");
   JsonObject body=JsonUtil.readRequestBodyAsJson(req);
   List<String> permissions=QualificationService.list(body,"permissions"),courses=QualificationService.list(body,"courseIds");
   Set<String> allowed=delegate?ReviewAccess.ADMIN_DELEGATIONS:ReviewAccess.PERMISSIONS;
   if(!allowed.containsAll(permissions))throw new QualificationService.Problem(400,"Unknown permission for this role.");
   if(delegate&&!permissions.isEmpty()&&courses.isEmpty())throw new QualificationService.Problem(400,"Choose at least one course for delegated access.");
   if(!delegate&&permissions.stream().anyMatch(p->!p.equals("review.read"))&&!permissions.contains("review.read"))
    throw new QualificationService.Problem(400,"Review actions also require View applications permission.");
   for(String course:courses)if(data.getData().getModules().stream().noneMatch(m->course.equals(m.getId())))
    throw new QualificationService.Problem(400,"Unknown course.");
   String reason=delegate?QualificationService.text(body,"reason",1000,true):"";
   target.setPermissions(permissions);target.setCourseIds(delegate&&permissions.isEmpty()?List.of():courses);
   data.save();
   data.addLog("Review Access Updated","info",(String)req.getSession().getAttribute("userName"),actor,
       "Updated grants for "+id+" | permissions: "+permissions+" | courses: "+target.getCourseIds()
       +(delegate?" | reason: "+reason:""),req.getRemoteAddr());
   JsonUtil.writeJson(resp,settings(actor));
  }catch(QualificationService.Problem e){JsonUtil.writeError(resp,e.status,e.getMessage());}
  catch(JsonParseException e){JsonUtil.writeError(resp,400,"Invalid JSON.");}
 }
}
